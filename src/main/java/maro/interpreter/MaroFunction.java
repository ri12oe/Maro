/*
File: MaroFunction.java
Date: 04/17/2026
Version: 1.5

Details:

When the user writes a function in Maro:

    fun greet(name) {
        return "Hello, " + name + "!"
    }
Need to store that function somewhere so we can call it later.
That's what this class is for:

A MaroFunction holds:
- the functions name
- the paramter names
- the body ast node
- the closure environment

The closure part is important because it leans the function remembers what variables existed
when it was crated. This is how closures work in Maro.


*/


package interpreter;

import ast.Node;
import java.util.List;

public class MaroFunction {
    // the name of the function
    public final String name;

    // the list of paramter names in order
    public final List<String> params;

    // the body of the function as an AST node
    public final Node body;

    // the environment where this function was defined
    // the interpreter will execute this when the function is called
    public final Environment closure;

    // constructor - just saves all four fields
    public MaroFunction(String name, List<String> params, Node body, Environment closure) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.closure = closure;
    }
    // toStirng() - what this looks this when printed
    // ex print(myFunction) would show <fun greet>
    @Override
    public String toString() {
        return "<fun " + name + ">";
    }
}
