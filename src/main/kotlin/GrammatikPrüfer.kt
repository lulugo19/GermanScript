import util.SimpleLogger
import java.util.*

class GrammatikPrüfer(dateiPfad: String): PipelineKomponente(dateiPfad) {
  val deklanierer = Deklanierer(dateiPfad)
  val ast = deklanierer.ast

  val logger = SimpleLogger()

  fun prüfe() {
    deklanierer.deklaniere()

    ast.visit() { knoten ->
      when (knoten) {
        is AST.Definition.FunktionOderMethode.Funktion -> prüfeFunktionsDefinition(knoten)
        is AST.Definition.FunktionOderMethode.Methode -> prüfeMethodenDefinition(knoten)
        is AST.Definition.Klasse -> prüfeKlassenDefinition(knoten)
        is AST.Satz.VariablenDeklaration -> prüfeVariablendeklaration(knoten)
        is AST.Satz.BedingungsTerm -> prüfeKontextbasiertenAusdruck(knoten.bedingung, null, EnumSet.of(Kasus.NOMINATIV))
        is AST.Satz.Zurückgabe -> prüfeKontextbasiertenAusdruck(knoten.ausdruck, null, EnumSet.of(Kasus.AKKUSATIV))
        is AST.Satz.FürJedeSchleife -> prüfeFürJedeSchleife(knoten)
        is AST.Satz.FunktionsAufruf -> prüfeFunktionsAufruf(knoten.aufruf)
        is AST.Satz.MethodenBlock -> prüfeNomen(knoten.name, EnumSet.of(Kasus.NOMINATIV))
        is AST.Ausdruck -> return@visit false
      }
      // visit everything
      true
    }
  }

  private fun prüfeNomen(nomen: AST.Nomen, fälle: EnumSet<Kasus>) {
    if (nomen.geprüft) {
      return
    }
    // Bezeichner mit nur einem Buchstaben sind Symbole
    if (nomen.bezeichner.wert.length == 1) {
      val symbol = nomen.bezeichner.wert
      nomen.numerus = Numerus.SINGULAR
      nomen.nominativ = symbol
      nomen.nominativSingular = symbol
      nomen.genus = Genus.NEUTRUM
      nomen.fälle.addAll(fälle)
    } else {
      val deklanation = deklanierer.holeDeklination(nomen)
      val numerus = deklanation.getNumerus(nomen.bezeichner.wert)
      nomen.numerus = numerus
      nomen.nominativ = deklanation.getForm(Kasus.NOMINATIV, numerus)
      nomen.nominativSingular = deklanation.getForm(Kasus.NOMINATIV, Numerus.SINGULAR)
      nomen.nominativPlural = deklanation.getForm(Kasus.NOMINATIV, Numerus.PLURAL)
      nomen.genus = deklanation.genus
      for (kasus in fälle) {
        val erwarteteForm = deklanation.getForm(kasus, numerus)
        if (nomen.bezeichner.wert == erwarteteForm) {
          nomen.fälle.add(kasus)
        }
      }
      if (nomen.fälle.isEmpty()) {
        // TODO: berücksichtige auch die möglichen anderen Fälle in der Fehlermeldung
        val kasus = fälle.first()
        val erwarteteForm = deklanation.getForm(kasus, numerus)
        throw GermanScriptFehler.GrammatikFehler.FormFehler.FalschesNomen(nomen.bezeichner.toUntyped(), kasus, nomen, erwarteteForm)
      }
    }
    prüfeVornomen(nomen)
  }

  private fun prüfeVornomen(nomen: AST.Nomen)
  {
    if (nomen.vornomen == null) {
      return
    }
    val vorNomen = nomen.vornomen
    val ersterFall = nomen.fälle.first()
    for (kasus in nomen.fälle) {
      val erwartetesVornomen = holeVornomen(vorNomen.typ, kasus, nomen.genus!!, nomen.numerus!!)
      if (vorNomen.wert == erwartetesVornomen) {
        nomen.vornomenString = erwartetesVornomen
      } else {
        nomen.fälle.remove(kasus)
      }
    }

    if (nomen.vornomenString == null) {
      val erwartetesVornomen = holeVornomen(vorNomen.typ, ersterFall, nomen.genus!!, nomen.numerus!!)
      throw GermanScriptFehler.GrammatikFehler.FormFehler.FalschesVornomen(vorNomen.toUntyped(), ersterFall, nomen, erwartetesVornomen)
    }
  }

  private fun holeVornomen(vorNomen: TokenTyp.VORNOMEN, kasus: Kasus, genus: Genus, numerus: Numerus): String {
    val kasusIndex = kasus.ordinal
    val spaltenIndex = if (numerus == Numerus.SINGULAR) genus.ordinal else 3
    return VORNOMEN_TABELLE.getValue(vorNomen)[kasusIndex][spaltenIndex]
  }

