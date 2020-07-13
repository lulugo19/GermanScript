import java.text.ParseException
import java.util.*

class Interpretierer(dateiPfad: String): ProgrammDurchlaufer<Wert>(dateiPfad) {
  val typPrüfer = TypPrüfer(dateiPfad)

  override val definierer = typPrüfer.definierer
  val ast: AST.Aufruf.Programm = typPrüfer.ast

  private var rückgabeWert: Wert? = null
  private val flags = EnumSet.noneOf(Flag::class.java)
  private val aufrufStapel = AufrufStapel()

  override val umgebung: Umgebung<Wert> get() = aufrufStapel.top().umgebung

  fun interpretiere() {
    typPrüfer.prüfe()
    try {
      aufrufStapel.push(ast, Umgebung())
      durchlaufeSätze(ast.sätze)
    } catch (stackOverflow: StackOverflowError) {
      throw GermanScriptFehler.LaufzeitFehler(
          aufrufStapel.top().aufruf.token,
          aufrufStapel.toString(),
          "Stack Overflow")
    }
  }

  private enum class Flag {
    SCHLEIFE_ABBRECHEN,
    SCHLEIFE_FORTFAHREN,
  }

  private class AufrufStapelElement(val aufruf: AST.Aufruf, val objekt: Wert.Objekt?, val umgebung: Umgebung<Wert>)
  companion object {
    private const val CALL_STACK_OUTPUT_LIMIT = 50
  }

  private inner class AufrufStapel {
    private val stapel = Stack<AufrufStapelElement>()

    fun top(): AufrufStapelElement = stapel.peek()
    fun push(funktionsAufruf: AST.Aufruf, neueUmgebung: Umgebung<Wert>, konstruktorObjekt: Wert.Objekt? = null) {
      val objekt = when (funktionsAufruf) {
        is AST.Aufruf.Programm -> null
        is AST.Aufruf.Funktion -> when (funktionsAufruf.aufrufTyp) {
          FunktionsAufrufTyp.FUNKTIONS_AUFRUF -> null
          FunktionsAufrufTyp.METHODEN_SELBST_AUFRUF -> top().objekt
          FunktionsAufrufTyp.METHODEN_BLOCK_AUFRUF -> top().umgebung.holeMethodenBlockObjekt()
          FunktionsAufrufTyp.METHODEN_OBJEKT_AUFRUF -> evaluiereAusdruck(funktionsAufruf.objekt!!.wert)
        }
        is AST.Aufruf.Konstruktor -> konstruktorObjekt
      } as Wert.Objekt?
      stapel.push(AufrufStapelElement(funktionsAufruf, objekt, neueUmgebung))
    }

    fun pop(): AufrufStapelElement = stapel.pop()

    override fun toString(): String {
      if (stapel.isEmpty()) {
        return ""
      }
      return "Aufrufstapel:\n"+ stapel.drop(1).reversed().joinToString(
          "\n",
          "\t",
          "",
          CALL_STACK_OUTPUT_LIMIT,
          "...",
          ::aufrufStapelElementToString
      )
    }

    private fun aufrufStapelElementToString(element: AufrufStapelElement): String {
      val aufruf = element.aufruf
      var zeichenfolge = "${aufruf.vollerName} in ${aufruf.token}"
      if (element.objekt != null) {
        val klassenName = element.objekt.klassenDefinition.name.nominativ!!
        zeichenfolge = "für $klassenName: $zeichenfolge"
      }

      return zeichenfolge
    }
  }

  override fun sollSätzeAbbrechen(): Boolean {
    return flags.contains(Flag.SCHLEIFE_FORTFAHREN) || flags.contains(Flag.SCHLEIFE_ABBRECHEN)
  }

  // region Sätze
  private fun durchlaufeBedingung(bedingung: AST.Satz.BedingungsTerm): Boolean {
      return if ((evaluiereAusdruck(bedingung.bedingung) as Wert.Boolean).boolean) {
        durchlaufeSätze(bedingung.sätze)
        true
      } else {
        false
      }
  }

  override fun durchlaufeVariablenDeklaration(deklaration: AST.Satz.VariablenDeklaration) {
    val wert = evaluiereAusdruck(deklaration.ausdruck)
    // Da der Typprüfer schon überprüft ob Variablen überschrieben werden können
    // werden hier die Variablen immer überschrieben
    umgebung.schreibeVariable(deklaration.name, wert, true)
  }

