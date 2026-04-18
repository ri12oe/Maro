/*
File: Lexer.java
Version: 6.2
Date: 04/20/2026

Details:

This is the first step in running a Maro program. The lexer reads raw source code text and breaks it
into a list of tokens. 

Think of it like reading a sentence word by word, except our words can also be symbols like + - / * = {}

How it works:
    1. We keep a cursor (pos) that points to where we are in the source string
    2. Look at the current character
    3. Figure out what kind of token it starts
    4. Read as many characters as belong to that tokten
    5. Add the token to our list and repeat
*/

package lexer;

import java.util.*;

public class Lexer {
    // the entire source code as one big string
    private final String source;

    // our cursor - which character position we're currently at
    // starts at 0
    private int pos = 0;

    // track which line we're on a error messages are helpful
    private int line = 1;

    // this is where we'll collect all our tokens as we find them
    private final List<Token> tokens = new ArrayList<>();
    
    // a map that converts the keyboard strings to their tokentype
    // HashMap is like a dictornary, you look up a key and get a value
    // I made it static so it's shared across all lexer instances
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    // it runs once when the class is first loaded, to fill in keywords 
    static {
        KEYWORDS.put("let",    TokenType.LET);
        KEYWORDS.put("fun",    TokenType.FUN);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("if",     TokenType.IF);
        KEYWORDS.put("else",   TokenType.ELSE);
        KEYWORDS.put("while",  TokenType.WHILE);
        KEYWORDS.put("for",    TokenType.FOR);
        KEYWORDS.put("in",     TokenType.IN);
        KEYWORDS.put("print",  TokenType.PRINT);
        KEYWORDS.put("true",   TokenType.TRUE);
        KEYWORDS.put("false",  TokenType.FALSE);
        KEYWORDS.put("null",   TokenType.NULL);
        KEYWORDS.put("and",    TokenType.AND);
        KEYWORDS.put("or",     TokenType.OR);
        KEYWORDS.put("not",    TokenType.NOT);
    }
    // constructor , takes the source code string and saves it
    public Lexer (String source) {
        this.source = source;
    }
    // tokenize() the main method
    // call this to convert source code into a list of tokens
    // returns a list<Token> (a list of token objects)
    public List<Token> tokenize() {
        while (pos < source.length()) {
            // skip over any whitespace and comments
            skipWhitespaceAndComments();

            // check again, might have hit the end after skipping
            if (pos >= source.length()) break;

            // look at the current character
            char c = source.charAt(pos);

            // Handle newlines and track line numbers
            if (c == '\n') {
                line++;
                pos++;
                continue;
            }

            // figure out what kind of token this character starts
            if (Character.isDigit(c) || isNegativeNumber()) {
                // starts with a digit (or a minus before a digt) = number
                readNumber();
            } else if (c == '"') {
                // starts with a quote = string literal
                readString();
            } else if (Character.isLetter(c) || c == '_') {
                // starts with a letter or underscore = keyword or identifier
                readIdentifierOrKeyword();
            } else {
                // must be a symbol like + - , etc...
                readSymbol();
            }
        }
        // always end with an EOF token so the parser knows we're done
        tokens.add(new Token(TokenType.EOF, "" , line));
        return tokens;
    }
    // helper: check if looking at a negative number
    // like -5 at the start of an expression
    private boolean isNegativeNumber() {
        // current char must be a minus sign
        if (source.charAt(pos) != '-') return false;
        // next char must be a digit
        if (pos + 1 >= source.length()) return false;
        if (!Character.isDigit(source.charAt(pos + 1))) return false;

        // the previous token (if any) should be an operator or opening apren
        // meaning we're at the start of an expression, not doing subreaction
        if (tokens.isEmpty()) return true;
        
        TokenType last = tokens.get(tokens.size() - 1).type;
        // If the minus follows these tokens, it's a unary negative, not a subtraction
        return last == TokenType.ASSIGN ||
               last == TokenType.LPAREN ||
               last == TokenType.COMMA ||
               last == TokenType.RETURN ||
               last == TokenType.COLON ||
               last == TokenType.LBRACKET ||
               isOperator(last);
    }
    // helper: skip whitespace (spaces, tabs) and # comments
    // call this at the start of each loop iteration
    private boolean isOperator(TokenType type) {
        return type == TokenType.PLUS || type == TokenType.MINUS || 
               type == TokenType.STAR || type == TokenType.SLASH;
    }

