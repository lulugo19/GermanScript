package germanskript

import germanskript.util.SimpleLogger
import java.io.File


sealed class Typ(val name: String) {
  override fun toString(): String = name
  val logger = SimpleLogger()

  abstract val definierteOperatoren: Map<Operator, Typ>
  abstract fun kannNachTypKonvertiertWerden(typ: Typ): kotlin.Boolean

  object Zahl : Typ("Zahl") {
    override val definierteOperatoren: Map<Operator, Typ> = mapOf(
          Operator.PLUS to Zahl,
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

    override fun kannNachTypKonvertiertWerden(typ: Typ) = typ == Zahl || typ == Zeichenfolge || typ == Boolean
  }

  object Zeichenfolge : Typ("Zeichenfolge") {
    override val definierteOperatoren: Map<Operator, Typ> = mapOf(
          Operator.PLUS to Zeichenfolge,
          Operator.GLEICH to Boolean,
          Operator.UNGLEICH to Boolean,
          Operator.GRÖßER to Boolean,
          Operator.KLEINER to Boolean,
          Operator.GRÖSSER_GLEICH to Boolean,
          Operator.KLEINER_GLEICH to Boolean
      )
    override fun kannNachTypKonvertiertWerden(typ: Typ) = typ == Zeichenfolge || typ == Zahl || typ == Boolean
  }

  object Boolean : Typ("Boolean") {
    override val definierteOperatoren: Map<Operator, Typ> = mapOf(
          Operator.UND to Boolean,
          Operator.ODER to Boolean,
          Operator.GLEICH to Boolean,
          Operator.UNGLEICH to Boolean
      )

    override fun kannNachTypKonvertiertWerden(typ: Typ) = typ == Boolean || typ == Zeichenfolge || typ == Zahl
  }

  object Generic : Typ("Generic") {
    override val definierteOperatoren: Map<Operator, Typ>
      get() = mapOf()

    override fun kannNachTypKonvertiertWerden(typ: Typ): kotlin.Boolean = false
  }

  sealed class KlassenTyp(name: String): Typ(name) {
    abstract val klassenDefinition: AST.Definition.Klasse

    data class Klasse(override val klassenDefinition: AST.Definition.Klasse):
        KlassenTyp(klassenDefinition.typ.name.hauptWort(Kasus.NOMINATIV, Numerus.SINGULAR)) {
        override val definierteOperatoren: Map<Operator, Typ> = mapOf(
            Operator.GLEICH to Boolean
        )

        override fun kannNachTypKonvertiertWerden(typ: Typ): kotlin.Boolean {
          return typ.name == this.name || typ == Zeichenfolge || klassenDefinition.konvertierungen.containsKey(typ.name)
        }
    }

    data class Liste(override val klassenDefinition: AST.Definition.Klasse, val elementTyp: Typ) : KlassenTyp("Liste($elementTyp)") {
      // Das hier muss umbedingt ein Getter sein, sonst gibt es Probleme mit StackOverflow
      override val definierteOperatoren: Map<Operator, Typ> get() = mapOf(
          Operator.PLUS to Liste(klassenDefinition, elementTyp),
          Operator.GLEICH to Boolean
      )
      override fun kannNachTypKonvertiertWerden(typ: Typ) = typ.name == this.name || typ == Zeichenfolge
    }
  }
}

class Typisierer(startDatei: File): PipelineKomponente(startDatei) {
  val definierer = Definierer(startDatei)
  val ast = definierer.ast
  private var _listenKlassenDefinition: AST.Definition.Klasse? = null
  val listenKlassenDefinition get() = _listenKlassenDefinition!!

  fun typisiere() {
    definierer.definiere()
    _listenKlassenDefinition = definierer.holeKlassenDefinition("Liste")
    definierer.funktionsDefinitionen.forEach(::typisiereFunktion)
    definierer.klassenDefinitionen.forEach(::typisiereKlasse)
  }

  fun bestimmeTypen(nomen: AST.Nomen): Typ {
    val typKnoten = AST.TypKnoten(nomen, emptyList())
    // setze den Parent hier manuell vom Nomen
    typKnoten.setParentNode(nomen.parent!!)
    return bestimmeTypen(typKnoten)!!
  }

  fun bestimmeTypen(typKnoten: AST.TypKnoten?): Typ? {
    if (typKnoten == null) {
      return null
    }
    val singularTyp = when(typKnoten.name.hauptWort(Kasus.NOMINATIV, Numerus.SINGULAR)) {
      "Zahl" -> Typ.Zahl
      "Zeichenfolge" -> Typ.Zeichenfolge
      "Boolean" -> Typ.Boolean
      "Typ" -> Typ.Generic
      "Liste" -> Typ.KlassenTyp.Liste(listenKlassenDefinition, Typ.Generic)
      else -> Typ.KlassenTyp.Klasse(definierer.holeKlassenDefinition(typKnoten))
    }
    typKnoten.typ = if (typKnoten.name.numerus == Numerus.SINGULAR) {
      singularTyp
    } else {
      Typ.KlassenTyp.Liste(listenKlassenDefinition, singularTyp)
    }
    return typKnoten.typ
   }

  private fun typisiereFunktion(funktion: AST.Definition.FunktionOderMethode.Funktion) {
    bestimmeTypen(funktion.rückgabeTyp)
    bestimmeTypen(funktion.objekt?.typKnoten)
    for (parameter in funktion.parameter) {
      bestimmeTypen(parameter.typKnoten)
    }
  }

  private fun typisiereKlasse(klasse: AST.Definition.Klasse) {
    bestimmeTypen(klasse.typ)
    for (eigenschaft in klasse.eigenschaften) {
      bestimmeTypen(eigenschaft.typKnoten)
    }
    klasse.methoden.values.forEach{ methode ->
      methode.klasse.typ = Typ.KlassenTyp.Klasse(klasse)
      typisiereFunktion(methode.funktion)
    }
    klasse.konvertierungen.values.forEach { konvertierung ->
      bestimmeTypen(konvertierung.typ)
    }
  }
}

fun main() {
  val typisierer = Typisierer(File("./iterationen/iter_2/code.gms"))
  typisierer.typisiere()
}