  override fun durchlaufeFunktionsAufruf(funktionsAufruf: AST.Aufruf.Funktion, istAusdruck: Boolean): Wert? {
    rückgabeWert = null
    val neueUmgebung = Umgebung<Wert>()
    neueUmgebung.pushBereich()
    for (argument in funktionsAufruf.argumente) {
      neueUmgebung.schreibeVariable(argument.name, evaluiereAusdruck(argument.wert), false)
    }
    val funktionsDefinition = funktionsAufruf.funktionsDefinition!!
    aufrufStapel.push(funktionsAufruf, neueUmgebung)
    durchlaufeSätze(funktionsDefinition.sätze)
    aufrufStapel.pop()
    return funktionsDefinition.rückgabeTyp?.let { rückgabeWert }
  }

  override fun durchlaufeZurückgabe(zurückgabe: AST.Satz.Zurückgabe) {
    val wert = evaluiereAusdruck(zurückgabe.ausdruck)
    rückgabeWert = wert
  }

  override fun durchlaufeBedingungsSatz(bedingungsSatz: AST.Satz.Bedingung) {
    val inBedingung = bedingungsSatz.bedingungen.any(::durchlaufeBedingung)

    if (!inBedingung && bedingungsSatz.sonst != null ) {
      durchlaufeSätze(bedingungsSatz.sonst)
    }
  }

  override fun durchlaufeSolangeSchleife(schleife: AST.Satz.SolangeSchleife) {
    while (!flags.contains(Flag.SCHLEIFE_ABBRECHEN) && (evaluiereAusdruck(schleife.bedingung.bedingung) as Wert.Boolean).boolean) {
      flags.remove(Flag.SCHLEIFE_FORTFAHREN)
      durchlaufeSätze(schleife.bedingung.sätze)
    }
    flags.remove(Flag.SCHLEIFE_ABBRECHEN)
  }

  override fun durchlaufeFürJedeSchleife(schleife: AST.Satz.FürJedeSchleife) {
    val liste = if (schleife.liste != null)  {
      evaluiereAusdruck(schleife.liste) as Wert.Liste
    } else {
      evaluiereVariable(schleife.singular!!.nominativPlural!!)!! as Wert.Liste
    }
    umgebung.pushBereich()
    for (element in liste.elemente) {
      flags.remove(Flag.SCHLEIFE_FORTFAHREN)
      umgebung.überschreibeVariable(schleife.binder, element)
      durchlaufeSätze(schleife.sätze)
      if (flags.contains(Flag.SCHLEIFE_ABBRECHEN)) {
        flags.remove(Flag.SCHLEIFE_ABBRECHEN)
        break
      }
    }
    umgebung.popBereich()
  }

  override fun durchlaufeIntern() = interneFunktionen.getValue(aufrufStapel.top().aufruf.vollerName!!)()


  override fun bevorDurchlaufeMethodenBlock(methodenBlock: AST.Satz.MethodenBlock, blockObjekt: Wert?) {
    // mache nichts hier, das ist eigentlich nur für den Typprüfer gedacht
  }

  override fun durchlaufeAbbrechen() {
    flags.add(Flag.SCHLEIFE_ABBRECHEN)
  }

  override fun durchlaufeFortfahren() {
    flags.add(Flag.SCHLEIFE_FORTFAHREN)
  }
  // endregion

  // region Ausdrücke

  override fun evaluiereZeichenfolge(ausdruck: AST.Ausdruck.Zeichenfolge): Wert {
    return ausdruck.zeichenfolge.typ.zeichenfolge
  }

  override fun evaluiereZahl(ausdruck: AST.Ausdruck.Zahl): Wert {
    return ausdruck.zahl.typ.zahl
  }

  override fun evaluiereBoolean(ausdruck: AST.Ausdruck.Boolean): Wert {
    return ausdruck.boolean.typ.boolean
  }

  override fun evaluiereListe(ausdruck: AST.Ausdruck.Liste): Wert {
    return Wert.Liste(ausdruck.elemente.map(::evaluiereAusdruck))
  }

  override fun evaluiereObjektInstanziierung(instanziierung: AST.Ausdruck.ObjektInstanziierung): Wert {
    val eigenschaften = hashMapOf<String, Wert>()
    for (zuweisung in instanziierung.eigenschaftsZuweisungen) {
      eigenschaften[zuweisung.name.nominativ!!] = evaluiereAusdruck(zuweisung.wert)
    }
    val klassenDefinition = (instanziierung.klasse.typ!! as Typ.Klasse).klassenDefinition
    val objekt = Wert.Objekt(klassenDefinition, eigenschaften)
    val konstruktor = klassenDefinition.konstruktor
    aufrufStapel.push(klassenDefinition.konstruktor, Umgebung(), objekt)
    durchlaufeSätze(konstruktor.sätze)
    aufrufStapel.pop()
    return objekt
  }

