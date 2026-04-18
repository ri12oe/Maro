# Maro Language

A simple, readble programming language written in Java, built as a learning project for CSCI-C212.

The design is based on Python, which is great for beginners to test Maro out! The name Maro was based on my name "Mario", just removed the letter "i".

## Compile & Run

'''bash

This command finds every Java file within your project's subdirectories and passes them all to the compiler at once to ensure they are built together. By using -d out, it automatically organizes the compiled .class files into a clean folder structure that reflects your package names. Once type, a new folder will appear called "out".

### javac -d out $(find src/main/java/maro -name "*.java")



If on a folder, then you need to type this out on the terminal

### java -cp out Maro examples/errorsShowcase.maro



If on a file / out of an folder , then you need to type this out on the terminal

### java -cp out Maro outOfFolder.maro



## REPL Mode

If you wanted to open REPL mode, first you need to check to see where the compiler is located at:

To check type this out on the terminal:

### find out -name "Maro.class"

If it says "out/Maro.class" , then run java -cp out Maro


If it says "out/maro/Maro.class" , then run java -cp out maro.Maro



## Project Structure

```
src/main/java/maro/
├── Maro.java                   Entry point + REPL
├── lexer/
│   ├── TokenType.java          All token types
│   ├── Token.java              Single token data class
│   └── Lexer.java              Source text → tokens
├── ast/
│   └── Node.java               AST node types
├── parser/
│   └── Parser.java             Tokens → AST (recursive descent)
└── interpreter/
    ├── Environment.java        Variable scopes
    ├── MaroFunction.java       User-defined function data
    └── Interpreter.java        Executes the AST
```

## Docunmentions
I have created a reference guide to my Maro program, in case if you are instereted in looking on how Maro works.


[View Document](Maro_Documention.pdf)
