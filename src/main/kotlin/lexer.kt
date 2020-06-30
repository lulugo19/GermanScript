import java.util.*
enum class Assoziativität {
    LINKS,
    RECHTS,
}

enum class OperatorKlasse(kasus: Kasus) {
    ARITHMETISCH(Kasus.AKKUSATIV),
    VERGLEICH(Kasus.DATIV),
    LOGISCH(Kasus.AKKUSATIV),
}

enum class Operator(val bindungsKraft: Int, val assoziativität: Assoziativität, klasse: OperatorKlasse) {
    ODER(1, Assoziativität.LINKS, OperatorKlasse.LOGISCH),
    UND(2, Assoziativität.LINKS, OperatorKlasse.LOGISCH),
    GLEICH(3, Assoziativität.LINKS, OperatorKlasse.ARITHMETISCH),
    UNGLEICH(3, Assoziativität.LINKS, OperatorKlasse.VERGLEICH),
    GRÖßER(3, Assoziativität.LINKS, OperatorKlasse.VERGLEICH),
    KLEINER(3, Assoziativität.LINKS, OperatorKlasse.LOGISCH),
    GRÖSSER_GLEICH(3, Assoziativität.LINKS, OperatorKlasse.VERGLEICH),
    KLEINER_GLEICH(3, Assoziativität.LINKS, OperatorKlasse.VERGLEICH),
    PLUS(4, Assoziativität.LINKS, OperatorKlasse.ARITHMETISCH),
    MINUS(4, Assoziativität.LINKS, OperatorKlasse.ARITHMETISCH),
    MAL(5, Assoziativität.LINKS, OperatorKlasse.ARITHMETISCH),
    GETEILT(5, Assoziativität.LINKS, OperatorKlasse.ARITHMETISCH),
    MODULO(5, Assoziativität.LINKS, OperatorKlasse.ARITHMETISCH),
    HOCH(6, Assoziativität.RECHTS, OperatorKlasse.ARITHMETISCH),

    NEGATION(3, Assoziativität.RECHTS, OperatorKlasse.ARITHMETISCH),
}

// Genus (Geschlecht)
enum class Genus {
    MASKULINUM,
    FEMININUM,
    NEUTRUM,
}

// Kasus (Fall)
enum class Kasus(val anzeigeName: String) {
    NOMINATIV("Nominativ"),
    GENITIV("Genitiv"),
    DATIV("Dativ"),
    AKKUSATIV("Akkusativ"),
}

// Numerus (Anzahl)
enum class Numerus(val anzeigeName: String) {
    SINGULAR("Singular"),
    PLURAL("Plural"),
}

data class Form(val bestimmt: Boolean, val genus: Genus?, val kasus: Kasus, val numerus: Numerus)

data class Token(val typ: TokenTyp, val wert: String, val anfang: Position, val ende: Position) {
    // um die Ausgabe zu vereinfachen
    // override fun toString(): String = typ.toString()

    fun <T: TokenTyp> toTyped() = TypedToken<T>(typ as T, wert, anfang, ende)

    open class Position(val zeile: Int, val spalte: Int) {
        object Ende: Token.Position(-1, -1)

        override fun toString(): String {
            return "($zeile, $spalte)"
        }
    }
}

// für den Parser gedacht
data class TypedToken<out T : TokenTyp>(val typ: T, val wert: String, val anfang: Token.Position, val ende: Token.Position) {
    fun toUntyped()  = Token(typ, wert, anfang, ende)
}

sealed class TokenTyp(val anzeigeName: String) {
    override fun toString(): String = javaClass.simpleName

    object FEHLER: TokenTyp("'Fehler'")