  override fun evaluiereEigenschaftsZugriff(eigenschaftsZugriff: AST.Ausdruck.EigenschaftsZugriff): Wert {
    val objekt = evaluiereAusdruck(eigenschaftsZugriff.objekt) as Wert.Objekt
    return objekt.eigenschaften.getValue(eigenschaftsZugriff.eigenschaftsName.nominativ!!)
  }

  override fun evaluiereSelbstEigenschaftsZugriff(eigenschaftsZugriff: AST.Ausdruck.SelbstEigenschaftsZugriff): Wert {
    val objekt = aufrufStapel.top().objekt!!
    return objekt.eigenschaften.getValue(eigenschaftsZugriff.eigenschaftsName.nominativ!!)
  }

  override fun evaluiereMethodenBlockEigenschaftsZugriff(eigenschaftsZugriff: AST.Ausdruck.MethodenBlockEigenschaftsZugriff): Wert {
    val objekt = umgebung.holeMethodenBlockObjekt()!! as Wert.Objekt
    return objekt.eigenschaften.getValue(eigenschaftsZugriff.eigenschaftsName.nominativ!!)
  }

  override  fun evaluiereBinärenAusdruck(ausdruck: AST.Ausdruck.BinärerAusdruck): Wert {
    val links = evaluiereAusdruck(ausdruck.links)
    val rechts = evaluiereAusdruck(ausdruck.rechts)
    val operator = ausdruck.operator.typ.operator
    return when (links) {
      is Wert.Zeichenfolge -> zeichenFolgenOperation(operator, links, rechts as Wert.Zeichenfolge)
      is Wert.Zahl -> {
        if ((rechts as Wert.Zahl).isZero() && operator == Operator.GETEILT) {
          throw GermanScriptFehler.LaufzeitFehler(holeErstesTokenVonAusdruck(ausdruck.rechts), aufrufStapel.toString(),
            "Division durch 0. Es kann nicht durch 0 dividiert werden.")
        }
        zahlOperation(operator, links, rechts)
      }
      is Wert.Boolean -> booleanOperation(operator, links, rechts as Wert.Boolean)
      is Wert.Liste -> listenOperation(operator, links, rechts as Wert.Liste)
      else -> throw Exception("Typprüfer sollte disen Fehler verhindern.")
    }
  }

  private fun zeichenFolgenOperation(operator: Operator, links: Wert.Zeichenfolge, rechts: Wert.Zeichenfolge): Wert {
    return when (operator) {
      Operator.GLEICH -> Wert.Boolean(links == rechts)
      Operator.UNGLEICH -> Wert.Boolean(links != rechts)
      Operator.GRÖßER -> Wert.Boolean(links > rechts)
      Operator.KLEINER -> Wert.Boolean(links < rechts)
      Operator.GRÖSSER_GLEICH -> Wert.Boolean(links >= rechts)
      Operator.KLEINER_GLEICH -> Wert.Boolean(links <= rechts)
      Operator.PLUS -> Wert.Zeichenfolge(links + rechts)
      else -> throw Exception("Operator $operator ist für den Typen Zeichenfolge nicht definiert.")
    }
  }

  private fun zahlOperation(operator: Operator, links: Wert.Zahl, rechts: Wert.Zahl): Wert {
    return when(operator) {
      Operator.GLEICH -> Wert.Boolean(links == rechts)
      Operator.UNGLEICH -> Wert.Boolean(links != rechts)
      Operator.GRÖßER -> Wert.Boolean(links > rechts)
      Operator.KLEINER -> Wert.Boolean(links < rechts)
      Operator.GRÖSSER_GLEICH -> Wert.Boolean(links >= rechts)
      Operator.KLEINER_GLEICH -> Wert.Boolean(links <= rechts)
      Operator.PLUS -> links + rechts
      Operator.MINUS -> links - rechts
      Operator.MAL -> links * rechts
      Operator.GETEILT -> links / rechts
      Operator.MODULO -> links % rechts
      Operator.HOCH -> links.pow(rechts)
      else -> throw Exception("Operator $operator ist für den Typen Zahl nicht definiert.")
    }
  }

  private fun booleanOperation(operator: Operator, links: Wert.Boolean, rechts: Wert.Boolean): Wert {
    return when (operator) {
      Operator.ODER -> Wert.Boolean(links.boolean || rechts.boolean)
      Operator.UND -> Wert.Boolean(links.boolean && rechts.boolean)
      Operator.GLEICH -> Wert.Boolean(links.boolean == rechts.boolean)
      Operator.UNGLEICH -> Wert.Boolean(links.boolean != rechts.boolean)
      else -> throw Exception("Operator $operator ist für den Typen Boolean nicht definiert.")
    }
  }

