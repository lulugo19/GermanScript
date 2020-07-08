# Iteration 2

# hinzugefügte Features
- mehrere Dateien, Standardbibliothek wird automatisch geladen
- Symbole
- Typ-Umwandlung
- Klassen
    - Definition
        - Felder
        - (Konstruktor)
    - Instanziierung
- Methoden
    - Definition
    - Methodenblock
- Zugriff auf Felder

## Symbole
Symbole sind Nomen mit nur einem Buchstaben also `X`, `Y`, `A`, `Z`, ...

Sie können als Bezeichner verwendet werden aber nur im Singular.
Der Genus ist automatisch immer `Neutrum`.

Abkürzung für: `Deklination Neutrum Singular(X,X,X,X)`

```
Verb(Zahl) fakultät von der Zahl X:
    wenn das X gleich 0: gebe 1 zurück.
    sonst: gebe das X * (fakultät von dem X (das X - 1)) zurück.
.

die Zahlen sind einige Zahlen [1, 2, 3, 4]
für jede Zahl X:
    schreibe die Zahl X
.
```

## Typ-Umwandlung

### implizit
Ein vererbter Typ lässt sich immer einer Variable mit dem Typ des Elterntyps zuweisen.

```
Nomen Person:.

Nomen Student als Person:.

die Person Lukas ist Student.
```

### explizit
`Ausdruck als Typ`

Beispiel:
`die Zahl ZweiUndVierzig ist "42" als Zahl`

Es kann eine Typumwandlung von einem Typ zu einem anderen Typ für jeden Typen definiert werden:

Syntax: 

```
alias Vorname ist Zeichenfolge
alias Nachname ist Zeichenfolge

Nomen Person mit dem Vornamen, dem Nachnamen:.

Als Zeichenfolge für Person:
    gebe meinen Namen Vorname + " " + meinen Namen Nachname zurück
.

die Person Lukas ist eine Person mit dem "Lukas", dem Nachnamen "Gobelet"
drucke die Zeichenfolge Lukas als Zeichenfolge // Lukas Gobelet
```

## Klassen

### Definieren einer Klasse

`Nomen BezeichnerP [als KlasseP] [mit Felder]: Konstruktor.`

Felder: `Kommaliste(ArtikelD TypD [BezeichnerP])`

#### Konstruktor

Konstruktor: `Sätze`

Der Konstruktor ist dafür da das Objekt zu initialisieren.
In dem Konstruktor können die Demonstativpronomen `diese` oder `jene`
verwendet werden um Felder zu erstellen, auf die man von außen nicht zugreifen kann.

Der Konstruktor ist eine spezielle [Methode](#definieren-einer-methode) und es gelten
die Regeln für Methoden.

Beispiel:
```
Nomen Person mit
    der Zeichenfolge Vorname,
    der Zeichenfolge Nachname,
    der Zahl Alter:

    dieser VollerName ist Vorname + " " + Nachname
    // jener VollerName ist Vorname + " " + Nachname
.

Nomen Student mit 
    einer Zeichenfolge Studiengang,
    einer Zahl Semester:.
```


### Instanziieren eines Objekts einer Klasse
`Bezeichner [mit Kommaliste(ArtikelDb BezeichnerD [AusdruckD])`]

Die Felder können in beliebiger Reihenfolge stehen.

Beispiel:

`die Person Donald ist eine Person mit dem Vornamen "Donald", dem Nachnamen "Duck", dem Alter 42`

### Zugriff auf Felder eines Objekts
mit dem Genitiv: `ArtikelNb Feld ArtikelGb AusdruckG`

Beispiel: `der Name des Lukas`

mit dem Dativ `ArtikelNb Feld von ArtikelDb AusdruckD`

Beispiel: `der Name von dem Lukas`

## Methoden

### Definieren einer Methode

`Verb[(Typ)] für Typ Verb [mir|mich] Parameter [Suffix]`

Das Verb einer Methode sollte im Imperativ stehen. Außerdem kann das Verb optional noch einen Suffix bekommen,
der dann bei dem Methodenaufruf am Ende stehen muss.

Innerhalb einer Methode kann man direkt ohne Methodenblock auf eigene Methoden zugreifen.

Beispiel:
```
Verb(Zeichenfolge) für Person stelle mich mit der Zeichenfolge Begrüßung, der Zeichenfolge LetzterSatz vor:
    zurück Begrüßung + ", " + "mein Name ist " + mein Name " und ich bin " + mein Alter " Jahre alt." + LetzterSatz.
```

### Methodenblock

`Bezeichner: Sätze!`

Um eine Methode aufzurufen gibt es den sogenannten Methodenblock. Man startet einen neuen Block mit dem Bezeichner
des Objekts, auf den man die Methode/n aufrufen möchte. Innerhalb des Blocks kann man jetzt die Methoden ganz normal wie Funktionen
aufrufen. Hat eine Funktion die gleiche Signatur wie eine Methode, wird die Funktion überschattet. Der Block endet diesmal nicht
mit einem `.` sondern einem `!`.

In Methodenblöcken kann auf Felder des Objekts mit `dein` zugegriffen werden. Wenn das Objekt eine Liste ist wird stattdessen `eure` verwendet.

Wenn in einer Methodendefinition `mir` (Dativ) oder `mich` (Akkusativ) verwendet wurde, wird diese bei allen Objekten mit `dir`
oder `dich` und bei Listen mit `euch` oder `euren` ersetzt.

Beispiel:

```
die Person Rick ist eine Person mit dem Vornamen "Rick", dem Nachnamen "Sanchez", dem Alter 70
Rick: stelle dich mit der Begrüßung "Woooobeeewoobeedubdub!", dem Nachwort "Rülps!" vor!
```

```
Verb(Verbindung) für Client verbinde mich:.
Verb für Client sende die Nachricht mit dem Flag:.

der Client ist Client mit ...
Client: verbinde dich
        sende die Nachricht "Hallo Welt" mit dem Flag "X"!
```