package com.craftinginterpreters.lox;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: jlox [script]");
            System.exit(64);
        }

        Lox l = new Lox();

        try {
            if (args.length == 1) {
               l.runFile(args[0]);
            } else {
               l.runPrompt();
            }
        } catch (IOException io) {
           io.printStackTrace();
        }
    }
}
