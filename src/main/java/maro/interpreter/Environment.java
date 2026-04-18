/*

File: Environment.java
Version: 1.5
Date: 04/11/2026

An envrionment is basically a scope, it holds all the variables that exist at a partiuclar point in the program.

The cool thing is that Environments form a chain.
Each environment has a parent (except the global one)

When look up a variable, first check the current environment. If it's not there, check the parent.
Then, the parent's parent, and so on up the chain

it also how closures work. when a function is created. it remebers its environment. thats way it can still access 
variables from the outer scope even later.
*/



package interpreter;

import java.util.HashMap;
import java.util.Map;


public class Environment {
    // the variales stored in this scope
    // can use object because values can be numbers, strings, lists, etc
    private final Map<String, Object> vars = new HashMap<>();

    // the parent scope
    // this is null for the global scope
    private final Environment parent;

    // constructor
    public Environment(Environment parent) {
        this.parent = parent;
    }

    // get() - look up varible
    // first checks this scope, then walks up the chain
    // throws an error if the variable doesn't exist anywehre
    public Object get(String name) {
        if (vars.containsKey(name)) {
            return vars.get(name);
        }

        if (parent != null) {
            return parent.get(name);
        }
        throw new RuntimeException("Undefined variable: '" + name + "'" );
    }

    // define() - create a new variable 
    // this is used for let x = ... declarations
    // it always creates in the current scope, never the parent
    public void define(String name, Object value) {
        vars.put(name, value);
    }


    // set() - update an existing variable
    // this is for assignemnt like x = newValue, when x was already declared somewhere above
    // it searches up the chan to find where the variable lives, then updates it their. this lets functions modify outer-scope variables
    public void set(String name, Object value) {
        if (vars.containsKey(name)) {
            vars.put(name, value);
            return;
        }

        if (parent != null) {
            parent.set(name, value);
            return;
        }
        throw new RuntimeException("Cannot assign to undefined variable: '" + name + "'");
    }

    // has() - check if variable exists
    // useful for things like "is this variable defined"
    public boolean has(String name) {
        return vars.containsKey(name) || (parent != null && parent.has(name));
    }
}
