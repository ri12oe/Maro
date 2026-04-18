/*

File: Parser.java
Version: 2.0;
Date: 04/10/2026

Details:
This is the second step in running a Maro program.
The parser takes the flat list of tokens from the Lxer and turns them into a tree (AST).

I use a technique called recursive descent parsing.
The idea is write one method for each grammer rule, and those methods call each other recursively.

For example:
    parseStatement() mike call parseIfStmt()
    and so on...

The hardest part is Operator Precedence, making sur that:
2 + 3 * 4 evaulautes as 2 + (3*4) = 14 , not (2+3) * 4 = 20

*/

package parser;

import ast.Node;
import lexer.Token;
import lexer.TokenType;

import java.util.ArrayList;
import java.util.List;



public class Parser {
    // the lists of token we ot from Lexer
    private final List<Token> tokens;
    
    // postion in the token list
    private int pos = 0;

    //constructor
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // utility / helper methods make the code cleaner

    // look at the current token
    private Token current() {
        return tokens.get(pos);
    }

    // look ahead by some number of positions
    private Token peek(int offset) {
        return tokens.get(Math.min (pos + offset, tokens.size() - 1));
    }

    // check if the current token is of a specific type
    private boolean check(TokenType t) {
        return current().type == t;
    }

    // if the current token matches any of given types
    private boolean match(TokenType... types) {
        for (TokenType t: types) {
            if (check(t)) {
                pos++;
                return true;
            }
        }
        return false;
    }

    // must have a specific token type here
    private Token expect(TokenType t) {
        if(!check(t)) {
            throw new RuntimeException(
                "Line " + current().line + ": I expected " + t + " but got '" + current().value + "' instead"
            );
        }
        return tokens.get(pos++);
    }

    // skip any semi-colons encounter
    private void skipSemicolons() {
        while (check(TokenType.SEMICOLON)) pos++;
    }

    // entry point
    public Node.Program parse() {
        List<Node> statements = new ArrayList<>();

        skipSemicolons();

        while (!check(TokenType.EOF)) {
            statements.add(parseStatement());
            skipSemicolons();
        }
        return new Node.Program(statements);
    }

    // statement parsing
    private Node parseStatement() {
        return switch (current().type) {
            case LET  -> parseLetStmt();
            case FUN    -> parseFunDecl();
            case RETURN -> parseReturnStmt();
            case IF     -> parseIfStmt();
            case WHILE  -> parseWhileStmt();
            case FOR    -> parseForStmt();
            case PRINT  -> parsePrintStmt();
            case LBRACE -> parseBlock(); // a { ... } block
            default     -> parseExprStmt(); // anything else is an expression statement

        };
    }

    // Parse: let variablename = expression
    private Node.LetStmt parseLetStmt() {
        expect(TokenType.LET);
        String name = expect(TokenType.IDENTIFIER).value;
        expect(TokenType.ASSIGN);
        Node value = parseExpr();
        return new Node.LetStmt(name, value);
    }

    // parse: fun functionname
    private Node.FunDecl parseFunDecl() {
        expect(TokenType.FUN);
        String name = expect(TokenType.IDENTIFIER).value;
        expect(TokenType.LPAREN);

        // parse the paremeters list
        List<String> params = new ArrayList<>();
        
        if(!check(TokenType.RPAREN)) {
            params.add(expect(TokenType.IDENTIFIER).value);

            while(match(TokenType.COMMA)) {
                params.add(expect(TokenType.IDENTIFIER).value);
            }
        }
        expect( TokenType.RPAREN);
        Node body = parseBlock();
        return new Node.FunDecl(name, params, body);
    }

    // Parse: Return expression
    private Node.ReturnStmt parseReturnStmt() {
        expect(TokenType.RETURN);
        // check
        Node value;
        if (check(TokenType.SEMICOLON) || check(TokenType.RBRACE)) {
            value = new Node.NullLit();
        } else {
            value = parseExpr();
        }
        return new Node.ReturnStmt(value);
    }