  private fun prüfeNumerus(nomen: AST.Nomen, numerus: Numerus) {
    if (nomen.numerus!! != numerus) {
      val numerusForm = deklanierer.holeDeklination(nomen).getForm(nomen.fälle.first(), numerus)
      throw GermanScriptFehler.GrammatikFehler.FalscherNumerus(nomen.bezeichner.toUntyped(), numerus, numerusForm)
    }
  }

  // region kontextbasierte Ausdrücke
  private fun prüfeKontextbasiertenAusdruck(ausdruck: AST.Ausdruck, kontextNomen: AST.Nomen?, fälle: EnumSet<Kasus>) {
    when (ausdruck) {
      is AST.Ausdruck.Variable -> prüfeVariable(ausdruck, kontextNomen, fälle)
      is AST.Ausdruck.Liste ->  prüfeListe(ausdruck, kontextNomen, fälle)
      is AST.Ausdruck.ListenElement -> prüfeListenElement(ausdruck, kontextNomen, fälle)
      is AST.Ausdruck.ObjektInstanziierung -> prüfeObjektinstanziierung(ausdruck, kontextNomen, fälle)
      is AST.Ausdruck.EigenschaftsZugriff -> prüfeEigenschaftsZugriff(ausdruck, kontextNomen, fälle)
      is AST.Ausdruck.MethodenBlockEigenschaftsZugriff -> prüfeNomenKontextBasiert(ausdruck.eigenschaftsName, kontextNomen, fälle)
      is AST.Ausdruck.SelbstEigenschaftsZugriff -> prüfeNomenKontextBasiert(ausdruck.eigenschaftsName, kontextNomen, fälle)
      is AST.Ausdruck.Konvertierung -> prüfeKonvertierung(ausdruck, kontextNomen, fälle)
      is AST.Ausdruck.BinärerAusdruck -> prüfeBinärenAusdruck(ausdruck, kontextNomen, fälle)
      is AST.Ausdruck.FunktionsAufruf -> prüfeFunktionsAufruf(ausdruck.aufruf)
      is AST.Ausdruck.Minus -> prüfeMinus(ausdruck)
    }
  }

  private fun prüfeVariable(variable: AST.Ausdruck.Variable, kontextNomen: AST.Nomen?, fälle: EnumSet<Kasus>) {
    prüfeNomen(variable.name, fälle)
    if (kontextNomen != null) {
      prüfeNumerus(variable.name, kontextNomen.numerus!!)
    }
  }

  private fun prüfeListe(liste: AST.Ausdruck.Liste, kontextNomen: AST.Nomen?, fälle: EnumSet<Kasus>) {
    prüfeNomen(liste.pluralTyp, fälle)
    prüfeNumerus(liste.pluralTyp, Numerus.PLURAL)
    if (kontextNomen != null) {
      prüfeNumerus(kontextNomen, Numerus.PLURAL)
    }
    liste.elemente.forEach {element -> prüfeKontextbasiertenAusdruck(element, null, EnumSet.of(Kasus.NOMINATIV))}
  }

  private fun prüfeObjektinstanziierung(instanziierung: AST.Ausdruck.ObjektInstanziierung, kontextNomen: AST.Nomen?, fälle: EnumSet<Kasus>) {
    prüfeNomen(instanziierung.klasse.name, fälle)
    if (kontextNomen != null) {
      prüfeNumerus(kontextNomen, Numerus.SINGULAR)
    }
    for (eigenschaftsZuweisung in instanziierung.eigenschaftsZuweisungen) {
      prüfeNomen(eigenschaftsZuweisung.name, EnumSet.of(Kasus.DATIV))
      prüfeKontextbasiertenAusdruck(eigenschaftsZuweisung.wert, eigenschaftsZuweisung.name, EnumSet.of(Kasus.NOMINATIV))
    }
  }

  private fun prüfeEigenschaftsZugriff(eigenschaftsZugriff: AST.Ausdruck.EigenschaftsZugriff, kontextNomen: AST.Nomen?, fälle: EnumSet<Kasus>) {
    prüfeNomenKontextBasiert(eigenschaftsZugriff.eigenschaftsName, kontextNomen, fälle)
    prüfeKontextbasiertenAusdruck(eigenschaftsZugriff.objekt, null, EnumSet.of(Kasus.GENITIV))
  }

  private fun prüfeNomenKontextBasiert(
      nomen: AST.Nomen,
      kontextNomen: AST.Nomen?,
      fälle: EnumSet<Kasus>)
  {
    prüfeNomen(nomen, fälle)
    if (kontextNomen != null) {
      prüfeNumerus(kontextNomen, nomen.numerus!!)
    }
  }