    // Schlüsselwörter
    object DEKLINATION: TokenTyp("'Deklination'")
    data class GENUS(val genus: Genus): TokenTyp("'Genus'")
    object GEBE: TokenTyp("'gebe'")
    object ZURÜCK: TokenTyp("'zurück'")
    object WENN: TokenTyp("'wenn'")
    object DANN: TokenTyp("'dann'")
    object SONST: TokenTyp("'sonst'")
    object SOLANGE: TokenTyp("'solange")
    object ALS: TokenTyp("'Als'")
    object PLURAL: TokenTyp("'Plural'")
    object SINGULAR: TokenTyp("'Singular'")
    object FORTFAHREN: TokenTyp("'fortfahren'")
    object ABBRECHEN: TokenTyp("'abbrechen'")
    object VERB: TokenTyp("'Verb'")
    object NOMEN: TokenTyp("'Nomen'")
    object ADJEKTIV: TokenTyp("'Adjektiv'")
    object ALIAS: TokenTyp("'Alias'")
    object MODUL: TokenTyp("'Modul'")
    object INTERN: TokenTyp("'intern'")

    // Artikel und Präpositionen
    data class JEDE(val genus: Genus): TokenTyp("'jeder' oder 'jede' oder 'jedes'")
    data class ZUWEISUNG(val numerus: EnumSet<Numerus>): TokenTyp("'ist' oder 'sind' oder '='")
    sealed class ARTIKEL(anzeigeName: String): TokenTyp(anzeigeName) {
        object BESTIMMT: ARTIKEL("bestimmter Artikel")
        object UMBESTIMMT: ARTIKEL("umbestimmter Artikel")
    }
    //Symbole
    object OFFENE_KLAMMER: TokenTyp("'('")
    object GESCHLOSSENE_KLAMMER: TokenTyp("')'")
    object KOMMA: TokenTyp("','")
    object PUNKT: TokenTyp("'.'")
    object DOPPELPUNKT: TokenTyp("':'")
    object DOPPEL_DOPPELPUNKT: TokenTyp("'::'")
    object SEMIKOLON: TokenTyp("';'") // Semikolon
    object NEUE_ZEILE: TokenTyp("neue Zeile")
    object EOF: TokenTyp("'EOF'")
    data class OPERATOR(val operator: Operator): TokenTyp("Operator")

    // Identifier
    data class BEZEICHNER_KLEIN(val name: String): TokenTyp("bezeichner")
    data class BEZEICHNER_GROSS(val name: String): TokenTyp("Bezeichner")

    // Literale
    data class BOOLEAN(val boolean: Boolean): TokenTyp("'richtig' oder 'falsch'")
    data class ZAHL(val zahl: Double): TokenTyp("Zahl")
    data class ZEICHENFOLGE(val zeichenfolge: String): TokenTyp("Zeichenfolge")

    object UNDEFINIERT: TokenTyp("undefiniert")
}


private val SYMBOL_MAPPING = mapOf<Char, TokenTyp>(
    '(' to TokenTyp.OFFENE_KLAMMER,
    ')' to TokenTyp.GESCHLOSSENE_KLAMMER,
    ',' to TokenTyp.KOMMA,
    ';' to TokenTyp.SEMIKOLON,
    '.' to TokenTyp.PUNKT,
    ':' to TokenTyp.DOPPELPUNKT,
    '!' to TokenTyp.OPERATOR(Operator.NEGATION),
    ';' to TokenTyp.SEMIKOLON,
    '+' to TokenTyp.OPERATOR(Operator.PLUS),
    '-' to TokenTyp.OPERATOR(Operator.MINUS),
    '*' to TokenTyp.OPERATOR(Operator.MAL),
    '/' to TokenTyp.OPERATOR(Operator.GETEILT),
    '^' to TokenTyp.OPERATOR(Operator.HOCH),
    '%' to TokenTyp.OPERATOR(Operator.MODULO),
    '=' to TokenTyp.ZUWEISUNG(EnumSet.of(Numerus.SINGULAR, Numerus.PLURAL)),
    '>' to TokenTyp.OPERATOR(Operator.GRÖßER),
    '<' to TokenTyp.OPERATOR(Operator.KLEINER),
    '&' to TokenTyp.UNDEFINIERT,
    '|' to TokenTyp.UNDEFINIERT
)

