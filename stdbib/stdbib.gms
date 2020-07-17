Deklination Femininum Singular(Zahl) Plural(Zahlen)
Deklination Femininum Singular(Zeichenfolge) Plural(Zeichenfolgen)
Deklination Neutrum Singular(Boolean) Plural(Booleans)
Deklination Femininum Singular(Zeile) Plural(Zeilen)
Deklination Femininum Singular(Liste) Plural(Listen)
Deklination Neutrum Singular(Element, Elements, Element, Element) Plural(Elemente, Elemente, Elementen, Elemente)
Deklination Maskulinum Singular(Index) Plural(Indizes)
Deklination Neutrum Singular(Minimum, Minimums, Minimum, Minimum) Plural(Minima)
Deklination Neutrum Singular(Maximum, Maximums, Maximum, Maximum) Plural(Maxima)
Deklination Maskulinum Singular(Separator, Separators, Separator, Separator) Plural(Separatoren)

// Standardausgabe
Verb schreibe die Zeichenfolge: intern. // print
Verb schreibe die Zeichenfolge Zeile: intern. // println
Verb schreibe die Zahl: intern.

// Standardeingabe
Verb(Zeichenfolge) lese: intern. // readline

// mathematische Funktionen
Verb(Zahl) runde die Zahl: intern.
Verb(Zahl) runde die Zahl auf: intern.
Verb(Zahl) runde die Zahl ab: intern.
Verb(Zahl) sinus von der Zahl: intern.
Verb(Zahl) cosinus von der Zahl: intern.
Verb(Zahl) tangens von der Zahl: intern.

Verb(Zahl) maximum von der ZahlA, der ZahlB:
    wenn die ZahlA > (die ZahlB) ist: gebe die ZahlA zurück.
    sonst: gebe die ZahlB zurück.
.
Verb(Zahl) minimum von der ZahlA, der ZahlB:
    wenn die ZahlA < (die ZahlB) ist: gebe die ZahlA zurück.
    sonst: gebe die ZahlB zurück.
.
Verb(Zahl) betrag von der Zahl:
    wenn die Zahl >= 0 ist: gebe die Zahl zurück.
    sonst: gebe minus die Zahl zurück.
.

// Random
Verb(Zahl) randomisiere: intern. // gibt Zahl zwischen 0 und 1 zurück
Verb(Zahl) randomisiere zwischen der Zahl Minimum, der Zahl Maximum: intern.

// String Funktionen
Verb(Zeichenfolge) buchstabiere die Zeichenfolge groß: intern.   // toUpperCase
Verb(Zeichenfolge) buchstabiere die Zeichenfolge klein: intern.  // toLowerCase
Verb(Zeichenfolgen) trenne die Zeichenfolge zwischen der Zeichenfolge Separator: intern.

/*
// Listen Methoden
Verb(Boolean) für Liste beinhaltet das Element: intern.
Verb für Liste füge das Element hinzu: intern.
Verb für Liste lösche das Element an der Zahl Index: intern.
*/