  private fun prüfeListenElement(listenElement: AST.Ausdruck.ListenElement, kontextNomen: AST.Nomen?, fälle: EnumSet<Kasus>) {
    prüfeNomen(listenElement.singular, fälle)
    prüfeNumerus(listenElement.singular, Numerus.SINGULAR)
    if (kontextNomen != null) {
      prüfeNumerus(kontextNomen, Numerus.SINGULAR)
    }
    prüfeKontextbasiertenAusdruck(listenElement.index, null, EnumSet.of(Kasus.NOMINATIV))
  }

  private fun prüfeKonvertierung(konvertierung: AST.Ausdruck.Konvertierung, kontextNomen: AST.Nomen?, fälle: EnumSet<Kasus>) {
    prüfeNomen(konvertierung.typ.name, EnumSet.of(Kasus.NOMINATIV))
    prüfeKontextbasiertenAusdruck(konvertierung.ausdruck, kontextNomen, fälle)
  }
  // endregion
  private fun prüfeVariablendeklaration(variablenDeklaration: AST.Satz.VariablenDeklaration) {
    val nomen = variablenDeklaration.name
    prüfeNomen(nomen, EnumSet.of(Kasus.NOMINATIV))
    // prüfe ob Numerus mit 'ist' oder 'sind' übereinstimmt
    if (!variablenDeklaration.zuweisungsOperator.typ.numerus.contains(nomen.numerus!!)) {
      throw GermanScriptFehler.GrammatikFehler.FalscheZuweisung(variablenDeklaration.zuweisungsOperator.toUntyped(), nomen.numerus!!)
    }
    if (variablenDeklaration.neu != null) {
      if (nomen.genus!! != variablenDeklaration.neu.typ.genus) {
        throw GermanScriptFehler.GrammatikFehler.FormFehler.FalschesVornomen(
            variablenDeklaration.neu.toUntyped(), Kasus.NOMINATIV, nomen, TokenTyp.JEDE.holeForm(nomen.genus!!)
        )
      }
    }
    // logger.addLine("geprüft: $variablenDeklaration")
    prüfeKontextbasiertenAusdruck(variablenDeklaration.ausdruck, nomen, EnumSet.of(Kasus.NOMINATIV))
  }

  private fun prüfeBinärenAusdruck(binärerAusdruck: AST.Ausdruck.BinärerAusdruck, kontextNomen: AST.Nomen?, fälle: EnumSet<Kasus>) {
    val rechterKasus = EnumSet.of(binärerAusdruck.operator.typ.operator.klasse.kasus)
    val linkerKasus = if (binärerAusdruck.istAnfang) fälle else rechterKasus

    // kontextNomen gilt nur für den linken Ausdruck (für den aller ersten Audruck in dem binären Ausdruck)
    prüfeKontextbasiertenAusdruck(binärerAusdruck.links, kontextNomen, linkerKasus)
    prüfeKontextbasiertenAusdruck(binärerAusdruck.rechts, null, rechterKasus)
    logger.addLine("geprüft: $binärerAusdruck")
  }

  private fun prüfeFürJedeSchleife(fürJedeSchleife: AST.Satz.FürJedeSchleife) {
    prüfeNomen(fürJedeSchleife.singular, EnumSet.of(Kasus.AKKUSATIV))
    prüfeNumerus(fürJedeSchleife.singular, Numerus.SINGULAR)
    if (fürJedeSchleife.jede.typ.genus != fürJedeSchleife.singular.genus!!) {
      throw GermanScriptFehler.GrammatikFehler.FormFehler.FalschesVornomen(
          fürJedeSchleife.jede.toUntyped(), Kasus.NOMINATIV, fürJedeSchleife.singular,
          TokenTyp.JEDE.holeForm(fürJedeSchleife.singular.genus!!)
      )
    }

    prüfeNomen(fürJedeSchleife.binder, EnumSet.of(Kasus.NOMINATIV))
    prüfeNumerus(fürJedeSchleife.binder, Numerus.SINGULAR)

    if (fürJedeSchleife.liste != null) {
      prüfeKontextbasiertenAusdruck(fürJedeSchleife.liste, fürJedeSchleife.singular, EnumSet.of(Kasus.DATIV))
    }
  }


  private fun prüfeParameter(parameter: AST.Definition.TypUndName, fälle: EnumSet<Kasus>) {
    val nomen = parameter.typKnoten.name
    prüfeNomen(nomen, fälle)
    prüfeNomen(parameter.name, EnumSet.of(Kasus.NOMINATIV))
    if (parameter.name.vornomenString == null) {
      val paramName = parameter.name
      paramName.vornomenString = holeVornomen(TokenTyp.VORNOMEN.ARTIKEL.BESTIMMT, nomen.fälle.first(), paramName.genus!!, paramName.numerus!!)
    }
  }