private val DOPPEL_SYMBOL_MAPPING = mapOf<String, TokenTyp>(
    "==" to TokenTyp.OPERATOR(Operator.GLEICH),
    "!=" to TokenTyp.OPERATOR(Operator.UNGLEICH),
    ">=" to TokenTyp.OPERATOR(Operator.GRÖSSER_GLEICH),
    "<=" to TokenTyp.OPERATOR(Operator.KLEINER_GLEICH),
    "&&" to TokenTyp.OPERATOR(Operator.UND),
    "||" to TokenTyp.OPERATOR(Operator.ODER),
    "::" to TokenTyp.DOPPEL_DOPPELPUNKT
)


private val WORT_MAPPING = mapOf<String, TokenTyp>(
    // Schlüsselwörter
    "Deklination" to TokenTyp.DEKLINATION,
    "Maskulinum" to TokenTyp.GENUS(Genus.MASKULINUM),
    "Femininum" to TokenTyp.GENUS(Genus.FEMININUM),
    "Neutrum" to TokenTyp.GENUS(Genus.NEUTRUM),
    "Nomen" to TokenTyp.NOMEN,
    "Verb" to TokenTyp.VERB,
    "intern" to TokenTyp.INTERN,
    "Adjektiv" to TokenTyp.ADJEKTIV,
    "Alias" to TokenTyp.ALIAS,
    "gebe" to TokenTyp.GEBE,
    "zurück" to TokenTyp.ZURÜCK,
    "wenn" to TokenTyp.WENN,
    "dann" to TokenTyp.DANN,
    "sonst" to TokenTyp.SONST,
    "solange" to TokenTyp.SOLANGE,
    "fortfahren" to TokenTyp.FORTFAHREN,
    "abbrechen" to TokenTyp.ABBRECHEN,
    "als" to TokenTyp.ALS,
    "Singular" to TokenTyp.SINGULAR,
    "Plural" to TokenTyp.PLURAL,
    "Modul" to TokenTyp.MODUL,
    "jede" to TokenTyp.JEDE(Genus.FEMININUM),
    "jeden" to TokenTyp.JEDE(Genus.MASKULINUM),
    "jedes" to TokenTyp.JEDE(Genus.NEUTRUM),

    // Werte
    "wahr" to TokenTyp.BOOLEAN(true),
    "falsch" to TokenTyp.BOOLEAN(false),

    // Operatoren
    "ist" to TokenTyp.ZUWEISUNG(EnumSet.of(Numerus.SINGULAR)),
    "sind" to TokenTyp.ZUWEISUNG(EnumSet.of(Numerus.PLURAL)),
    "gleich" to TokenTyp.OPERATOR(Operator.GLEICH),
    "ungleich" to TokenTyp.OPERATOR(Operator.UNGLEICH),
    "und" to TokenTyp.OPERATOR(Operator.UND),
    "oder" to TokenTyp.OPERATOR(Operator.ODER),
    "kleiner" to TokenTyp.OPERATOR(Operator.KLEINER),
    "größer" to TokenTyp.OPERATOR(Operator.GRÖßER),
    "plus" to TokenTyp.OPERATOR(Operator.PLUS),
    "minus" to TokenTyp.OPERATOR(Operator.MINUS),
    "mal" to TokenTyp.OPERATOR(Operator.MAL),
    "durch" to TokenTyp.OPERATOR(Operator.GETEILT),
    "hoch" to TokenTyp.OPERATOR(Operator.HOCH),
    "modulo" to TokenTyp.OPERATOR(Operator.MODULO),

    // Artikel
    "der" to TokenTyp.ARTIKEL.BESTIMMT,
    "die" to TokenTyp.ARTIKEL.BESTIMMT,
    "das" to TokenTyp.ARTIKEL.BESTIMMT,
    "den" to TokenTyp.ARTIKEL.BESTIMMT,
    "ein" to TokenTyp.ARTIKEL.UMBESTIMMT,
    "eine" to TokenTyp.ARTIKEL.UMBESTIMMT,
    "eines" to TokenTyp.ARTIKEL.UMBESTIMMT,
    "einer" to TokenTyp.ARTIKEL.UMBESTIMMT,
    "einige" to TokenTyp.ARTIKEL.UMBESTIMMT,
    "einigen" to TokenTyp.ARTIKEL.UMBESTIMMT,
    "einiger" to TokenTyp.ARTIKEL.UMBESTIMMT
)