    // parse; if conditions
    private Node.IfStmt parseIfStmt() {
        expect(TokenType.IF);
        expect(TokenType.LPAREN);
        Node condition = parseExpr();
        expect(TokenType.RPAREN);
        Node thenBranch = parseBlock();

        Node elseBranch = null;
        if (check(TokenType.ELSE)) {
            pos++;
            if(check(TokenType.IF)) {
                elseBranch = parseIfStmt();
            } else {
                elseBranch = parseBlock();
            }
        }
        return new Node.IfStmt(condition,thenBranch, elseBranch);
    }

    // parse: while condition
    private Node.WhileStmt parseWhileStmt() {
        
        expect(TokenType.WHILE);
        expect(TokenType.LPAREN);
        Node condition = parseExpr();
        expect(TokenType.RPAREN);
        Node body = parseBlock();

        return new Node.WhileStmt(condition, body);
    }

    // parse: for loop condition
    private Node.ForStmt parseForStmt() {
        expect(TokenType.FOR);
        expect(TokenType.LPAREN);
        String varname = expect(TokenType.IDENTIFIER).value;
        expect(TokenType.IN);
        Node iterable = parseExpr();
        expect(TokenType.RPAREN);
        Node body = parseBlock();

        return new Node.ForStmt(varname, iterable, body);
    }


    // parse: print statement
    private Node.PrintStmt parsePrintStmt() {
        expect(TokenType.PRINT);
        expect(TokenType.LPAREN);
        Node value = parseExpr();

        expect(TokenType.RPAREN);

        return new Node.PrintStmt(value);
    }

