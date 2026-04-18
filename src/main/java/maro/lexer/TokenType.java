package lexer;
/* 
File: TokenType.java
Version: 4.3
Date: 04/05/2026

Details:

A token is like a single piece your code. If you write: let = 5.
That gets broken into : [LET] [IDENTIFIER] [ASSIGN] [NUMBER]

This enum just lists every possbile type of token that Maro can have. Think it as
like a big category list. A enum in Java is just a special class where you list a fixed a set
of named constants. You can't add new ones at runtime, which is actually what we want.


*/

public enum TokenType {
    // literal values
    // these are values the user types in their code
    NUMBER, // intergers
    STRING, // ex. "hello world"
    BOOL, // true or false
    NULL, // no value


    // Identifeirs
    IDENTIFIER, // ex. x, myVariable, or age


    // Keywords
    LET, // used to declare a variale 
    FUN, // used to delcare a function
    RETURN, // used to return a value
    IF, // start of an if statmements
    ELSE, // the else branch
    WHILE, // a while loop
    FOR, // a for loop
    IN, // used in for loops ex. for (x in myList)
    PRINT, // built-in 
    TRUE, // the boolean value true
    FALSE, // the boolean value false

    // Math Operators
    PLUS, // +
    MINUS, // -
    STAR, // *
    SLASH, // /
    PERCENT, // %


    // Comparasion Opeators
    // these compare two values and give back true or false

    EQ, // ==
    NEQ, // !=
    LT, // <
    GT, // >
    LTE, // <=
    GTE, // >=


    // Logical Operators
    AND, // true and false
    OR, // true or false
    NOT, // not false


    // Assignement
    ASSIGN, // =

    // Brackts and Delimiters
    LPAREN, // ()
    RPAREN, // )
    LBRACE, // {
    RBRACE, // }
    LBRACKET, // [
    RBRACKET, // ]


    // Punctuation
    COMMA, // ,
    SEMICOLON, // ;
    COLON, // :
    DOT, // .
    ARROW, // ->

    // Extra tokens
    NEWLINE, // a line break
    EOF, // every token must end with this
}