  private fun listenOperation(operator: Operator, links: Wert.Liste, rechts: Wert.Liste): Wert {
    return when (operator) {
      Operator.PLUS ->links + rechts
      else -> throw Exception("Operator $operator ist für den Typen Liste nicht definiert.")
    }
  }

  override fun evaluiereMinus(minus: AST.Ausdruck.Minus): Wert.Zahl {
    val ausdruck = evaluiereAusdruck(minus.ausdruck) as Wert.Zahl
    return -ausdruck
  }

  override fun evaluiereListenElement(listenElement: AST.Ausdruck.ListenElement): Wert {
    val liste = evaluiereVariable(listenElement.singular.nominativPlural!!) as Wert.Liste
    val index = (evaluiereAusdruck(listenElement.index) as Wert.Zahl).toInt()
    if (index >= liste.elemente.size) {
      throw GermanScriptFehler.LaufzeitFehler(holeErstesTokenVonAusdruck(listenElement.index),
        aufrufStapel.toString(),"Index außerhalb des Bereichs. Der Index ist $index, doch die Länge der Liste ist ${liste.elemente.size}.\n")
    }
    return liste.elemente[index]
  }
  // endregion

  // region interne Funktionen
  private val interneFunktionen = mapOf<String, () -> (Unit)>(
      "schreibe die Zeichenfolge" to {
        val zeichenfolge = umgebung.leseVariable("Zeichenfolge") as Wert.Zeichenfolge
        print(zeichenfolge)
      },

      "schreibe die Zeile" to {
        val zeile = umgebung.leseVariable("Zeile") as Wert.Zeichenfolge
        println(zeile)
      },

      "schreibe die Zahl" to {
        val zahl = umgebung.leseVariable("Zahl") as Wert.Zahl
        println(zahl)
      },

      "lese" to{
        rückgabeWert = Wert.Zeichenfolge(readLine()!!)
      }
  )

  override fun evaluiereKonvertierung(konvertierung: AST.Ausdruck.Konvertierung): Wert {
    val wert = evaluiereAusdruck(konvertierung.ausdruck)
    return when (konvertierung.typ.typ!!) {
      is Typ.Zahl -> konvertiereZuZahl(konvertierung, wert)
      is Typ.Boolean -> konvertiereZuBoolean(konvertierung, wert)
      is Typ.Zeichenfolge -> konvertiereZuZeichenfolge(konvertierung, wert)
      else -> throw Exception("Typprüfer sollte diesen Fall schon überprüfen!")
    }
  }

  private fun konvertiereZuZahl(konvertierung: AST.Ausdruck.Konvertierung, wert: Wert): Wert.Zahl {
    return when (wert) {
      is Wert.Zeichenfolge -> {
        try {
          Wert.Zahl(wert.zeichenfolge)
        }
        catch (parseFehler: ParseException) {
          throw GermanScriptFehler.LaufzeitFehler(konvertierung.typ.name.bezeichner.toUntyped(), aufrufStapel.toString(),
              "Die Zeichenfolge '${wert.zeichenfolge}' kann nicht in eine Zahl konvertiert werden.")
        }
      }
      is Wert.Boolean -> Wert.Zahl(if (wert.boolean) 1.0 else 0.0)
      else -> throw Exception("Typ-Prüfer sollte dies schon überprüfen!")
    }
  }

  private fun konvertiereZuZeichenfolge(konvertierung: AST.Ausdruck.Konvertierung, wert: Wert): Wert.Zeichenfolge {
    return when (wert) {
      is Wert.Zahl -> Wert.Zeichenfolge(wert.toString())
      is Wert.Boolean -> Wert.Zeichenfolge(if(wert.boolean) "wahr" else "falsch")
      else -> throw Exception("Typ-Prüfer sollte dies schon überprüfen!")
    }
  }

  private fun konvertiereZuBoolean(konvertierung: AST.Ausdruck.Konvertierung, wert: Wert): Wert.Boolean {
    return when (wert) {
      is Wert.Zeichenfolge -> Wert.Boolean(wert.zeichenfolge.isNotEmpty())
      is Wert.Zahl -> Wert.Boolean(!wert.isZero())
      else -> throw Exception("Typ-Prüfer sollte dies schon überprüfen!")
    }
  }
  // endregion
}

fun main() {
  val interpreter = Interpretierer("./iterationen/iter_2/code.gms")
  try {
    interpreter.interpretiere()
  } catch (fehler: GermanScriptFehler) {
    // Anstatt zu werfen gebe Fehler später einfach aus
    //System.err.println(fehler.message!!)
    throw fehler
  }
}