  private fun prüfePräpositionsParameter(präposition: AST.Definition.PräpositionsParameter) {
    for (parameter in präposition.parameter) {
      prüfeParameter(parameter, präposition.präposition.fälle)
    }
  }

  private fun prüfeFunktionsDefinition(funktionsDefinition: AST.Definition.FunktionOderMethode.Funktion) {
    if (funktionsDefinition.rückgabeTyp != null) {
      prüfeNomen(funktionsDefinition.rückgabeTyp.name, EnumSet.of(Kasus.NOMINATIV))
    }
    if (funktionsDefinition.objekt != null) {
      prüfeParameter(funktionsDefinition.objekt, EnumSet.of(Kasus.DATIV, Kasus.AKKUSATIV))
    }
    for (präposition in funktionsDefinition.präpositionsParameter) {
      prüfePräpositionsParameter(präposition)
    }
    // logger.addLine("geprüft: $funktionsDefinition")
  }

  private fun prüfeMethodenDefinition(methodenDefinition: AST.Definition.FunktionOderMethode.Methode){
    prüfeNomen(methodenDefinition.klasse.name, EnumSet.of(Kasus.NOMINATIV))
    prüfeFunktionsDefinition(methodenDefinition.funktion)
  }

  private fun prüfeKlassenDefinition(klasse: AST.Definition.Klasse) {
    prüfeNomen(klasse.name, EnumSet.of(Kasus.NOMINATIV))
    prüfeNumerus(klasse.name, Numerus.SINGULAR)

    for (eigenschaft in klasse.eigenschaften) {
      prüfeNomen(eigenschaft.typKnoten.name, EnumSet.of(Kasus.DATIV))
      prüfeNomen(eigenschaft.name, EnumSet.of(Kasus.NOMINATIV))
    }
  }


  private fun prüfeArgument(argument: AST.Argument, fälle: EnumSet<Kasus>) {
    prüfeNomen(argument.name, fälle)
    prüfeKontextbasiertenAusdruck(argument.wert, argument.name, EnumSet.of(Kasus.NOMINATIV))
  }

  private fun prüfePräpositionsArgumente(präposition: AST.PräpositionsArgumente) {
    for (argument in präposition.argumente) {
      prüfeArgument(argument, präposition.präposition.fälle)
    }
  }

  private fun prüfeFunktionsAufruf(funktionsAufruf: AST.Aufruf.Funktion) {
    if (funktionsAufruf.objekt != null) {
      prüfeArgument(funktionsAufruf.objekt, EnumSet.of(Kasus.AKKUSATIV, Kasus.DATIV))
    }
    for (präposition in funktionsAufruf.präpositionsArgumente) {
      prüfePräpositionsArgumente(präposition)
    }
    // logger.addLine("geprüft: $funktionsAufruf")
  }

  private fun prüfeMinus(knoten: AST.Ausdruck.Minus) {
    if (knoten.ausdruck is AST.Ausdruck.Variable) {
      val variable = knoten.ausdruck
      prüfeNomen(variable.name, EnumSet.of(Kasus.AKKUSATIV))
    }
  }
}

private val VORNOMEN_TABELLE = mapOf<TokenTyp.VORNOMEN, Array<Array<String>>>(
    TokenTyp.VORNOMEN.ARTIKEL.BESTIMMT to arrayOf(
        arrayOf("der", "die", "das", "die"),
        arrayOf("des", "der", "des", "der"),
        arrayOf("dem", "der", "dem", "den"),
        arrayOf("den", "die", "das", "die")
    ),

    TokenTyp.VORNOMEN.ARTIKEL.UNBESTIMMT to arrayOf(
        arrayOf("ein", "eine", "ein", "einige"),
        arrayOf("eines", "einer", "eines", "einiger"),
        arrayOf("einem", "einer", "einem", "einigen"),
        arrayOf("einen", "eine", "ein", "einige")
    ),

    TokenTyp.VORNOMEN.POSSESSIV_PRONOMEN.MEIN to arrayOf(
        arrayOf("mein", "meine", "mein", "meine"),
        arrayOf("meines", "meiner", "meines", "meiner"),
        arrayOf("meinem", "meiner", "meinem", "meinen"),
        arrayOf("meinen", "meine", "mein", "meine")
    ),

    TokenTyp.VORNOMEN.POSSESSIV_PRONOMEN.DEIN to arrayOf(
        arrayOf("dein", "deine", "dein", "deine"),
        arrayOf("deines", "deiner", "deines", "deiner"),
        arrayOf("deinem", "deiner", "deinem", "deinen"),
        arrayOf("deinen", "deine", "dein", "deine")
    )
)

fun main() {
  val grammatikPrüfer = GrammatikPrüfer("./iterationen/iter_2/code.gms")
  grammatikPrüfer.prüfe()
  grammatikPrüfer.logger.print()
}

