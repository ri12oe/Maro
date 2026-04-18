
/*
File: Interpreter.java
Version: 4.0
Date: 04/20/2026


This is final and most important step. The interpreter executes maro program.

It does this by walking the AST tree that the Parser built. 
For every node it encounters, it does something:
    LetSmt - create a variable
    IfStmt - check the condition, run the right branch
    BinaryOp - do the math
    Call - call the function
    .. etc.

There are two main methods:
    execute() - for Statements
    evalute() - for expressions

Design notes I learned while building this:
    - use Object as the type for all MAro vlaues because
    Maro is dynamically typed (like python) - any variable can
    hold any type. Java's object can hold anything

    - numbers are stored as Java "double" (decimal numbers)
    - lists are Java ArrayList<Object>
    - Maps are Java LinkedHashMap<String, Object>
    - throw a speical "ReturnSignal" exception to handle return
    statments
*/


package interpreter;

import ast.Node;
import java.util.*;


public class Interpreter {
    
    // We collect it as a string so the caller can get the result
    private final StringBuilder output = new StringBuilder();

    // The global environment (outermost scope)
    // All built-in functions are defined here
    private Environment globalEnv;

    private static class ReturnSignal extends RuntimeException {
        final Object value; // the value being returned

        ReturnSignal(Object value) {
            super(null, null, true, false); // disable stack trace for performance
            this.value = value;
        }
    }

    // Called by Nova.java - get all the printed output as a string
    public String getOutput() {
        return output.toString();
    }

    // run() - entry point
    public void run(Node.Program program) {
        globalEnv = new Environment(null);

        registerBuiltins(globalEnv);

        for (Node statement : program.body()) {
            execute(statement, globalEnv);
        }
    }

    // built-functions
    @FunctionalInterface
    public interface BuiltinFunction {
        Object call(List<Object> args);
        
    }

    // register all built-in functions into the given environment
    private void registerBuiltins(Environment env) {

        // len(x)
        env.define("len", (BuiltinFunction) args -> {
            Object a = args.get(0);
            if (a instanceof String s) return (double) s.length();
            if (a instanceof List<?> l) return (double) l.size();
            if (a instanceof Map<?,?> m) return (double) m.size();
            throw new RuntimeException("len() needs a string, list, or map");
        });

        // str(x)
        env.define("str", (BuiltinFunction) args -> maroToString(args.get(0)));

        // num(x)
        env.define("num", (BuiltinFunction) args -> Double.parseDouble(maroToString(args.get(0))));

        // type(x)
        env.define("type", (BuiltinFunction) args -> {
            Object a = args.get(0);
            if (a == null) return "null";
            if (a instanceof Double)  return "number";
            if (a instanceof String) return "string";
            if (a instanceof Boolean) return "bool";
            if (a instanceof List) return "list";
            if (a instanceof Map) return "map";
            if ( a instanceof MaroFunction || a instanceof BuiltinFunction) return "function";
            return "unknown";
        });

        // range(n) or range(start, end)
        env.define("range", (BuiltinFunction) args -> {
            double start = args.size() == 1 ? 0 : (double) args.get(0);
            double end = args.size() == 1 ? (double) args.get(0) : (double) args.get(1); 
            // old version
            // double start = args.size() == 1 ? 0 : Double.parseDouble(maroToString(args.get(0)));
            // double end = Double.parseDouble(maroToString(args.get(1)));

            List<Object> list = new ArrayList<>();
            for (double i = start; i < end; i++) {
                list.add(i);
            }
            return list;
        });

        // push(list, item)
        env.define("push", (BuiltinFunction) args -> {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) args.get(0);
            list.add(args.get(1));
            return list;
        });

