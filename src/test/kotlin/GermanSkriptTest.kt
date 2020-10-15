import germanskript.GermanSkriptFehler
import germanskript.Interpretierer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.*
import kotlin.Exception

class GermanSkriptTest {
  private fun führeGermanSkriptCodeAus(germanSkriptSource: String) {
    // erstelle temporäre Datei mit dem Source-Code
    val tempFile = createTempFile("germanskript_test_temp", ".gm")
    tempFile.writeText(germanSkriptSource)

    val interpretierer = Interpretierer(tempFile)
    try {
      interpretierer.interpretiere()
    }
    finally {
      // lösche temporäre Datei
      tempFile.delete()
    }
  }

  private fun testeGermanSkriptCode(quellCode: String, erwarteteAusgabe: String) {
    // leite den Standardoutput in einen Byte-Array-Stream um
    val myOut = ByteArrayOutputStream()
    System.setOut(PrintStream(myOut))

    try {
      führeGermanSkriptCodeAus(quellCode)
    } catch (fehler: Exception) {
      System.err.println(fehler)
    }
    finally {
      val actual = myOut.toString()
      assertThat(actual).isEqualToNormalizingNewlines(erwarteteAusgabe)
      // set out back to stdout
      System.setOut(PrintStream(FileOutputStream(FileDescriptor.out)))
    }
  }

  @Test
  @DisplayName("Standardbibliothek kompiliert")
  fun standardBibliothekKompiliert(){
    führeGermanSkriptCodeAus("")
  }