    // parser block statement
    private Node.Block parseBlock() {
        expect(TokenType.LBRACE);

        List<Node> statements = new ArrayList<>();

        skipSemicolons();

        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            statements.add(parseStatement());
            skipSemicolons();
        }
        expect(TokenType.RBRACE);
        return new Node.Block(statements);
    }

    // an expression used in statement
    private Node parseExprStmt() {
        Node expr = parseExpr();
        return new Node.ExprStmt(expr);
    }

    // Expr parsing
    private Node parseExpr() {
        return parseAssign();
    }

    // assignment 
    private Node parseAssign() {
        Node left = parseOr();

        if (check(TokenType.ASSIGN) && left instanceof Node.Identifier id ) {
            pos++;
            Node value = parseAssign();
            return new Node.Assign(id.name(), value);
        }
        return left;
    }

    // logical: OR
    private Node parseOr() {
        Node left = parseAnd();

        while (check(TokenType.OR)) {
            pos++;
            Node right = parseAnd();
            left = new Node.BinaryOp(left, "or", right);
        }

        return left;
    }

    // logical AND
    private Node parseAnd() {
        Node left = parseEquality();

        while(check(TokenType.AND)) {
            pos++;
            Node right = parseEquality();
            left = new Node.BinaryOp(left, "and", right);
        }

        return left;
    }

    // equality check
    private Node parseEquality() {
        Node left = parseComparison();

        while (check(TokenType.EQ) || check(TokenType.NEQ)) {
            String op = current().value;
            pos++;
            Node right = parseComparison();
            left = new Node.BinaryOp(left, op, right);
        }

        return left;
    }

    // comparsion
    private Node parseComparison() {
        Node left = parseAddSub();

        while (check(TokenType.LT) || check(TokenType.GT) ||
                 check(TokenType.LTE) || check(TokenType.GTE)) {

                String op = current().value;
                pos++;
                Node right = parseAddSub();
                left = new Node.BinaryOp(left, op, right);
                 } 

                 return left;
    }

    // Add and SUb
    private Node parseAddSub() {
        Node left = parseMultDiv();

        while(check(TokenType.PLUS) || check(TokenType.MINUS)) {
            String op = current().value;
            pos++;
            Node right = parseMultDiv();
            left = new Node.BinaryOp(left, op, right);
        }

        return left;
    }

    // Mult and DIv
    private Node parseMultDiv() {
        Node left = parseUnary();


        while (check(TokenType.STAR) || check(TokenType.SLASH) || check(TokenType.PERCENT)) {
            String op = current().value;
            pos++;
            Node right = parseUnary();

            left = new Node.BinaryOp(left, op, right);
        }

        return left;
    }

    // Unary
    private Node parseUnary() {
        if (check(TokenType.NOT) || check(TokenType.MINUS)) {
            String op = current().value;
            pos++;
            Node operand = parseUnary();
            return new Node.UnaryOp(op, operand);
        }

        return parsePostFix(); // no unary operator
    }

    private Node parsePostFix() {
        Node expr = parsePrimary();

        while(true) {
            if (check(TokenType.LPAREN)) {
                // its a function call. parse the arguments
                pos++;
                List<Node> args = new ArrayList<>();

                if (!check(TokenType.RPAREN)) {
                    args.add(parseExpr());

                    while (match(TokenType.COMMA)) {
                        args.add(parseExpr()); // more arguments
                    }
                }
                expect(TokenType.RPAREN);
                expr = new Node.Call(expr, args);
            } else if (check(TokenType.LBRACKET)) {
                // its index acess. like myList[0] or myMap["key"]
                pos++;
                Node key = parseExpr();
                expect(TokenType.RBRACKET);
                expr = new Node.Index(expr, key);
            } else if (check(TokenType.DOT)) {
                // it's field acess. like person.name
                pos++;
                String fieldname = expect(TokenType.IDENTIFIER).value;
                expr = new Node.FieldAccess(expr, fieldname);
            } else {
                break; // no more postfix ops, we're done
            }
        }
        return expr;
    }


    // Primary expression
    private Node parsePrimary() {
        Token t = current();

        return switch (t.type) {
            case NUMBER -> {
                pos++;
                // Double.parseDouble converts the string "42" to the number 42.0
                yield new Node.NumberLit(Double.parseDouble(t.value));
            }
            case STRING -> {
                pos++;
                yield new Node.StringLit(t.value);
            }
            case TRUE -> {
                pos++;
                yield new Node.BoolLit(true);
            }
            case FALSE -> {
                pos++;
                yield new Node.BoolLit(false);
            }

            case NULL -> {
                pos++;
                yield new Node.NullLit();
            }
            case IDENTIFIER -> {
                pos++;
                yield new Node.Identifier(t.value);
            }

            case LPAREN -> {
                // grouped expreesion like (2+1)
                // the parens just change precendce, they dont create a node
                pos++;
                Node inner = parseExpr();
                expect(TokenType.RPAREN);
                yield inner;
            }

            case LBRACKET -> {
                // list literal: [1,2, 3]
                pos++;
                List<Node> elements = new ArrayList<>();
                
                if (!check(TokenType.RBRACKET)) {
                    elements.add(parseExpr());
                    while (match(TokenType.COMMA)) {
                        // trailing commas re fine, check for ] before parsing
                        if (check(TokenType.RBRACKET)) break;
                        elements.add(parseExpr());
                    }
                }

                expect(TokenType.RBRACKET);
                yield new Node.ListLit(elements);
            }
            case LBRACE -> {
                // map lieteral
                // keys are identifiers, values are any expression
                pos++;
                List<String> keys = new ArrayList<>();
                List<Node> values = new ArrayList<>();

                if (!check(TokenType.RBRACE)) {
                    // parse first key-alue pair
                    keys.add(expect(TokenType.IDENTIFIER).value);
                    expect(TokenType.COLON);
                    values.add(parseExpr());
                    // parse remaining key-value pairs
                    while(match(TokenType.COMMA)) {

                        if(check(TokenType.RBRACE)) break;
                        keys.add(expect(TokenType.IDENTIFIER).value);
                        expect(TokenType.COLON);
                        values.add(parseExpr());
                    }
                }

                expect(TokenType.RBRACE);
                yield new Node.MapLit(keys, values);
            }
            // if we get here, we don't recongize the token as the start
            // of any expression - thats a syntax error
            default -> throw new RuntimeException( "Line " + t.line + ": I don't know what to do with '" + t.value + "'");
        };
    }
}

 