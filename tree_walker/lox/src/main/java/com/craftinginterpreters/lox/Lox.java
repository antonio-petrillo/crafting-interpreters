package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class Lox {
    private boolean hadError = false;
    private final String PROMPT;

    public Lox() {
        PROMPT = "JLOX :> ";
    }

    public Lox(String prompt) {
        PROMPT = prompt;
    }

    public boolean hasErrored() {
        return hadError;
    }

    public void setErrored(boolean error) {
        hadError = error;
    }

    public void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) {
            System.exit(65);
        }
    }

    public void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while(true) {
            System.out.print(PROMPT);
            Optional<String> line = Optional.ofNullable(reader.readLine());

            if (line.isEmpty()) break;

            run(line.get());
            hadError = false;
        }
    }

    private void run(String source) {
        Scanner scanner = new Scanner(this, source);
        List<Token> tokens = scanner.scanTokens();

        for (Token tok : tokens) {
            System.out.println(tok);
        }
    }

    public void error(int line, String message) {
        report(line, "", message);
    }

    public void report(int line, String where, String message) {
        hadError = true;
        System.err.println(String.format("[line %d] Error %s: %s", line, where, message));
    }
}