class Lexer(val quellcode: String) {
    private var iterator = Peekable(quellcode.iterator())
    private var zeilenIndex = 0

    private val currentTokenPos: Token.Position get() = Token.Position(zeilenIndex, iterator.index)
    private val nextTokenPos: Token.Position get() = Token.Position(zeilenIndex, iterator.index + 1)
    private val eofToken = Token(TokenTyp.EOF, "EOF", Token.Position.Ende, Token.Position.Ende)

    fun tokeniziere() : Sequence<Token> = sequence {
        var inMehrZeilenKommentar = false
        for ((zeilenIndex, zeile) in quellcode.lines().map(String::trim).withIndex()) {
            this@Lexer.zeilenIndex = zeilenIndex
            var kannWortLesen = true
            if (zeile == "") {
                continue
            }
            iterator = Peekable(zeile.iterator())
            while (iterator.peek() != null) {
                val zeichen = iterator.peek()!!
                // ignoriere Kommentare
                if (inMehrZeilenKommentar) {
                    if (zeichen == '*' && iterator.peekDouble() == '/') {
                        iterator.next()
                        iterator.next()
                        inMehrZeilenKommentar = false
                    }
                    break
                }
                if (zeichen == '/' && iterator.peekDouble() == '/') {
                    iterator.next()
                    iterator.next()
                    break
                }
                if (zeichen == '/' && iterator.peekDouble() == '*') {
                    iterator.next()
                    iterator.next()
                    inMehrZeilenKommentar = true
                    break
                }
                if (zeichen == ' ') {
                    iterator.next()
                    kannWortLesen = true
                    continue
                }
                yieldAll(when {
                    SYMBOL_MAPPING.containsKey(zeichen) -> symbol().also { kannWortLesen = true }
                    zeichen.isDigit() -> zahl().also { kannWortLesen = false }
                    zeichen == '"' -> zeichenfolge().also { kannWortLesen = false }
                    kannWortLesen -> wort().also { kannWortLesen = false }
                    else -> throw GermanScriptFehler.SyntaxFehler.LexerFehler(
                        Token(
                            TokenTyp.FEHLER,
                            zeichen.toString(),
                            currentTokenPos,
                            nextTokenPos
                        )
                    )
                })
            }

            yield(Token(
                TokenTyp.NEUE_ZEILE,
                "\\n",
                currentTokenPos,
                nextTokenPos
            ))
        }
        while (true) {
            yield(eofToken)
        }
    }

    private fun symbol(): Sequence<Token> = sequence {
        val startPos = currentTokenPos
        var symbolString = iterator.next()!!.toString()
        val potenziellesDoppelSymbol = symbolString + (iterator.peek()?: '\n')
        val tokenTyp = when {
          DOPPEL_SYMBOL_MAPPING.containsKey(potenziellesDoppelSymbol) -> {
              iterator.next()
              symbolString = potenziellesDoppelSymbol
              DOPPEL_SYMBOL_MAPPING.getValue(potenziellesDoppelSymbol)
          }
          SYMBOL_MAPPING.containsKey(symbolString[0]) -> {
              SYMBOL_MAPPING.getValue(symbolString[0])
          }
          else -> {
              TokenTyp.UNDEFINIERT
          }
        }
        val endPos = currentTokenPos
        if (tokenTyp is TokenTyp.UNDEFINIERT) {
            val fehlerToken = Token(TokenTyp.FEHLER, symbolString, startPos, endPos)
            throw GermanScriptFehler.SyntaxFehler.LexerFehler(fehlerToken)
        }
        yield(Token(tokenTyp, symbolString, startPos, endPos))
    }