  @Test
  @DisplayName("Hallo Welt")
  fun halloWelt() {
    val quellCode = """
      schreibe die Zeile "Hallo Welt"
    """.trimIndent()
    val erwarteteAusgabe = "Hallo Welt\n"

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Bedingungen")
  fun bedingungen() {
    val source = """
      Verb teste die Zahl:
          wenn die Zahl gleich 3 ist:
            schreibe die Zeile "Alle guten Dinge sind drei!".
          sonst wenn die Zahl gleich 42 ist:
            schreibe die Zeile "Die Antwort auf alles.".
          sonst: schreibe die Zahl.
      .
      teste die Zahl 11
      teste die Zahl 3
      teste die Zahl 42
      teste die Zahl 12
    """.trimIndent()

    val expectedOutput = """
      11
      Alle guten Dinge sind drei!
      Die Antwort auf alles.
      12
      
    """.trimIndent()

    testeGermanSkriptCode(source, expectedOutput)
  }

  @Test
  @DisplayName("Fakultät")
  fun fakultät() {
    val source = """
      Verb(Zahl) fakultät von der Zahl:
        wenn die Zahl gleich 0 ist: gebe 1 zurück.
        sonst: gebe die Zahl * (fakultät von der Zahl - 1) zurück.
      .
      
      schreibe die Zahl (fakultät von der Zahl 3)
      schreibe die Zahl (fakultät von der Zahl 5)
      schreibe die Zahl (fakultät von der Zahl 6)
    """.trimIndent()

    val expectedOutput = """
      6
      120
      720
      
    """.trimIndent()

    testeGermanSkriptCode(source, expectedOutput)
  }

  @Test
  @DisplayName("Solange-Schleife")
  fun solangeSchleife() {
    val source = """
      eine Zahl ist -1
      solange wahr:
        eine Zahl ist die Zahl plus 1
        wenn die Zahl > 10 ist: abbrechen.
        wenn die Zahl mod 2 gleich 1 ist: fortfahren.
        schreibe die Zahl
      .
    """.trimIndent()

    val expectedOutput = """
      0
      2
      4
      6
      8
      10
      
    """.trimIndent()

    testeGermanSkriptCode(source, expectedOutput)
  }

  @Test
  @DisplayName("Listenindex")
  fun listen() {
    val source = """
      die Zahlen sind einige Zahlen [1, 2, 3]
      schreibe die Zahl[0]
      ein Index ist 1
      solange der Index kleiner als die AnZahl der Zahlen ist:
        schreibe die Zahl[Index]
        ein Index ist der Index plus 1
      .

    """.trimIndent()

    val expectedOutput = """
      1
      2
      3
      
    """.trimIndent()

    testeGermanSkriptCode(source, expectedOutput)
  }

  @Test
  @DisplayName("Für-Jede-Schleifen")
  fun fürJedeSchleifen() {
    val quellCode = """
      die Zahlen sind einige Zahlen [1, 2, 3]
      für jede Zahl:
        schreibe die Zahl
      .
      für jedes X in den Zahlen:
        schreibe die Zahl X
      .
      für jede Zahl in einigen Zahlen [11, 12, 13]:
        schreibe die Zahl
      .
    """.trimIndent()

    val erwarteteAusgabe = """
      1
      2
      3
      1
      2
      3
      11
      12
      13
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Variablen-Überdeckung")
  fun variablenÜberdeckung() {
    val source = """
      eine Zeichenfolge ist "Erste Variable"
      :
        schreibe die Zeile Zeichenfolge
        eine Zeichenfolge ist "Erste veränderte Variable"
        schreibe die Zeile Zeichenfolge
        eine neue Zeichenfolge ist "Zweite Variable"
        schreibe die Zeile Zeichenfolge
      .
      schreibe die Zeile Zeichenfolge
    """.trimIndent()

    val expectedOutput = """
      Erste Variable
      Erste veränderte Variable
      Zweite Variable
      Erste veränderte Variable
      
    """.trimIndent()

    testeGermanSkriptCode(source, expectedOutput)
  }

  @Test
  @DisplayName("Unveränderliche Variable können nicht neu zugewiesen werden")
  fun unverÄnderlicheVariablen() {
    val source = """
      die Zahl ist 6
      :
        die Zahl ist 5
      .
      die Zahl ist 10 // Fehler hier
    """.trimIndent()

    assertThatExceptionOfType(GermanSkriptFehler.Variablenfehler::class.java).isThrownBy {
      führeGermanSkriptCodeAus(source)
    }
  }

  @Test
  @DisplayName("Klassendefinition und Objektinstanziierung")
  fun klasseDefinitionUndObjekte() {
    val quellCode = """
      Deklination Maskulinum Singular(Name, Namens, Namen, Namen) Plural(Namen)
      Deklination Neutrum Singular(Alter, Alters, Alter, Alter) Plural(Alter)
      Deklination Femininum Singular(Person) Plural(Personen)

      Nomen Person mit
          der Zeichenfolge VorName,
          der Zeichenfolge NachName,
          einer Zahl Alter:

          dieser Name ist "#{mein VorName} #{mein NachName}"
          schreibe die Zeile "#{mein Name} (#{mein Alter} Jahre alt) wurde erstellt!"
      .
      
      die Person ist eine Person mit dem VorNamen "Max", dem NachNamen "Mustermann", dem Alter 23
      die PersonJANE ist eine Person mit dem VorNamen "Jane", dem NachNamen "Doe", dem Alter 41
    """.trimIndent()

    val erwarteteAusgabe = """
      Max Mustermann (23 Jahre alt) wurde erstellt!
      Jane Doe (41 Jahre alt) wurde erstellt!
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Eigene Konvertierungsdefinition")
  fun konvertierungsDefinition() {
    val quellCode = """
      Deklination Maskulinum Singular(Name, Namens, Namen, Namen) Plural(Namen)
      Deklination Neutrum Singular(Alter, Alters, Alter, Alter) Plural(Alter)
      Deklination Femininum Singular(Person) Plural(Personen)

      Nomen Person mit
          der Zeichenfolge VorName,
          der Zeichenfolge NachName,
          einer Zahl Alter:

          dieser Name ist "#{mein VorName} #{mein NachName}"
          // Man könnte auch schreiben: Ich als Zeichenfolge + " wurde erstellt!"
          schreibe die Zeile "#{Ich} wurde erstellt!"
      .
      
      Implementiere die Person:
        Als Zeichenfolge:
          gebe "#{mein Name} (#{mein Alter} Jahre alt)" zurück
        .
      .

      die Person ist eine Person mit dem VorNamen "Max", dem NachNamen "Mustermann", dem Alter 29
    """.trimIndent()

    val erwarteteAusgabe = """
      Max Mustermann (29 Jahre alt) wurde erstellt!
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("mehrere Implementierungs-Bereiche")
  fun mehrereImplementierungsBereiche() {
    val quellCode = """
      Deklination Maskulinum Singular(Name, Namens, Namen, Namen) Plural(Namen)
      Deklination Neutrum Singular(Alter, Alters, Alter, Alter) Plural(Alter)
      Deklination Femininum Singular(Person) Plural(Personen)
      Deklination Femininum Singular(Begrüßung) Plural(Begrüßungen)

      Nomen Person mit
          der Zeichenfolge VorName,
          der Zeichenfolge NachName,
          einer Zahl Alter:

          // Man könnte auch schreiben: Ich als Zeichenfolge + " wurde erstellt!"
          schreibe die Zeile "#{Ich} wurde erstellt!"
      .
      
      // implementiere eine Methode
      Implementiere die Person:
        Verb begrüße mich mit der Zeichenfolge Begrüßung:
          schreibe die Zeile "#{die Begrüßung} #{mein Name}!"
        .
      .
      
      // implementiere eine Eigenschaft
      Implementiere die Person:
        Eigenschaft(Zeichenfolge) Name:
          gebe meinen VorNamen + " " + meinen NachNamen zurück
        .
      .
      
      // implementiere eine Konvertierung
      Implementiere die Person:
        Als Zeichenfolge:
          gebe "#{mein Name} (#{mein Alter} Jahre alt)" zurück
        .
      .

      die Person ist eine Person mit dem VorNamen "Max", dem NachNamen "Mustermann", dem Alter 29
      begrüße die Person mit der Begrüßung "Hallo"
    """.trimIndent()

    val erwarteteAusgabe = """
      Max Mustermann (29 Jahre alt) wurde erstellt!
      Hallo Max Mustermann!
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("veränderliche Eigenschaften eines Objekt")
  fun veränderlicheEigenschaftenEinesObjekts() {
    val quellCode = """
      Deklination Maskulinum Singular(Zähler, Zählers, Zähler, Zähler) Plural(Zähler)
      
      Nomen Zähler:
        jene Zahl ist 0
        schreibe die Zahl (meine Zahl)
      .
      
      Implementiere den Zähler:
        Verb erhöhe mich um die Zahl:
          meine Zahl ist meine Zahl + die Zahl
          schreibe die Zahl (meine Zahl)
        .
      
        Verb resette mich:
          meine Zahl ist 0
          schreibe die Zahl (meine Zahl)
        .
      .

      der Zähler ist ein Zähler
      
      erhöhe den Zähler um die Zahl 5
      erhöhe den Zähler um die Zahl 3
      resette den Zähler
      Zähler:
        erhöhe dich um die Zahl -5
        erhöhe dich um die Zahl 10
        resette dich
      !
    """.trimIndent()

    val erwarteteAusgabe = """
      0
      5
      8
      0
      -5
      5
      0
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Modul (Funktionen)")
  fun module() {
    val source = """
      Verb hallo:
        schreibe die Zeile "Hallo Welt"
      .
      
      Modul Foo:
        Verb hallo:
          schreibe die Zeile "Hallo Foo"
        .
        
        Modul Bar:
          Verb hallo:
            schreibe die Zeile "Hallo Bar"
          .
        .
      .
      
      Modul Foo::Bar:
        Verb test:
          schreibe die Zeile "Test"
        .
      .
      
      hallo
      Foo::hallo
      Foo::Bar::hallo
      Foo::Bar::test
    """.trimIndent()

    val expectedOutput = """
      Hallo Welt
      Hallo Foo
      Hallo Bar
      Test
      
    """.trimIndent()

    testeGermanSkriptCode(source, expectedOutput)
  }

  @Test
  @DisplayName("verwende Module")
  fun verwendeModule() {
    val source = """
      Modul A:
        Modul B:
          Verb test: schreibe die Zeile "Test".
        .
      .
      verwende A::B
      test
    """.trimIndent()

    val expectedOutput = """
      Test
      
    """.trimIndent()

    testeGermanSkriptCode(source, expectedOutput)
  }

  @Test
  @DisplayName("verwende Module (komplexer)")
  fun verwendeModuleKomplexer() {
    val source = """
      Modul A:
        Deklination Neutrum Singular(Bar) Plural(Bars)
        Deklination Neutrum Singular(Foo) Plural(Foos)
        
        verwende C
        Modul B:
          Nomen Foo mit dem Bar:.
          
          Implementiere das Bar:
            Verb test:
              schreibe die Zeile "Bar"
            .
          .
        .
        Modul C:
          Nomen Bar:.
        .
      .
      
      verwende A
      
      das Foo ist ein B::Foo mit einem C::Bar
      das Bar ist das Bar des Foo
      Bar: test!
    """.trimIndent()

    val expectedOutput = """
      Bar
      
    """.trimIndent()

    testeGermanSkriptCode(source, expectedOutput)
  }

  @Test
  @DisplayName("Zugriff auf einzelne Zeichen einer Zeichenfolge")
  fun zeichenfolgeUmkehren() {
    val quellCode = """
      Verb(Zeichenfolge) kehre die Zeichenfolge um:
        ein Index ist die Länge der Zeichenfolge minus 1
        ein ERGEBNIS ist ""
        solange der Index größer gleich 0 ist:
          ein ERGEBNIS ist das ERGEBNIS + die Zeichenfolge[Index]
          ein Index ist der Index - 1
        .
        gebe das ERGEBNIS zurück
      .
      
      schreibe die Zeichenfolge (kehre die Zeichenfolge "Hallo Welt" um)
    """.trimIndent()

    testeGermanSkriptCode(quellCode, "tleW ollaH")
  }

  @Test
  @DisplayName("Schnittstelle (Adjektiv)")
  fun schnittstelle() {
    val quellCode = """
      Deklination Femininum Singular(Farbe) Plural(Farben)
      Deklination Neutrum Singular(Dreieck, Dreiecks, Dreieck, Dreieck) Plural(Dreiecke, Dreiecke, Dreiecken, Dreiecke)
      
      Adjektiv zeichenbar:
          Verb zeichne mich mit der Zeichenfolge Farbe
          Verb skaliere mich um die Zahl
      .

      Verb zeichne das Zeichenbare mit der Zeichenfolge Farbe: 
          Zeichenbares: zeichne dich mit der Farbe!
      .

      Nomen Dreieck:.
      
      Implementiere das zeichenbare Dreieck:
        Verb zeichne mich mit der Zeichenfolge Farbe:
           schreibe die Zeile "zeichne das Dreieck mit der Farbe #{die Farbe}"
        .

        Verb skaliere mich um die Zahl:
           schreibe die Zeile "skaliere das Dreieck um #{die Zahl}"
        .
      .

      das Dreieck ist ein Dreieck

      zeichne das zeichenbare Dreieck mit der Farbe "rot"
      skaliere das Dreieck um die Zahl 2
    """.trimIndent()

    val erwarteteAusgabe = """
      zeichne das Dreieck mit der Farbe rot
      skaliere das Dreieck um 2
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Schnittstelle 2")
  fun schnittstelle2() {
    val quellCode = """
      Deklination Neutrum Singular(Fenster, Fensters, Fenster, Fenster) Plural(Fenster)
      
      Adjektiv klickbar:
        Verb klick mich
      .
      
      Verb registriere das KlickbareX:
        wenn wahr:
          KlickbaresX: klick dich!
        .
      .
      
      Nomen Fenster:
        jene AnZahl ist 0
      .
      
      Implementiere das klickbare Fenster:
          Verb klick mich:
            meine AnZahl ist meine AnZahl plus 1
            schreibe die Zeile "Das Fenster wurde zum #{meine AnZahl}. angeklickt!"
          .
      .

      das Fenster ist ein Fenster
      registriere das klickbare Fenster
      
      klick das Fenster
    """.trimIndent()

    val erwarteteAusgabe = """
      Das Fenster wurde zum 1. angeklickt!
      Das Fenster wurde zum 2. angeklickt!
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Vererbung")
  fun vererbung() {
    val quellCode = """
      Deklination Femininum Singular(Person) Plural(Personen)
      Deklination Maskulinum Singular(Student, Studenten, Studenten, Studenten) Plural(Studenten)
      Deklination Maskulinum Singular(Name, Namens, Namen, Namen) Plural(Namen)
      Deklination Neutrum Singular(Alter, Alters, Alter, Alter) Plural(Alter)
      Deklination Maskulinum Singular(Studiengang, Studiengangs, Studiengang, Studiengang) Plural(Studiengänge)
      
      Nomen Person mit
        der Zeichenfolge VorName,
        der Zeichenfolge NachName,
        einer Zahl Alter:
        
        dieser Name ist "#{mein VorName} #{mein NachName}"
        schreibe die Zeile "#{mein Name} (#{mein Alter} Jahre alt) wurde erstellt!"
      .
      
      Nomen Student mit
          der Zeichenfolge VorName,
          der Zeichenfolge NachName,
          einer Zahl Alter,
          der Zeichenfolge Studiengang
          als Person mit dem VorNamen, dem NachNamen, dem Alter:
        schreibe die Zeile "#{mein VorName} #{mein NachName} ist ein #{mein Studiengang}-Student!"
      .
      
      Implementiere die Person:
        Verb stell mich vor:
          schreibe die Zeile "Hallo, mein Name ist #{mein Name} und ich bin #{mein Alter} Jahre alt!"
        .
      .
      
      Implementiere den Studenten:
        Verb stell mich vor:
          Super: stell mich vor!
          schreibe die Zeile "Ich bin #{mein Studiengang}-Student."
        .
      .

      Verb stell die Person vor:
        Person: stell dich vor!
      .
      
      der Student ist ein Student mit 
        dem VorNamen "Lukas",
        dem NachNamen "Gobelet",
        dem Alter 22,
        dem Studiengang "Informatik"
        
      stell die Person Student vor
    """.trimIndent()

    val erwarteteAusgabe = """
      Lukas Gobelet (22 Jahre alt) wurde erstellt!
      Lukas Gobelet ist ein Informatik-Student!
      Hallo, mein Name ist Lukas Gobelet und ich bin 22 Jahre alt!
      Ich bin Informatik-Student.
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Closure")
  fun closures() {
    val quellCode = """
      Adjektiv klickbar:
          Verb klick mich
      .

      Verb registriere das Klickbare:
          Klickbares: klick dich!
      .

      eine Zahl ist 0
      registriere etwas Klickbares:
          die Zahl ist die Zahl + 1
          schreibe die Zeile "Ich wurde zum #{die Zahl}. angeklickt."
      .
    """.trimIndent()

    val erwarteteAusgabe = """
      Ich wurde zum 1. angeklickt.
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Closure zurückgeben")
  fun closureZurückgeben() {
    val quellCode = """
      Adjektiv zählbar:
        Verb(Zahl) zähle weiter
      .
      
      Verb(Zählbares) zähler von der ZahlA zur ZahlB:
        eine Zahl ist die ZahlA minus 1
        
        das Zählbare ist etwas Zählbares:
          wenn die Zahl größer gleich der ZahlB ist:
            eine Zahl ist die ZahlA minus 1
          .
          eine Zahl ist die Zahl plus 1
          die Zahl
        .
        
        gebe das Zählbare zurück
      .
      
      das Zählbare ist zähler von der Zahl 1 zur Zahl 3
      
      Zählbares:
        schreibe die Zeile (zähle weiter) als Zeichenfolge
        schreibe die Zeile (zähle weiter) als Zeichenfolge
        schreibe die Zeile (zähle weiter) als Zeichenfolge
        schreibe die Zeile (zähle weiter) als Zeichenfolge
        schreibe die Zeile (zähle weiter) als Zeichenfolge
      !
    """.trimIndent()

    val erwarteteAusgabe = """
      1
      2
      3
      1
      2
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Objektinitialisierung als Argument")
  fun objektInitialisierungAlsArgument() {
    val quellCode = """
      Deklination Femininum Singular(Person) Plural(Personen)
      Deklination Maskulinum Singular(Name, Namens, Namen, Namen) Plural(Namen)
      
      Nomen Person mit der Zeichenfolge Name:.
      
      Verb begrüße die Person:
        schreibe die Zeichenfolge "Hallo #{der Name der Person}!"
      .
      
      begrüße eine Person mit dem Namen "Max"
    """.trimIndent()

    val erwarteteAusgabe = "Hallo Max!"

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Alias Singular")
  fun aliasSingular() {
    val quellCode = """
      Deklination Femininum Singular(Person) Plural(Personen)
      Deklination Maskulinum Singular(Name, Namens, Namen, Namen) Plural(Namen)
      Deklination Neutrum Singular(Alter) Plural(Alter)
      
      Alias Alter ist Zahl
      Alias Name ist Zeichenfolge
      
      Nomen Person mit dem Namen, einem Alter:
        schreibe die Zeile "#{mein Name} (#{mein Alter} Jahre alt) wurde erstellt!"
      .
      
      die Person ist eine Person mit dem Namen "Lukas", dem Alter 22
      das NeueAlter ist das Alter der Person plus 1
      schreibe die Zeile "#{der Name der Person} ist jetzt #{das NeueAlter} Jahre alt!"
    """.trimIndent()

    val erwarteteAusgabe = """
      Lukas (22 Jahre alt) wurde erstellt!
      Lukas ist jetzt 23 Jahre alt!
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Alias Plural")
  fun aliasPlural() {
    val quellCode = """
      Deklination Femininum Singular(Person) Plural(Personen)
      Deklination Maskulinum Singular(Mensch, Menschen, Menschen, Menschen) Plural(Menschen)
      Deklination Maskulinum Singular(Name, Namens, Namen, Namen) Plural(Namen)
      
      Nomen Person mit der Zeichenfolge Name:.
      
      Alias Mensch ist Person
      
      die Menschen sind einige Menschen [(ein Mensch mit dem Namen "Max"), (eine Person mit dem Namen "Lukas")]
      
      für jeden Menschen:
        schreibe die Zeile (der Name des Menschen)
      .
    """.trimIndent()

    val erwarteteAusgabe = """
      Max
      Lukas
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Alias Fehler")
  fun aliasFehler() {
    val quellCode = """
      Deklination Neutrum Singular(Alter) Plural(Alter)
      Deklination Femininum Singular(Menge) Plural(Mengen)
      
      Alias Menge ist Zahl
      Alias Alter ist Menge
    """.trimIndent()

    assertThatExceptionOfType(GermanSkriptFehler.AliasFehler::class.java).isThrownBy {
        führeGermanSkriptCodeAus(quellCode)
    }
  }

  @Test
  @DisplayName("berechnete Eigenschaften")
  fun berechneteEigenschaften() {
    val quellCode = """
      Deklination Femininum Singular(Person) Plural(Personen)
      Deklination Maskulinum Singular(Name, Namens, Namen, Namen) Plural(Namen)
      
      Nomen Person mit der Zeichenfolge VorName, der Zeichenfolge NachName:.
      
      Implementiere die Person:
        Eigenschaft(Zeichenfolge) Name:
          gebe meinen VorNamen + " " + meinen NachNamen zurück
        .
      .

      die Person ist eine Person mit dem VorNamen "Max", dem NachNamen "Mustermann"
      schreibe die Zeichenfolge (der Name der Person)
    """.trimIndent()

    testeGermanSkriptCode(quellCode, "Max Mustermann")
  }

  @Test
  @DisplayName("Konstante")
  fun konstante() {
    val quellCode = """
      Modul MatheX:
        Konstante PI ist 3,14159265
      .
      
      die Zahl ist 2 * MatheX::PI
      schreibe die Zahl
    """.trimIndent()

    val erwarteteAusgabe = """
      6,2831853
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Verwende Konstante")
  fun verwendeKonstante() {
    val quellCode = """
      Modul MatheX:
        Konstante PI ist 3,14159265
      .
      
      verwende MatheX::PI
      
      schreibe die Zahl PI
    """.trimIndent()

    val erwarteteAusgabe = """
      3,14159265
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Variable überschreibt Konstante")
  fun variableÜberschreibtKonstante() {
    val quellCode = """
      Modul MatheX:
        Konstante PI ist 3,14159265
      .
      
      verwende MatheX
      
      das PI ist 3
      schreibe die Zahl MatheX::PI
      schreibe die Zahl PI
    """.trimIndent()

    val erwarteteAusgabe = """
      3,14159265
      3
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Konstante ist Literal")
  fun konstanteIstLiteral() {
    val quellCode = """
      // eine Konstante kann keine Liste sein
      Konstante PI ist einige Zahlen[1, 2, 3, 4]
    """.trimIndent()

    assertThatExceptionOfType(GermanSkriptFehler.KonstantenFehler::class.java).isThrownBy {
      führeGermanSkriptCodeAus(quellCode)
    }
  }

  @Test
  @DisplayName("Für-Jede-Schleife Reichweiten 1")
  fun reichweiten1() {
    val quellCode = """
      für jede Zahl von 0 bis 3:
        schreibe die Zahl
      .
      schreibe die Zeile ""
      
      für jede Zahl von 2 bis -2:
        schreibe die Zahl
      .
      schreibe die Zeile ""
      
      für jede Zahl von 0,5 bis 3:
        schreibe die Zahl
      .
    """.trimIndent()

    val erwarteteAusgabe = """
      0
      1
      2
      
      2
      1
      0
      -1
      
      0,5
      1,5
      2,5
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Für-Jede-Schleife Reichweiten 2")
  fun reichweiten2() {
    val quellCode = """
      einige Zahlen sind einige Zahlen [2, 3, 5, 7, 11]
      für jeden Index von 0 bis zur AnZahl der Zahlen:
        schreibe die Zahl[Index]
      .
    """.trimIndent()

    val erwarteteAusgabe = """
      2
      3
      5
      7
      11
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Versuche-Fange: Fehler wird gefangen")
  fun versucheFangeFehlerGefangen() {
    val quellCode = """
      versuche:
        die Zahl ist "Hallo" als Zahl
      .
      fange den KonvertierungsFehler:
        schreibe die Zeile (die FehlerMeldung des KonvertierungsFehlers)
      .
      schlussendlich:
        schreibe die Zeile "Ich werde immer ausgeführt!"
      .
    """.trimIndent()

    val erwarteteAusgabe = """
      Die Zeichenfolge 'Hallo' kann nicht in eine Zahl konvertiert werden.
      Ich werde immer ausgeführt!
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Versuche-Fange: kein Fehler wird gewurfen")
  fun versucheFangeErfolg() {
    val quellCode = """
      versuche:
        schreibe die Zeile "Eine erfolgreiche Operation."
      .
      fange den Fehler:
        schreibe die Zeile (die FehlerMeldung des Fehlers)
      .
      schlussendlich:
        schreibe die Zeile "Ich werde immer ausgeführt!"
      .
    """.trimIndent()

    val erwarteteAusgabe = """
      Eine erfolgreiche Operation.
      Ich werde immer ausgeführt!
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Versuche-Fange: Fehler wird nicht gefangen")
  fun versucheFangeNichtGefangen() {
    val quellCode = """
      versuche:
        werfe einen Fehler mit der FehlerMeldung "Ich werde nicht gefangen!"
      .
      fange die Zeichenfolge:
        schreibe die Zeichenfolge
      .
      schlussendlich:
        schreibe die Zeile "Ich werde immer ausgeführt!"
      .
    """.trimIndent()

    try {
      testeGermanSkriptCode(quellCode, "Ich werde immer ausgeführt!\n")
    } catch (fehler: Exception) {
       assertThat(fehler).isInstanceOf(GermanSkriptFehler.UnbehandelterFehler::class.java)
    }
  }

  @Test
  @DisplayName("Werfe Fehler")
  fun werfeFehler() {
    val quellCode = """
      Verb(Zahl) fakultät von der Zahl:
        wenn die Zahl kleiner 0 ist: 
          werfe einen Fehler mit der FehlerMeldung "Die Fakultät von einer negativen Zahl ist undefiniert."
        .
        wenn die Zahl gleich 0 ist: gebe 1 zurück.
        sonst: gebe die Zahl * (fakultät von der Zahl - 1) zurück.
      .
      
      versuche:
        schreibe die Zahl (fakultät von der Zahl 3)
        schreibe die Zahl (fakultät von der Zahl -1)
        schreibe die Zahl (fakultät von der Zahl 5)
      .
      fange die Zahl:
        schreibe die Zeile "Die Zahl sollte nicht gefangen werden"
      .
      fange den Fehler:
        schreibe die Zeile (die FehlerMeldung des Fehlers)
      .
    """.trimIndent()

    val erwarteteAusgabe = """
      6
      Die Fakultät von einer negativen Zahl ist undefiniert.
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Liste (füge hinzu, enthalten)")
  fun liste() {
    val quellCode = """
      die Zahlen sind einige Zahlen[1, 2, 3, 4]
      Zahlen:
        füge die Zahl 5 hinzu
        wenn enthalten die Zahl 5:
          schreibe die Zeile "Die Zahl 5 wurde hinzugefügt!"
        .
      !
    """.trimIndent()

    val erwarteteAusgabe = "Die Zahl 5 wurde hinzugefügt!\n"

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Generics")
  fun generics() {
    val quellCode = """
      Deklination Maskulinum Singular(Test, Tests, Test, Test) Plural(Tests)
      Nomen<Typ> Test mit dem Typ:.
      
      der Test ist ein Test<Zahl> mit der Zahl 5
      schreibe die Zahl des Tests
    """.trimIndent()

    val erwarteteAusgabe = "5\n"

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Liste sortieren")
  fun listeSortieren() {

    fun formatiereListe(liste: Array<Int>) = "[${liste.joinToString(", ")}]"

    fun generiereZahlen(anzahl: Int) = Array(anzahl) {(Math.random() * 100).toInt()}

    fun testeSortierung(testListe: Array<Int>, erwarteteListe: Array<Int>, aufsteigend: Boolean) {
      val aufOderAbCode = if (aufsteigend) "die ZahlA - die ZahlB" else "die ZahlB - die ZahlA"
      val quellCode = """
        die Zahlen sind einige Zahlen${formatiereListe(testListe)}
        die sortiertenZahlen sind Zahlen:
          sortiere euch mit etwas Vergleichendem: $aufOderAbCode.
        !
        schreibe die Zeichenfolge (die sortiertenZahlen als Zeichenfolge)
      """.trimIndent()

      testeGermanSkriptCode(quellCode, formatiereListe(erwarteteListe))
    }

    testeSortierung(arrayOf(3, 2, 1), arrayOf(1, 2, 3), true)
    testeSortierung(arrayOf(1, 2, 3), arrayOf(3, 2, 1), false)
    testeSortierung(arrayOf(4, -99, -10, 100, 23, 11, 5, 42), arrayOf(-99, -10, 4, 5, 11, 23, 42, 100), true)

    for (i in 0..10) {
      val zahlen = generiereZahlen(i * 10)
      testeSortierung(zahlen, zahlen.sortedArray(), true)
    }

  }

  @Test
  @DisplayName("Implementiere Adjektiv mit Typparameter")
  fun implementiereAdjektivMitTypparameter() {
    val quellCode = """
      Deklination Maskulinum Singular(Test, Tests, Test, Test) Plural(Tests)
      
      Adjektiv<Typ> testbar:
        Verb teste den Typ
      .
      
      Nomen Test:.
      
      Implementiere den testbaren<Zeichenfolge> Test:
        Verb teste die Zeichenfolge:
          schreibe die Zeichenfolge
        .
      .
      
      der Test ist ein Test
      Test: teste die Zeichenfolge "Hallo Welt!"!
    """.trimIndent()

    führeGermanSkriptCodeAus(quellCode)
    //testeGermanSkriptCode(quellCode, "Hallo Welt!")
  }

  @Test
  @DisplayName("Implementiere mehrerere Adjektive")
  fun implementiereMehrereAdjektive() {
    val quellCode = """
      Adjektiv gefräßig:
        Verb fütter mich
      .
      
      Adjektiv flauschig:
        Verb kuschel mich
      .
      
      Deklination Maskulinum Singular(Hund, Hunds, Hund, Hund) Plural(Hunde)
      
      Nomen Hund:.
      
      Implementiere den gefräßigen, flauschigen Hund:
        Verb fütter mich:
          schreibe die Zeile "Jam, jam, schmatz, schmatz..."
        .
        
        Verb kuschel mich:
          schreibe die Zeile "entspanntes Ausatmen"
        .
      .
      
      Verb fütter das Gefräßige:
        Gefräßiges: fütter dich!
      .
      
      der HundKIRA ist ein Hund
      fütter den gefräßigen HundKIRA
      kuschel den HundKIRA
    """.trimIndent()

    val erwarteteAusgabe = """
      Jam, jam, schmatz, schmatz...
      entspanntes Ausatmen
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Falscher Typ bei implementierter Schnittsteller")
  fun falscheTypenBeiImplementierterSchnittstelle() {
    val quellCode = """
      Adjektiv zählbar:
        Verb (Zahl) zähle mich weiter
      .
      
      Deklination Maskulinum Singular(Zähler, Zählers, Zähler, Zähler) Plural(Zähler)
      
      Nomen Zähler:
        jene Zahl ist 0
      .
      
      Implementiere den zählbaren Zähler:
        Verb(Zeichenfolge) zähle mich weiter:
          meine Zahl ist meine Zahl plus 1
          gebe meine Zahl als Zeichenfolge zurück
        .
      .
      
      der Zähler ist ein Zähler
      zähle den Zähler weiter
    """.trimIndent()

    assertThatExceptionOfType(GermanSkriptFehler.TypFehler.FalscherSchnittstellenTyp::class.java).isThrownBy {
      führeGermanSkriptCodeAus(quellCode)
    }
  }

  @Test
  @DisplayName("Falscher Typ bei implementierter generischer Schnittstelle")
  fun falscherTypBeiImplementierterGenerischerSchnittstelle() {
    val quellCode = """
      Deklination Maskulinum Singular(Test, Tests, Test, Test) Plural(Tests)
      
      Adjektiv<Typ> testbar:
        Verb teste den Typ
      .
      
      Nomen Test:.
      
      Implementiere den testbaren<Zahl> Test:
        Verb teste die Zeichenfolge Zahl:
          schreibe die Zeile (die Zahl)
        .
      .
    """.trimIndent()

    assertThatExceptionOfType(GermanSkriptFehler.TypFehler.FalscherSchnittstellenTyp::class.java).isThrownBy {
      führeGermanSkriptCodeAus(quellCode)
    }
  }

  @Test
  @DisplayName("Liste von Schnittstellen")
  fun listeVonSchnittstellen() {
    val quellCode = """
      Deklination Maskulinum Singular(Hund, Hunds, Hund, Hund) Plural(Hunde)
      Deklination Femininum Singular(Katze) Plural(Katzen)
      Deklination Neutrum Singular(Tier, Tiers, Tier, Tier) Plural(Tiere)
      
      Adjektiv tierisch:
        Verb melde mich
      .
      
      Nomen Hund:.
      Nomen Katze:.
      
      Implementiere den tierischen Hund:
        Verb melde mich:
          schreibe die Zeile "Woof!"
        .
      .
      
      Implementiere die tierische Katze:
        Verb melde mich:
          schreibe die Zeile "Miauu!"
        .
      .
      
      Verb melde die tierischen Tiere:
        für jedes Tier:
          Tier: melde dich!
        .
      .
      
      die Tiere sind einige Tierische[eine Katze, ein Hund, ein Hund, eine Katze, eine Katze]
      melde die Tiere
    """.trimIndent()

    val erwarteteAusgabe = """
      Miauu!
      Woof!
      Woof!
      Miauu!
      Miauu!
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Bedingungs-Aufrufweise einer Methode")
  fun bedingungsAufrufweiseEinerMethode() {
    val quellCode = """
      die Zahlen sind einige Zahlen [0, 1, 2, 3]

      wenn die Zahlen die Zahl 3 enthalten:
        schreibe die Zeile "3 ist enthalten"
      .
    """.trimIndent()

    testeGermanSkriptCode(quellCode, "3 ist enthalten\n")
  }

  @Test
  @DisplayName("Methodenblock als Ausdruck")
  fun methodenBlockAlsAusdruck() {
    val quellCode = """
      die Zahlen sind einige Zahlen [1, 2, 3]
      das ENTHÄLT ist Zahlen: enthalten die Zahl 4!
      schreibe die Zeile (das ENTHÄLT als Zeichenfolge)
    """.trimIndent()

    testeGermanSkriptCode(quellCode, "falsch\n")
  }

  @Test
  @DisplayName("Funktion mit generischer Liste als Parameter")
  fun funktionMitGenerischerListeAlsParameter() {
    val quellCode = """
      Verb<Typ> drucke die Typen Elemente:
        schreibe die Zeile (die Elemente als Zeichenfolge)
      .
      
      drucke die Elemente einige Zahlen[1, 2, 3]
    """.trimIndent()

    testeGermanSkriptCode(quellCode, "[1, 2, 3]\n")
  }

  @Test
  @DisplayName("Standardbibliothek Zeichenfolge")
  fun standardBibZeichenfolge() {
    val quellCode = """
      eine Zeichenfolge ist "Hallo Welt!"
      schreibe die Zeile (buchstabiere die Zeichenfolge groß)
      schreibe die Zeile (buchstabiere die Zeichenfolge klein)
      schreibe die Zeile (Zeichenfolge: startet mit der Zeichenfolge "Hallo"!) als Zeichenfolge
      schreibe die Zeile (Zeichenfolge: startet mit der Zeichenfolge "Test"!) als Zeichenfolge
      schreibe die Zeile (Zeichenfolge: enthält die Zeichenfolge "Welt"!) als Zeichenfolge
      schreibe die Zeile (Zeichenfolge: enthält die Zeichenfolge "Test"!) als Zeichenfolge
      die ZeichenfolgeX ist wiederhole die Zeichenfolge mit der AnZahl 3
      schreibe die Zeile ZeichenfolgeX
      der IndexA ist ZeichenfolgeX: index von der Zeichenfolge "Welt"!
      schreibe die Zahl IndexA
      der IndexB ist ZeichenfolgeX: index von der Zeichenfolge "Welt" ab dem IndexA + 1!
      schreibe die Zahl IndexB
      der IndexC ist ZeichenfolgeX: letzter_index von der Zeichenfolge "Welt"!
      schreibe die Zahl IndexC
      schreibe die Zeile (teile die Zeichenfolge ab dem Index 6 zum Index (die Länge der Zeichenfolge - 1))

      eine Zeichenfolge ist "Öl Überhänge Rüben"
      schreibe die Zeile (buchstabiere die Zeichenfolge groß)
      schreibe die Zeile (buchstabiere die Zeichenfolge klein)
      schreibe die Zeile (Zeichenfolge: startet mit der Zeichenfolge "Öl Ü"!) als Zeichenfolge
      schreibe die Zeile (Zeichenfolge: startet mit der Zeichenfolge "ÖlT"!) als Zeichenfolge
      schreibe die Zeile (Zeichenfolge: enthält die Zeichenfolge "Rüb"!) als Zeichenfolge
      schreibe die Zeile (Zeichenfolge: enthält die Zeichenfolge "e Rük"!) als Zeichenfolge
    """.trimIndent()

    val erwarteteAusgabe = """
      HALLO WELT!
      hallo welt!
      wahr
      falsch
      wahr
      falsch
      Hallo Welt!Hallo Welt!Hallo Welt!
      6
      17
      28
      Welt
      ÖL ÜBERHÄNGE RÜBEN
      öl überhänge rüben
      wahr
      falsch
      wahr
      falsch
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Closure mit expliziten Parameternamen")
  fun closureMitExplizitenParameterNamen() {
    val quellCode = """
      die Zahlen sind einige Zahlen[2, 3, 5, 7, 11, 13]
      die sortiertenZahlen sind sortiere die Zahlen mit etwas Vergleichendem(X, Y): das Y - das X.
      schreibe die Zeile sortierteZahlen als Zeichenfolge
    """.trimIndent()

    val erwarteteAusgabe = "[13, 11, 7, 5, 3, 2]\n"

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Bedingungen als Ausdruck")
  fun bedingungenAlsAusdruck() {
    val quellCode = """
      eine Zahl ist 42
      eine Zahl ist wenn die Zahl mod 2 gleich 0 ist: die Zahl / 2. sonst: die Zahl * 2.
      schreibe die Zahl
    """.trimIndent()

    testeGermanSkriptCode(quellCode, "21\n")
  }

  @Test
  @DisplayName("Ungültige Bedingung als Ausdruck")
  fun ungültigeBedingungAlsAusdruck() {
    val quellCode = """
      eine Zahl ist wenn wahr: 32. sonst: "Hallo".
    """.trimIndent()

    assertThatExceptionOfType(GermanSkriptFehler.TypFehler.TypenUnvereinbar::class.java).isThrownBy {
      führeGermanSkriptCodeAus(quellCode)
    }
  }

  @Test
  @DisplayName("Filter Map Reduce")
  fun filterMapReduce() {
    val quellCode = """
      Deklination Femininum Singular(Summe) Plural(Summen)

      die Zahlen sind einige Zahlen[1, 2, 3, 4, 5, 6, 7, 8, 10]
      
      die quadratischeSumme ist Zahlen:
        filter euch mit etwas Bedingtem: die Zahl mod 2 = 0.!:
        transformiere<Zahl> euch mit etwas Transformierendem: die Zahl hoch 2.!:
        reduziere<Zahl> euch mit dem AnfangsWert 0, etwas Reduzierendem: der Akkumulator + die Zahl.!
      
      schreibe die Zahl die quadratischeSumme
    """.trimIndent()

    testeGermanSkriptCode(quellCode, "220\n")
  }

  @Test
  @DisplayName("Methoden-Subjekt-Aufruf")
  fun methodenSubjektAufruf() {
    val quellCode = """
      die Zeichenfolge ist "Hallo Welt"
      schreibe die Zeile (die Zeichenfolge: startet mit der Zeichenfolge "Hallo"!) als Zeichenfolge
      schreibe die Zeile (die Zeichenfolge: endet mit der Zeichenfolge "Welt"!) als Zeichenfolge
      
    """.trimIndent()

    val erwarteteAusgabe = """
      wahr
      wahr
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Rückgabetyp mit Name")
  fun rückgabeTypMitName() {
    val quellCode = """
      Verb(Zahl) lese_zahl:
        gebe 42 zurück
      .
      
      Verb lese (eine Zahl):
        gebe 42 zurück
      .
      
      die Zahl ist lese eine Zahl
      schreibe die Zahl
    """.trimIndent()

    testeGermanSkriptCode(quellCode, "42\n")
  }

  @Test
  @DisplayName("Überprüfe Schnittstelle mit Objekt-Rückgabetyp")
  fun schnittstelleMitObjektRückgabeTyp() {
    val quellCode = """
      Adjektiv testbar:
        Verb teste (die Zeichenfolge)
      .
      
      Deklination Maskulinum Singular(Test, Tests, Test, Test) Plural(Tests)
      
      Nomen Test:.
      
      Implementiere den testbaren Test:
        Verb(Zeichenfolge) teste die Zeichenfolge:
          gebe "Test" zurück
        .
      .
    """.trimIndent()

    assertThatExceptionOfType(GermanSkriptFehler.TypFehler.RückgabeObjektErwartet::class.java).isThrownBy {
      führeGermanSkriptCodeAus(quellCode)
    }
  }

  @Test
  @DisplayName("Typ-Check Nomen")
  fun typCheckNomen() {
    val quellCode = """
      die Zahl ist 5
      wenn die Zahl eine Zahl ist:
        schreibe die Zeile "Die Zahl ist eine Zahl!"
      .
      sonst wenn die Zahl eine Zeichenfolge ist:
        schreibe die Zeile "Die Zahl ist eine Zeichenfolge!"
      .
    """.trimIndent()

    testeGermanSkriptCode(quellCode, "Die Zahl ist eine Zahl!\n")
  }

  @Test
  @DisplayName("Typ-Check Adjektiv")
  fun typCheckAdjektiv() {
    val quellCode = """
      Adjektiv testbar:.
      
      Deklination Maskulinum Singular(Test, Tests, Test, Test) Plural(Tests)
      
      Nomen Test:.
      
      Implementiere den testbaren Test:.
      
      der Test ist ein Test
      
      wenn der Test testbar ist:
        schreibe die Zeile "Der Test ist testbar!"
      .
      sonst:
        schreibe die Zeile "Der Test ist untestbar!"
      .
    """.trimIndent()

    val erwarteteAusgabe = "Der Test ist testbar!\n"

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Typecasting Vererbung")
  fun typeCastingVererbung() {
    val quellCode = """
      Deklination Femininum Singular(Klasse) Plural(Klassen)

      Nomen ElternKlasse:.
      
      Nomen KindKlasse mit der Zeichenfolge als ElternKlasse:.
      
      die Klassen sind einige ElternKlassen [
        eine ElternKlasse, 
        eine KindKlasse mit der Zeichenfolge "Hallo"
      ]
      
      eine Klasse ist die Klasse[1] als KindKlasse
      schreibe die Zeile (die Zeichenfolge der Klasse)
      
      versuche:
        eine Klasse ist die Klasse[0] als KindKlasse
      .
      fange den KonvertierungsFehler:
        schreibe die Zeile "Fehler gefangen!"
      .
    """.trimIndent()

    val erwarteteAusgabe = """
      Hallo
      Fehler gefangen!
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Summe")
  fun summe() {
    val quellCode = """
      die Zahlen sind einige Zahlen[1, 2, 3, 4, 5]
      die Summe ist summiere die Zahlen
      schreibe die Zeile (Summe als Zeichenfolge)
    """.trimIndent()

    val erwarteteAusgabe = "15\n"

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Sortiere Zeichenfolgen")
  fun sortiereZeichenfolgen() {
    val quellCode = """
      die Zeichenfolgen sind einige Zeichenfolgen ["Lukas", "Michelle", "Arnold", "Peter", "Tom", "Thomas", "Lucas"]
      
      die sortiertenZeichenfolgen sind sortiere die Zeichenfolgen
      
      für jede sortierteZeichenfolge:
        schreibe die Zeile (sortierteZeichenfolge)
      .
      
      schreibe die Zeile ""
      
      die absteigendenZeichenfolgen sind sortiere die Zeichenfolgen absteigend
      für jede absteigendeZeichenfolge:
        schreibe die Zeile (absteigendeZeichenfolge)
      .
    """.trimIndent()

    val erwarteteAusgabe = """
      Arnold
      Lucas
      Lukas
      Michelle
      Peter
      Thomas
      Tom
      
      Tom
      Thomas
      Peter
      Michelle
      Lukas
      Lucas
      Arnold
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Kann keine Booleans sortieren")
  fun kannKeineBooleansSortieren() {
    val quellCode = """
      die Booleans sind einige Booleans[wahr, falsch, wahr, wahr, falsch]
      sortiere die Booleans
      summiere die Booleans
      schreibe die Zeile "#{Booleans}"
    """.trimIndent()

    assertThatExceptionOfType(GermanSkriptFehler.Undefiniert.Methode::class.java).isThrownBy {
      führeGermanSkriptCodeAus(quellCode)
    }
  }

  @Test
  @DisplayName("Listen Iterator")
  fun listenIterator() {
    val quellCode = """
      die Zahlen sind einige Zahlen[2, 3, 5, 7, 11]
      der Iterator ist Zahlen: hole den Iterator!
      solange der Iterator weiter läuft:
        die Zahl ist Iterator: hole die Zahl!
        schreibe die Zahl
      .
    """.trimIndent()

    val erwarteteAusgabe = """
      2
      3
      5
      7
      11
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Reichweiten Iterator")
  fun reichWeitenIterator() {
    val quellCode = """
      die ReichWeite ist eine ReichWeite mit dem Start 3, dem Ende 9
      der Iterator ist ReichWeite: hole den Iterator!
      solange der Iterator weiter läuft:
        die Zahl ist Iterator: hole die Zahl!
        schreibe die Zahl
      .
    """.trimIndent()

    val erwarteteAusgabe = """
      3
      4
      5
      6
      7
      8
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }

  @Test
  @DisplayName("Anonyme Klasse mit Eigenschaften")
  fun anonymeKlasseMitEigenschaften() {
    val quellCode = """
      der Iterator ist etwas Iterierendes<Zahl>:
        jenes X ist 1
        jenes Y ist 1
        
        Verb(Boolean) läuft weiter:
          gebe mein X kleiner 15 zurück
        .
        
        Verb hole (die nächsteZahl):
          das T ist mein X
          mein X ist mein Y
          mein Y ist das T + mein Y
          gebe das T zurück
        .
      .
      
      schreibe die Zahl (das X des Iterators)
      schreibe die Zahl (das Y des Iterators)
      
      schreibe die Zeile ""
      
      solange der Iterator weiter läuft:
        schreibe die Zahl (Iterator: hole die nächsteZahl!)
      .
      
      schreibe die Zeile ""
      
      schreibe die Zahl (das X des Iterators)
      schreibe die Zahl (das Y des Iterators)
    """.trimIndent()

    val erwarteteAusgabe = """
      1
      1
      
      1
      1
      2
      3
      5
      8
      13
      
      21
      34
      
    """.trimIndent()

    testeGermanSkriptCode(quellCode, erwarteteAusgabe)
  }
}