    private void skipWhitespaceAndComments() {
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\r') {
                pos++;
            } else if (c == '#') {
                // a comment. In maro, # starts a comment until end of line and skip until hit a newline
                while (pos < source.length() && source.charAt(pos) != '\n') {
                    pos++;
                }
            } else {
                break;
            }
        }
    }
    // helper: read ad number token (int or decimal)
    private void readNumber() {
        StringBuilder sb = new StringBuilder();
        boolean hasDot = false;
        // if it starts with a minus, include that
        if (source.charAt(pos) == '-') sb.append(source.charAt(pos++));

        // keep reading digits and dots
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (Character.isDigit(c)) {
                sb.append(source.charAt(pos++));
            } else if (c == '.' && !hasDot) {
                // Ensure next char is a digit to avoid trailing dots or ".."
                if (pos + 1 < source.length() && Character.isDigit(source.charAt(pos + 1))) {
                    hasDot = true;
                    sb.append(source.charAt(pos++));
                } else {
                    break; 
                }
            } else {
                break;
            }
        }
        // add the completed number token
        tokens.add(new Token(TokenType.NUMBER, sb.toString(), line));
    }
    // helper: read a string literal and also handle ecape sequences like \n and \t
    private void readString() {
        pos++; // skip open quote
        StringBuilder sb = new StringBuilder();
        // keep reading until hit the losing quote
        while (pos < source.length() && source.charAt(pos) != '"') {
            char c = source.charAt(pos);
            if (c == '\\') {
                // escape sequence
                pos++;
                // Check bounds to prevent crash on trailing backslash
                if (pos >= source.length()) {
                    throw new RuntimeException("Unterminated string escape sequence at line " + line);
                }
                char escaped = source.charAt(pos++);
                // convert the escape seuqence to the actual charcter
                switch (escaped) {
                    case 'n' -> sb.append('\n'); // newline
                    case 't' -> sb.append('\t'); // tab
                    case '"' -> sb.append('"'); // literal quote
                    case '\\' -> sb.append('\\'); // literal backslash
                    default -> sb.append(escaped); 
                }
            } else {
                if (c == '\n') line++; // Handle multi-line strings
                sb.append(source.charAt(pos++));
            }
        }

        if (pos >= source.length()) {
            throw new RuntimeException("Unterminated string literal at line " + line);
        }
        pos++; // skip closing quote
        tokens.add(new Token(TokenType.STRING, sb.toString(), line));
    }
    // helper: read an identifier (varaible / fuction name) 
    private void readIdentifierOrKeyword() {
        StringBuilder sb = new StringBuilder();
        // keep reading letters, digits, and underscores
        // identifiers can have numbers in them, just not start with one
        while (pos < source.length() && (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_')) {
            sb.append(source.charAt(pos++));
        }
        String word = sb.toString();
        // check if this word is a keyword. getOrDeauflt means: look up word in keywords
        TokenType type = KEYWORDS.getOrDefault(word, TokenType.IDENTIFIER);
        tokens.add(new Token(type, word, line));
    }
    // helper: read a single charcater symbol
    private void readSymbol() {
        char c = source.charAt(pos);
        int currentline = line; // save line in case adavnce

        switch(c) {
            // simple single-character tokens
            case '+' -> { tokens.add(new Token(TokenType.PLUS, "+", currentline)); pos++; }
            case '*' -> { tokens.add(new Token(TokenType.STAR, "*", currentline)); pos++; }
            case '%' -> { tokens.add(new Token(TokenType.PERCENT, "%", currentline)); pos++; }
            case '(' -> { tokens.add(new Token(TokenType.LPAREN, "(", currentline)); pos++; }
            case ')' -> { tokens.add(new Token(TokenType.RPAREN, ")", currentline)); pos++; }
            case '{' -> { tokens.add(new Token(TokenType.LBRACE, "{", currentline)); pos++; }
            case '}' -> { tokens.add(new Token(TokenType.RBRACE, "}", currentline)); pos++; }
            case '[' -> { tokens.add(new Token(TokenType.LBRACKET, "[", currentline)); pos++; }
            case ']' -> { tokens.add(new Token(TokenType.RBRACKET, "]", currentline)); pos++; }
            case ',' -> { tokens.add(new Token(TokenType.COMMA, ",", currentline)); pos++; }
            case ';' -> { tokens.add(new Token(TokenType.SEMICOLON, ";", currentline)); pos++; }
            case ':' -> { tokens.add(new Token(TokenType.COLON, ":", currentline)); pos++; }
            case '.' -> { tokens.add(new Token(TokenType.DOT, ".", currentline)); pos++; }
            case '/' -> { tokens.add(new Token(TokenType.SLASH, "/", currentline)); pos++; }
            case '-' -> {
                if (pos + 1 < source.length() && source.charAt(pos + 1) == '>') {
                    tokens.add(new Token(TokenType.ARROW, "->", currentline));
                    pos += 2;
                } else {
                    tokens.add(new Token(TokenType.MINUS, "-", currentline));
                    pos++;
                }
            }
            // equal
            case '=' -> {
                if (pos + 1 < source.length() && source.charAt(pos + 1) == '=') {
                    tokens.add(new Token(TokenType.EQ, "==", currentline));
                    pos += 2;
                } else {
                    tokens.add(new Token(TokenType.ASSIGN, "=", currentline));
                    pos++;
                }
            }
            // not
            case '!' -> {
                if (pos + 1 < source.length() && source.charAt(pos + 1) == '=') {
                    tokens.add(new Token(TokenType.NEQ, "!=", currentline));
                    pos += 2;
                } else {
                    tokens.add(new Token(TokenType.NOT, "!", currentline));
                    pos++;
                }
            }
            // lesser than
            case '<' -> {
                if (pos + 1 < source.length() && source.charAt(pos + 1) == '=') {
                    tokens.add(new Token(TokenType.LTE, "<=", currentline));
                    pos += 2;
                } else {
                    tokens.add(new Token(TokenType.LT, "<", currentline));
                    pos++;
                }
            }
            //greater than
            case '>' -> {
                if (pos + 1 < source.length() && source.charAt(pos + 1) == '=') {
                    tokens.add(new Token(TokenType.GTE, ">=", currentline));
                    pos += 2;
                } else {
                    tokens.add(new Token(TokenType.GT, ">", currentline));
                    pos++;
                }
            }
            default -> { 
                System.err.println("Unexpected character '" + c + "' at line " + line);
                pos++; 
            }
        }
    }
}