    private val ZAHLEN_PATTERN = """(0|[1-9]\d?\d?(\.\d{3})+|[1-9]\d*)(\,\d+)?""".toRegex()

    private fun zahl(): Sequence<Token> = sequence {
        val startPos = currentTokenPos
        var zahlenString = ""
        var hinternKomma = false
        while (iterator.peek() != null) {
            val zeichen = iterator.peek()!!
            if (zeichen.isDigit()) {
                zahlenString += iterator.next()
            } else if (!hinternKomma && (zeichen == '.' || zeichen == ',') && iterator.peekDouble()?.isDigit() == true) {
                hinternKomma = zeichen == ','
                zahlenString += iterator.next()
                zahlenString += iterator.next()
            } else {
                break
            }
        }

        val endPos = currentTokenPos
        val zahl = zahlenString.replace(".", "").replace(',', '.').toDouble()
        val token = Token(TokenTyp.ZAHL(zahl), zahlenString, startPos, endPos)

        if (!zahlenString.matches(ZAHLEN_PATTERN)) {
            val fehlerToken = Token(TokenTyp.FEHLER, zahlenString, startPos, endPos)
            throw GermanScriptFehler.SyntaxFehler.LexerFehler(fehlerToken)
        }
        yield(token)
    }

    private fun zeichenfolge(): Sequence<Token> = sequence {
        val startPos = currentTokenPos
        iterator.next() // consume first '"'
        var zeichenfolge = ""
        while (iterator.peek() != null && iterator.peek() != '"') {
            zeichenfolge += iterator.next()
        }
        if (iterator.peek() != '"') {
            throw GermanScriptFehler.SyntaxFehler.LexerFehler(eofToken)
        }
        iterator.next()
        val endPos = currentTokenPos
        val token = Token(TokenTyp.ZEICHENFOLGE(zeichenfolge), '"' + zeichenfolge + '"', startPos, endPos)
        yield(token)
    }

    private val NOMEN_PATTERN = """[A-ZÖÄÜ][\wöäüß]*""".toRegex()
    private val VERB_PATTERN = """[a-zöäü][\wöäüß]*[\?!]?""".toRegex()

