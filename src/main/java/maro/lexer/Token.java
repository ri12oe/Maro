/*
File: Token.java
Version: 1.0
Date: 04/01/2026

Detail:

A Token is just a small chunk of the source code that we've already indentified and label.
For example, if the user writes: let age = 21
We get these 4 tokens:
    Token(LET, "let", line=1)
    Token(IDENTIFIER, "age", line=1)
    Token(ASSIGN, "=" , line=1)
    Token(NUMBER, "21", line=1)
I'm using a simple class here with 3 fields. In java, "final" means the field can't be changed
after the object is created, which make sense for tokens because we never want to modify them.
 */


package lexer;

public class Token {
    // the type of this token
    // final means that the field can't be changed once the object is created
    public final TokenType type;

    // the text from the source code
    // ex. for NUMBER token, value might be 100
    public final String value;

    // which line of source code this token is on
    // store this so we can show helpful error meesages
    public final int line;

    // constructor, just set all three fields
    public Token(TokenType type, String value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }

    // toString() is called whenever Java needs to convert object to string
    // I override it to show something readable for debugging
    @Override
    public String toString() {
        return String.format("Token(%s, '%s', line=%d)", type, value, line);
    }
}
