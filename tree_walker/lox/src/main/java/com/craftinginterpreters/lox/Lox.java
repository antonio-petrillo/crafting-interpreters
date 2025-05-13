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
    private boolean hadRuntimeError = false;
    private final String PROMPT;
    private final Interpreter interpreter;

    public Lox() {
        PROMPT = "JLOX :> ";
        this.interpreter = new Interpreter(this);
    }

    public Lox(String prompt) {
        PROMPT = prompt;
        this.interpreter = new Interpreter(this);
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
        if (hadError)
            System.exit(65);
        if (hadRuntimeError)
            System.exit(70);
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

    private void run(String source)  {
        Scanner scanner = new Scanner(this, source);
        List<Token> tokens = scanner.scanTokens();

        for (Token tok : tokens) {
            System.out.println(tok);
        }

        Parser parser = new Parser(this, tokens);
        Optional<Expr> expr = parser.parse();

        if (hadError || expr.isEmpty()) {
            System.err.printf("");
            return;
        }

        System.out.println(new AstPrinter().print(expr.get()));

        try {
            interpreter.interpret(expr.get());
        } catch (Expr.VisitException ve) {
            ve.printStackTrace();
            runtimeError(ve);
        }
    }

    public void error(int line, String message) {
        report(line, "", message);
    }

    public void error(Token token, String message) {
        if (token.type() == TokenType.EOF) {
            report(token.line(), " at end", message);
        } else {
            report(token.line(), String.format(" at '%s'", token.lexeme()), message);
        }
    }

    public void report(int line, String where, String message) {
        hadError = true;
        System.err.printf("[line %d] Error %s: %s\n", line, where, message);
    }

    private void runtimeError(Exception e) {
        System.err.println(e.getMessage());
        hadRuntimeError = true;
    }
}