    private fun wort(): Sequence<Token> = sequence {
        val firstWordStartPos = currentTokenPos
        val erstesWort = teilWort()
        val firstWordEndPos = currentTokenPos
        var spaceBetweenWords = ""
        when {
            WORT_MAPPING.containsKey(erstesWort) -> when (erstesWort) {
                "größer", "kleiner" -> {
                    while (iterator.peek() == ' ') {
                        spaceBetweenWords += iterator.next()
                    }
                    val nächstesZeichen = iterator.peek()
                    val nächstesIstWort = !(SYMBOL_MAPPING.containsKey(nächstesZeichen) ||
                        nächstesZeichen == '&' ||
                        nächstesZeichen == '|' ||
                        nächstesZeichen == '"' ||
                        nächstesZeichen?.isDigit() == true)
                    if (nächstesIstWort) {
                        val nextWordStartPos = currentTokenPos
                        val nächstesWort = teilWort()
                        val nextWordEndPos = currentTokenPos
                        if (nächstesWort == "gleich") {
                            val tokenTyp = when (erstesWort) {
                                "größer" -> TokenTyp.OPERATOR(Operator.GRÖSSER_GLEICH)
                                "kleiner" -> TokenTyp.OPERATOR(Operator.KLEINER_GLEICH)
                                else -> throw Exception("Diser Fall wird nie ausgeführt")
                            }
                            yield(Token(tokenTyp, erstesWort + spaceBetweenWords + nächstesWort, firstWordStartPos, nextWordEndPos))
                        } else {
                            yield(Token(WORT_MAPPING.getValue(erstesWort), erstesWort, firstWordStartPos, firstWordEndPos))
                            val tokenTyp = (WORT_MAPPING.getOrElse(nächstesWort, {
                                when {
                                    NOMEN_PATTERN.matches(nächstesWort) -> TokenTyp.BEZEICHNER_GROSS(nächstesWort)
                                    VERB_PATTERN.matches(nächstesWort) -> TokenTyp.BEZEICHNER_KLEIN(nächstesWort)
                                    else -> throw GermanScriptFehler.SyntaxFehler.LexerFehler(
                                        Token(TokenTyp.FEHLER, nächstesWort, nextWordStartPos, nextWordEndPos)
                                    )
                                }
                            }))
                            yield(Token(tokenTyp, nächstesWort, nextWordStartPos, nextWordEndPos))
                        }
                    }
                    else {
                        yield(Token(WORT_MAPPING.getValue(erstesWort), erstesWort, firstWordStartPos, firstWordEndPos))
                    }
                }
                else -> yield(Token(WORT_MAPPING.getValue(erstesWort), erstesWort, firstWordStartPos, firstWordEndPos))
            }
            NOMEN_PATTERN.matches(erstesWort) -> yield(Token(TokenTyp.BEZEICHNER_GROSS(erstesWort), erstesWort, firstWordStartPos, firstWordEndPos))
            VERB_PATTERN.matches(erstesWort) -> yield(Token(TokenTyp.BEZEICHNER_KLEIN(erstesWort), erstesWort, firstWordStartPos, firstWordEndPos))
            else -> throw GermanScriptFehler.SyntaxFehler.LexerFehler(Token(TokenTyp.FEHLER, erstesWort, firstWordStartPos, firstWordEndPos))
        }
    }

    private fun teilWort(): String {
        var wort = iterator.next()!!.toString()
        while (iterator.peek() != null) {
            val nächstesZeiches = iterator.peek()!!
            if (nächstesZeiches == ' ' ||
                (nächstesZeiches != '!' && nächstesZeiches != '?' && SYMBOL_MAPPING.containsKey(nächstesZeiches))) {
                break
            }
            wort += iterator.next()!!
        }
        return wort
    }
}



fun main() {

    fun outputTokenTypes(code: String) {
        Lexer(code).tokeniziere().takeWhile { token -> token.typ != TokenTyp.EOF }.forEach { println(it) }
    }

        val quellcode = """// INTERNE FUNKTIONEN
    Verb schreibe die Zeichenfolge: intern. // print
    
    Verb schreibe die Zeichenfolge Zeile: intern. // println
    
    Verb schreibe die Zahl: intern.
    
    // VARIABLEN-DEKLARATIONEN, ZUWEISUNGEN UND ZAHLEN
    
    die Zahl ist 5 hoch 3
    schreibe die Zahl // 125
    
    Deklination Femininum Singular(Summe, Summe, Summe, Summe) Plural(Summen, Summen, Summen, Summen)
    
    eine Summe ist die Zahl + 25,5
    schreibe die Summe // 150,5
    eine Summe ist die Summe - 0,5
    schreibe die Summe // 150
    
    
    Deklination Femininum Singular(Welt, Welt, Welt, Welt) Plural(Welten, Welten, Welten, Welten)
    Deklination Maskulinum Singular(Mond, Mondes, Mond, Mond) Plural(Monde, Monde, Monden, Monde)
    
    // FUNKTIONS-DEFINTION
    Verb begrüße die Zeichenfolge Welt:
        schreibe "Hallo " + die Welt
    .
    
    begrüße "GermanScript" // Hallo GermanScript
    
    die Welt ist "Welt"
    begrüße die Welt // Hallo Welt
    
    der Mond ist "Mond"
    begrüße die Welt Mond // Hallo Mond"""


    outputTokenTypes(quellcode)
}