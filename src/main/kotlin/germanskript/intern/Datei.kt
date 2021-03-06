package germanskript.intern

import germanskript.*
import germanskript.IM_AST
import germanskript.Interpretierer
import java.io.BufferedWriter
import java.io.File

class Datei(): Objekt(BuildIn.IMMKlassen.datei, BuildIn.Klassen.datei) {

  lateinit var file: File

  override fun rufeMethodeAuf(
      aufruf: IM_AST.Satz.Ausdruck.IAufruf,
      injection: Interpretierer.InterpretInjection): Objekt {

    return when (aufruf.name) {
      "erstelle Datei" -> konstruktor(injection)
      "lese die Zeilen" -> leseZeilen()
      "hole den Schreiber" -> holeSchreiber()
      else -> throw Exception("Die Methode '${aufruf.name}' ist nicht definiert!")
    }
  }

  private fun konstruktor(injection: Interpretierer.InterpretInjection): Nichts {
    // TODO: handle hier mögliche Datei-Fehler

    // Der Dateiname wird relativ zur Startdatei aufgelöst
    file = injection.startDatei.parentFile
        .resolve(File((eigenschaften.getValue("DateiName") as Zeichenfolge).zeichenfolge))
    return Nichts
  }

  private fun leseZeilen(): Liste {
    val zeilen = file.readLines().map<String, Objekt> { zeile -> Zeichenfolge(zeile) }.toMutableList()
    return Liste(Typ.Compound.Klasse(BuildIn.Klassen.liste ,listOf(zeichenFolgenTypArgument)), zeilen)
  }

  private fun holeSchreiber() : Schreiber {
    return Schreiber(file.bufferedWriter())
  }
}

class Schreiber(private val writer: BufferedWriter): Objekt(BuildIn.IMMKlassen.schreiber, BuildIn.Klassen.schreiber) {
  override fun rufeMethodeAuf(aufruf: IM_AST.Satz.Ausdruck.IAufruf, injection: Interpretierer.InterpretInjection): Objekt {
    return when(aufruf.name) {
      "schreibe die Zeile" -> schreibeDieZeile(injection)
      "schreibe die Zeichenfolge" -> schreibeDieZeichenfolge(injection)
      "füge die Zeile hinzu" -> fügeDieZeileHinzu(injection)
      "füge die Zeichenfolge hinzu" -> fügeDieZeichenfolgeHinzu(injection)
      "schließe mich" -> schließe()
      else -> super.rufeMethodeAuf(aufruf, injection)
    }
  }

  override fun holeEigenschaft(eigenschaftsName: String): Objekt {
    throw Exception("Die Klasse Schreiber hat keine Eigenschaften!")
  }

  override fun setzeEigenschaft(eigenschaftsName: String, wert: Objekt) {
    TODO("Not yet implemented")
  }

  private fun schreibeDieZeichenfolge(injection: Interpretierer.InterpretInjection): Nichts {
    val zeile = injection.umgebung.leseVariable("Zeichenfolge") as Zeichenfolge
    writer.write(zeile.zeichenfolge)
    return Nichts
  }

  private fun schreibeDieZeile(injection: Interpretierer.InterpretInjection): Nichts {
    val zeile = injection.umgebung.leseVariable("Zeile") as Zeichenfolge
    writer.write(zeile.zeichenfolge)
    writer.newLine()
    return Nichts
  }

  private fun fügeDieZeichenfolgeHinzu(injection: Interpretierer.InterpretInjection): Nichts {
    val zeile = injection.umgebung.leseVariable("Zeichenfolge") as Zeichenfolge
    writer.append(zeile.zeichenfolge)
    return Nichts
  }

  private fun fügeDieZeileHinzu(injection: Interpretierer.InterpretInjection): Nichts {
    val zeile = injection.umgebung.leseVariable("Zeile") as Zeichenfolge
    writer.appendln(zeile.zeichenfolge)
    return Nichts
  }

  private fun schließe(): Nichts {
    writer.close()
    return Nichts
  }
}