        // pop(list)
        env.define("pop", (BuiltinFunction) args -> {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) args.get(0);
            if (list.isEmpty()) return null;
            return list.remove(list.size()- 1);
        });

        // key(map)
        env.define("key", (BuiltinFunction) args -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) args.get(0);
            return new ArrayList<>(map.keySet());
        });

        // contatin(collection, item)
        env.define("contain", (BuiltinFunction) args -> {
            Object collection = args.get(0);
            Object item = args.get(1);
            if ( collection instanceof List<?> l) return l.contains(item);
            if (collection instanceof Map<?, ?> m) return m.containsKey(item);
            if (collection instanceof String s) return s.contains(maroToString(item));
            return false;
        });

        // split(string, separator)
        env.define("split", (BuiltinFunction) args -> {
            String str = (String) args.get(0);
            String separator = (String) args.get(1);
            List<Object> result = new ArrayList<>();
            for (String part : str.split(separator, -1)) {
                result.add(part);
            }
            return result;
        });


        // join(list, separator)
        env.define("join", (BuiltinFunction) args -> {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) args.get(0);
            String separator = (String) args.get(1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(separator);
                sb.append(maroToString(list.get(i)));
            }
            return sb.toString();
        });


        // String manipulation
        env.define("upper", (BuiltinFunction) args -> ((String) args.get(0)).toUpperCase());
        env.define("lower", (BuiltinFunction) args -> ((String) args.get(0)).toLowerCase());


        // Math functions
        env.define("abs", (BuiltinFunction) args -> Math.abs((double) args.get(0)));
        env.define("floor", (BuiltinFunction) args -> Math.floor((double) args.get(0)));
        env.define("ceil", (BuiltinFunction) args -> Math.ceil((double) args.get(0)));
        env.define("sqrt", (BuiltinFunction) args -> Math.sqrt((double) args.get(0)));
        env.define("pow", (BuiltinFunction) args -> Math.pow((double) args.get(0), (double) args.get(1)));
        env.define("max", (BuiltinFunction) args -> Math.max((double) args.get(0), (double) args.get(1)));
    }

    // Execute - handle statements
    private void execute(Node node, Environment env) {
        switch(node) {

            // evaulute the expression and define the variable
            case Node.LetStmt s -> {
                Object value  = evaluate(s.value(), env);
                env.define(s.name(), value);
            }

            // fun greet(name)
            case Node.FunDecl f -> {
                MaroFunction func = new MaroFunction(f.name() , f.params(), f.body(), env);
                env.define(f.name(), func);
            }

            // print(expression)
            case Node.PrintStmt p-> {
                String text = maroToString(evaluate(p.value(), env));
                output.append(text).append("\n");
            }

            // return expression
            case Node.ReturnStmt r -> {
                Object returnValue = evaluate(r.value(), env);
                throw new ReturnSignal(returnValue);
            }

             // if (condition) { then } else { els }
            // Check the condition and run the appropriate branch
            case Node.IfStmt s -> {
                Object condResult = evaluate(s.condition(), env);
                if (isTruthy(condResult)) {
                    execute(s.thenBranch(), env);
                } else if (s.elseBranch() != null) {
                    execute(s.elseBranch(), env);
                }
            }
            // while (condition)
            case Node.WhileStmt s -> {
                while (isTruthy(evaluate(s.condition(), env))) {
                    execute(s.body(), env);
                }
            }
            // for loop
            case Node.ForStmt s -> {
                Object iterableValue = evaluate(s.iterable(), env);
                List<?> list = toList(iterableValue);

                for (Object item : list) {
                    Environment loopEnv = new Environment(env);
                    loopEnv.define(s.var(), item);
                    execute(s.body(), loopEnv);
                }
            }

            // execute each statment in a inner scope
            case Node.Block b -> {
                Environment blockEnv = new Environment(env);
                for (Node statement : b.body()) {
                    execute(statement, blockEnv);
                }
            }

            case Node.ExprStmt e -> evaluate(e.expr(), env);

            default -> throw new RuntimeException("I don't know how to execute: " + node.getClass().getSimpleName());

        }

    }
    // Evalute - handle expressons
    @SuppressWarnings("unchecked")
    private Object evaluate(Node node, Environment env) {
        return switch (node) {

            // literal values
            case Node.NumberLit n -> n.value();  // ex. 42.0
            case Node.StringLit s -> s.value(); // "hello"
            case Node.BoolLit b -> b.value(); // true or false
            case Node.NullLit n -> null;

            // variable refernce - look it up in the environment
            case Node.Identifier i -> env.get(i.name());

            case Node.ListLit l -> {
                List<Object> list = new ArrayList<>();
                for (Node element : l.elements()) {
                    list.add(evaluate(element, env));
                }
                yield list;
            }

            case Node.MapLit m -> {
                Map<String, Object> map = new LinkedHashMap<>();
                for (int i = 0; i < m.keys().size(); i++) {
                    String key = m.keys().get(i);
                    Object value = evaluate(m.values().get(i), env);
                    map.put(key, value);
                }
                yield map;
            }

            case Node.Assign a -> {
                Object value = evaluate(a.value(), env);
                env.set(a.name(), value);
                yield value;
            }

            case Node.UnaryOp u -> evaluateUnary(u, env);

            case Node.BinaryOp b -> evaluateBinary(b, env);

            case Node.Call c -> evaluateCall(c, env);

            case Node.Index idx -> {
                Object target = evaluate(idx.target(), env);
                Object key = evaluate(idx.key(), env);

                if (target instanceof List<?> list) {
                    int i = (int)(double)(Double) key;
                    if (i <0) i = list.size() + i;

                    yield list.get(i);
                }

                if (target instanceof Map<?, ?> map) {
                    yield map.get(maroToString(key));
                }

                if (target instanceof String str) {
                    int i = (int)(double)(Double) key;
                    yield String.valueOf(str.charAt(i));
                }

                throw new RuntimeException("Cannot index into: " + maroToString(target));
            }
            case Node.FieldAccess f -> {
                Object target = evaluate(f.target(), env);
                yield getField(target,  f.field());
            }

            // default -> { evaluate(node, env); yield null;}
            default -> throw new RuntimeException("Unknown expression type: " + node.getClass().getSimpleName());

        };
    }

    // evaluteUnary()
    private Object evaluateUnary(Node.UnaryOp u, Environment env) {
        Object value = evaluate(u.operand(), env);

        return switch (u.op()) {
            case "-" -> -(double) value;
            case "not" , "!" -> !isTruthy(value);
            default -> throw new RuntimeException("Unkown operator: " + u.op());
        };
    }

    // evauluateBinary()
    private Object evaluateBinary(Node.BinaryOp b, Environment env) {
        if (b.op().equals("and")) {
            Object left = evaluate(b.left(), env);
            return isTruthy(left) ? evaluate(b.right(), env) : false;
        }

        if (b.op().equals("or")) {
            Object left = evaluate(b.left(), env);
            return isTruthy(left) ? true : evaluate(b.right(), env);
        }

        Object left = evaluate(b.left(), env);
        Object right = evaluate(b.right(), env);

        return switch (b.op()) {
            case "+" -> {
                if (left instanceof String || right instanceof String) {
                    yield  maroToString(left) + maroToString(right);
                }
                yield (double) left + (double) right;
            }
            case "-" -> (double) left - (double) right;

            case "*" -> {
                if (left instanceof String str && right instanceof Double n) {
                    yield str.repeat(n.intValue());
                }
                yield (double) left * (double) right;
            }

            case "/" -> {
                if ((double) right == 0) {
                    throw new RuntimeException("Cannot divided by zero!");
                }
                yield (double) left / (double) right;
            }

            case "%" -> (double) left % (double) right;

            case "==" -> maroEquals(left, right);
            case "!=" -> !maroEquals(left, right);

            case "<" -> (double) left < (double) right;
            case ">" -> (double) left > (double) right;
            case "<=" -> (double) left <= (double) right;
            case ">=" -> (double) left >= (double) right;

            default -> throw new RuntimeException("Unknown operator: " + b.op());
        };
    }

    // evaulauteCall()
    private Object evaluateCall(Node.Call c, Environment env) {
        Object callee = evaluate(c.callee(), env);

        List<Object> args = new ArrayList<>();
        for (Node arg : c.args()) {
            args.add(evaluate(arg, env));
        }

        if (callee instanceof BuiltinFunction fn) {
            return fn.call(args);
        }

        if (callee instanceof MaroFunction fn) {
            Environment funcEnv = new Environment(fn.closure);

            for (int i = 0; i < fn.params.size(); i++) {
                Object argValue = (i < args.size()) ? args.get(i) : null;
                funcEnv.define(fn.params.get(i), argValue);
            }

            try {
                execute(fn.body, funcEnv);
                return null;
            } catch (ReturnSignal r) {
                return r.value;
            }
        }
        throw new RuntimeException("Cannot call '" + maroToString(callee) + "' - it's not a function!");
    }

    // getField()
    @SuppressWarnings("unchecked")
    private Object getField(Object target, String field) {
        if (target instanceof Map<?,?> m) {
            return ((Map<String, Object>) m).get(field);
        }

        if (target instanceof String s) {
            return switch (field) {
                case "length" -> (double) s.length();
                case "upper" -> s.toUpperCase();
                case "lower" -> s.toLowerCase();
                case "trim" -> s.strip();

                default -> throw new RuntimeException("String don't have a field called '" + field + "'");
            };
        }

        if (target instanceof List<?> l) {
            return switch (field) {
                case "length" , "size" -> (double) l.size();
                default -> throw new RuntimeException("Lists don't have afield called '" + field + "'");
            };
        }
        throw new RuntimeException("Cannot access field '" + field + "' on " + maroToString(target));
    }

    // helper methods
    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Double d) return d != 0;
        if (value instanceof String s) return !s.isEmpty();
        if (value instanceof List<?> l) return !l.isEmpty();
        return true;
    }

    //maroEquals()
    private boolean maroEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    // toList()
    private List<?> toList(Object value) {
        if (value instanceof List<?> l) {
            return l;
        }

        if (value instanceof String s) {
            List<Object> chars = new ArrayList<>();
            for (char ch : s.toCharArray()) {
                chars.add(String.valueOf(ch));
            }
            return chars;
        }

        if (value instanceof Map<?,?> m) {
            return new ArrayList<>(m.keySet());
        }

        throw new RuntimeException("Cannot iterate over: " + maroToString(value) + " (only lists, strings, and map are iterable");
    }

    // maroToString()
    public String maroToString(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean b) return b.toString();

        if (value instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf(d.longValue());
            }
            return String.valueOf(d);
        }

        if (value instanceof List<?> l) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(maroToString(l.get(i)));
            }
            return sb.append("]").toString();
        }

        if (value instanceof Map<?,?> m) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?,?> entry : m.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey())
                    .append(": ")
                    .append(maroToString(entry.getValue()));
                first = false;
            }
            return sb.append("}").toString();
        }
        return value.toString();
    }
    
}
