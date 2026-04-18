
import lexer.Lexer;
import lexer.Token;
import ast.Node;
import parser.Parser;
import interpreter.Interpreter;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Maro {
    public static RunResult run (String sourceCode) {
        try {
            Lexer lexer = new Lexer(sourceCode);
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            Node.Program programTree = parser.parse();

            Interpreter interpreter = new Interpreter();
            interpreter.run(programTree);

            return new RunResult(interpreter.getOutput(), null);
        } catch (Exception e) {
            return new RunResult("", e.getMessage());
        }
    }

    public record RunResult(String output, String error) {
        public boolean hasError() {
            return error != null;
        }
    }

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            startREPL();
        } else {
            runFile(args[0]);
        }
    }

    private static void runFile(String filename) throws Exception {
        String sourceCode = Files.readString(Path.of(filename));

        RunResult result = run(sourceCode);

        if (result.hasError()) {
            System.err.println("Maro Error: " + result.error());
            System.exit(1);
        } else {
            System.out.print(result.output());
        }
    }

    private static void startREPL() {
        System.out.println("=================================");
        System.out.println(" Maro Language v1.0");
        System.out.println("=================================");

        Scanner keyboard = new Scanner(System.in);

        StringBuilder codeBuffer = new StringBuilder();

        while (true) {
            if (codeBuffer.isEmpty()) {
                System.out.print(">>> ");
            } else {
                System.out.print("... ");
            }

            String line = keyboard.nextLine();

            if (line.trim().equals("exit")) {
                System.out.println("Goodbye!");
                break;
            }

            codeBuffer.append(line).append("\n");

            if (!isCodeComplete(codeBuffer.toString())) {
                continue;
            }

            RunResult result = run(codeBuffer.toString());

            if (result.hasError()) {
                System.out.println("Error: " + result.error());
            } else if (!result.output().isEmpty()) {
                System.out.print(result.output());
            }

            codeBuffer.setLength(0);
        }
    }

    public static boolean isCodeComplete(String code) {
        int depth = 0;
        
        for (char c : code.toCharArray()) {
            if (c == '{') depth++;
            if (c == '}') depth--;
        }

        return depth == 0;
    }
}
