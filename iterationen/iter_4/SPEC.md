# Iteration 4

## hinzugefügte Features
- Lambdas
- Alias
- berechnete Eigenschaften einer Klasse
- Konstanten
- Für-Jede-Schleife über Zahlen

## Lambdas
`etwas Bezeichner: Sätze.`

Lambdas funktionieren über die Schnittstellen (Adjektive). Wenn eine Schnittstelle nur eine einzige
Methode definiert, dann kann man für diese Schnittstelle ein Lambda erstellen. 
Der Bezeichner, der nach dem Vornomen `etwas` kommt ist das nominalisierte Adjektiv der Schnittstelle.

```
Adjektiv klickbar:
    Verb klick mich
.

Verb registriere das Klickbare:
    Klickbare: klick mich!
.

eine Zahl ist 0
registriere etwas Klickbares:
    die Zahl ist die Zahl plus 1
    schreibe die Zeile "Ich wurde zum #{die Zahl}. angeklickt."
.
```

## Alias
`Alias BezeichnerP ist Typ`

Beispiel:

`Alias Alter ist Zahl`

Ein Typ kann über den Alias einen andern Namen bekommen, über dem man auf diesen Typ verweisen kann.

## berechnete Eigenschaften einer Klasse
`Eigenschaft(Typ) BezeichnerN für Typ: Sätze.`

Eine berechnete Eigenschaft ist eine Eigenschaft die sich aus anderen Eigenschaften der Klasse ergibt.

Beispiel:

```
Eigenschaft(Zeichenfolge) Name für Person:
    gebe meinen VorNamen + " " meinen NachNamen zurück
.

die Person ist eine Person mit dem VorNamen "Max", dem NachNamen "Mustermann"
schreibe die Zeichenfolge (der Name der Person) // Max Mustermann
```

## Definieren einer Konstante
`Konstante BezeichnerF ist Literal`

Konstanten sind unveränderbar und können nur einmal zugewiesen werden. Nur Zahlen-, Zeichenfolgen- oder Boolean-Literale können einer Konstante zugewiesen werden.

Beispiel: `Konstante PI ist 3,14159265359`


## Für-Jede-Schleife über Zahlen

Die untere Grenze ist inklusiv und die obere Grenze exklusiv.

Beispiel:

```
für jede Zahl von 1 bis 12:
    schreibe die Zahl 
.


die Zahlen sind einige Zahlen [1, 2, 3, 4, 5]
für jeden Index von 0 bis zur Anzahl der Zahlen:
    schreibe die Zahl[Index]
.
```