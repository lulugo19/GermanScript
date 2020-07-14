import util.SimpleLogger

sealed class Typ(val name: String) {
  override fun toString(): String = name
  val logger = SimpleLogger()

  abstract val definierteOperatoren: Map<Operator, Typ>
  abstract val definierteKonvertierungen: MutableSet<Typ>

  object Zahl : Typ("Zahl") {
    override val definierteOperatoren: Map<Operator, Typ>
      get() = mapOf(
          Operator.PLUS to  Zahl,
          Operator.MINUS to Zahl,
          Operator.MAL to Zahl,
          Operator.GETEILT to Zahl,
          Operator.MODULO to Zahl,
          Operator.HOCH to Zahl,
          Operator.GRÖßER to Boolean,
          Operator.KLEINER to Boolean,
          Operator.GRÖSSER_GLEICH to Boolean,
          Operator.KLEINER_GLEICH to Boolean,
          Operator.UNGLEICH to Boolean,
          Operator.GLEICH to Boolean
      )
    override val definierteKonvertierungen: MutableSet<Typ>
      get() = mutableSetOf(
          Zeichenfolge,
          Boolean
      )
  }

  object Zeichenfolge : Typ("Zeichenfolge") {
    override val definierteOperatoren: Map<Operator, Typ>
      get() = mapOf(
          Operator.PLUS to Zeichenfolge,
          Operator.GLEICH to Boolean,
          Operator.UNGLEICH to Boolean,
          Operator.GRÖßER to Boolean,
          Operator.KLEINER to Boolean,
          Operator.GRÖSSER_GLEICH to Boolean,
          Operator.KLEINER_GLEICH to Boolean
      )
    override val definierteKonvertierungen: MutableSet<Typ>
      get() = mutableSetOf(
          Zahl
      )
  }

  object Boolean : Typ("Boolean") {
    override val definierteOperatoren: Map<Operator, Typ>
      get() = mapOf(
          Operator.UND to Boolean,
          Operator.ODER to Boolean,
          Operator.GLEICH to Boolean,
          Operator.UNGLEICH to Boolean
      )
    override val definierteKonvertierungen: MutableSet<Typ>
      get() = mutableSetOf(
          Zeichenfolge,
          Zahl
      )
  }

  data class Liste(val elementTyp: Typ) : Typ("Liste($elementTyp)") {
    override val definierteOperatoren: Map<Operator, Typ>
      get() = mapOf(
          Operator.PLUS to Liste(elementTyp)
      )
    override val definierteKonvertierungen: MutableSet<Typ>
      get() = mutableSetOf()
  }

  data class Klasse(val klassenDefinition: AST.Definition.Klasse):
      Typ(klassenDefinition.name.hauptWort(Kasus.NOMINATIV, Numerus.SINGULAR)) {
    override val definierteOperatoren: Map<Operator, Typ>
      get() = mapOf()

    override val definierteKonvertierungen: MutableSet<Typ>
      get() = mutableSetOf()
  }
}

class Typisierer(dateiPfad: String): PipelineKomponente(dateiPfad) {
  val definierer = Definierer(dateiPfad)
  val ast = definierer.ast

  fun typisiere() {
    definierer.definiere()
    definierer.funktionsDefinitionen.forEach(::typisiereFunktion)
    definierer.klassenDefinitionen.forEach{
      typisiereKlasse(it)
      it.methoden.values.forEach{ methode ->
        methode.klasse.typ = Typ.Klasse(it)
        typisiereFunktion(methode.funktion)
      }
    }
  }

  fun bestimmeTypen(nomen: AST.Nomen): Typ {
    val singularTyp = bestimmeTypen(nomen.hauptWort(Kasus.NOMINATIV, Numerus.SINGULAR))?: throw GermanScriptFehler.Undefiniert.Typ(nomen.bezeichner.toUntyped())
    return if (nomen.numerus == Numerus.SINGULAR) {
      singularTyp
    } else {
      Typ.Liste(singularTyp)
    }
   }

  private fun bestimmeTypen(typ: String): Typ? {
    return when(typ) {
      "Zahl" -> Typ.Zahl
      "Zeichenfolge"  -> Typ.Zeichenfolge
      "Boolean" -> Typ.Boolean
      else -> try {
        Typ.Klasse(definierer.holeKlassenDefinition(typ))
      } catch (fehler: NoSuchElementException) {
        null
      }
    }
  }

  fun typisiereTypKnoten(typKnoten: AST.TypKnoten?) {
    if (typKnoten != null) {
      typKnoten.typ = bestimmeTypen(typKnoten.name)
    }
  }

  private fun typisiereFunktion(funktion: AST.Definition.FunktionOderMethode.Funktion) {
    typisiereTypKnoten(funktion.rückgabeTyp)
    typisiereTypKnoten(funktion.objekt?.typKnoten)
    for (parameter in funktion.parameter) {
      typisiereTypKnoten(parameter.typKnoten)
    }
  }

  private fun typisiereKlasse(klasse: AST.Definition.Klasse) {
    for (eigenschaft in klasse.eigenschaften) {
      typisiereTypKnoten(eigenschaft.typKnoten)
    }
  }
}

fun main() {
  val typisierer = Typisierer("./iterationen/iter_2/code.gms")
  typisierer.typisiere()
}