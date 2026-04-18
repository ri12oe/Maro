/*
File: Node.java
Version: 2.0
Date: 04/08/2026

Details:

After the lexer turns our code into tokens, the parser, turns those tokens into a tree structure called the AST.

Each node in the tre represents one piece of our program.
For example:
    let x = 1 + 2
    Becomes this tree.
I'm using Java's sealed interface feature here.
A sealed interface says: only these specific types can implement this interface.
This is useful because the compiler can then warn us if we forget to handle a case.

THe record keyword creates a simple data class automatically. Which generate,
constructor, getters, equals, hasCode, toString, which is perfect for AST nodes because nodes are just date containers.

Note: permits lists every allowed subtype. The Node.java file contains all the node types as nested records inside
the sealed interface.
*/


package ast;

import java.util.List;

public sealed interface Node permits

    Node.Program,
    Node.LetStmt,
    Node.PrintStmt,
    Node.ReturnStmt,
    Node.IfStmt,
    Node.WhileStmt,
    Node.ForStmt,
    Node.ExprStmt,
    Node.FunDecl,
    Node.Block,
    Node.NumberLit,
    Node.StringLit,
    Node.BoolLit,
    Node.NullLit,
    Node.ListLit,
    Node.MapLit,
    Node.Identifier,
    Node.BinaryOp,
    Node.UnaryOp,
    Node.Assign,
    Node.Call,
    Node.Index,
    Node.FieldAccess
{
    // statements nodes


    // the root of the whole program
    record Program(List<Node> body) implements Node {}

    // block of code surrounded by { }
    record Block(List<Node> body) implements Node {}

    // a varible delcaration
    record LetStmt(String name, Node value) implements Node {}

    // the prnt statments
    record PrintStmt(Node value) implements Node {}

    // a return statement
    record ReturnStmt(Node value) implements Node {}

    // a statement that just an expression
    record ExprStmt(Node expr) implements Node {}

    // an if statement 
    record IfStmt(Node condition, Node thenBranch, Node elseBranch) implements Node {}

    // while loop
    record WhileStmt(Node condition, Node body) implements Node {}

    // for-in loop
    record ForStmt(String var, Node iterable, Node body) implements Node {}

    // function declaration
    record FunDecl(String name, List<String> params, Node body) implements Node {}

    // Expressions Nodes
    
    // number
    record NumberLit(double value) implements Node {}

    //string
    record StringLit(String value) implements Node {}

    // boolean
    record BoolLit(boolean value) implements Node {}

    // null no value
    record NullLit() implements Node {}

    // list literal
    record ListLit(List<Node> elements) implements Node {}

    // a dictionary
    record MapLit(List<String> keys, List<Node> values) implements Node {}

    // a varible name
    record Identifier(String name) implements Node {}

    // math operation
    record BinaryOp(Node left, String op, Node right) implements Node {}

    // one sided operation
    record UnaryOp(String op, Node operand) implements Node {}

    // asssignemnt expression
    record Assign(String name, Node value) implements Node {}

    // function call
    record Call(Node callee, List<Node> args) implements Node {}

    // Index access
    record Index(Node target, Node key) implements Node {}

    // field/propery
    record FieldAccess(Node target, String field) implements Node {}
}

