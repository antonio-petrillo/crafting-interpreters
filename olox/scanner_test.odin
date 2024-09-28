package main

import "core:fmt"
import "core:testing"

@(test)
test_scanner_token_single_or_two :: proc(t: ^testing.T) {
    s := &Scanner{}
    source := "(){},.-+;/*!!====>>=<<="
    expecteds := [?]TokenType{
            .LEFT_PAREN, .RIGHT_PAREN,
            .LEFT_BRACE, .RIGHT_BRACE,
            .COMMA, .DOT,
            .MINUS, .PLUS, .SEMICOLON,
            .SLASH, .STAR,

            .BANG, .BANG_EQUAL,
            .EQUAL_EQUAL, .EQUAL,
            .GREATER, .GREATER_EQUAL,
            .LESS, .LESS_EQUAL
    }
    init_scanner(s, source)
    for expected in expecteds {
        tok := scan_token(s)
        testing.expect_value(t, tok.type, expected)
    }
}

@(test)
test_scanner_token_single_or_two_with_spaces :: proc(t: ^testing.T) {
    s := &Scanner{}
    source := "( ) { } , . - + ; / * ! != == = > >= < <="
    expecteds := [?]TokenType{
            .LEFT_PAREN, .RIGHT_PAREN,
            .LEFT_BRACE, .RIGHT_BRACE,
            .COMMA, .DOT,
            .MINUS, .PLUS, .SEMICOLON,
            .SLASH, .STAR,

            .BANG, .BANG_EQUAL,
            .EQUAL_EQUAL, .EQUAL,
            .GREATER, .GREATER_EQUAL,
            .LESS, .LESS_EQUAL
    }
    init_scanner(s, source)
    for expected in expecteds {
        tok := scan_token(s)
        testing.expect_value(t, tok.type, expected)
    }
}

@(test)
test_scanner_keywords :: proc(t: ^testing.T) {
    s := &Scanner{}
    source := "and class else false for fun if nil or print return super this true var while"
    expecteds := [?]TokenType{
            .AND, .CLASS, .ELSE, .FALSE,
            .FOR, .FUN, .IF, .NIL, .OR,
            .PRINT, .RETURN, .SUPER, .THIS,
            .TRUE, .VAR, .WHILE,
    }
    init_scanner(s, source)

    for expected in expecteds {
        tok := scan_token(s)
        testing.expect_value(t, tok.type, expected)
    }
}

@(test)
test_scanner_identifiers :: proc(t: ^testing.T) {
    s := &Scanner{}
    source := "12323 123.12341324 \"sdfasdfasdfad\" sucuzzone"
    expecteds := [?]Token {
        Token{
            type = .NUMBER,
            line = 1,
            source = "12323"
        },
        Token{
            type = .NUMBER,
            line = 1,
            source = "123.12341324"
        },
        Token{
            type = .STRING,
            line = 1,
            source = "\"sdfasdfasdfad\"",
        },
        Token{
            type = .IDENTIFIER,
            line = 1,
            source = "sucuzzone",
        },
    }
    init_scanner(s, source)

    for expected in expecteds {
        tok := scan_token(s)
        testing.expect_value(t, tok.type, expected.type)
        testing.expect_value(t, tok.line, expected.line)
        testing.expect_value(t, tok.source, expected.source)
    }
}

@(test)
test_scanner_error_on_unterminated_string :: proc(t: ^testing.T) {
    s := &Scanner{}
    source := "\"this string is not closed properly "
    expected := Token{
        type = .ERROR,
        line = 1,
        source = "Unterminated string.",
    }
    init_scanner(s, source)

    tok := scan_token(s)
    testing.expect_value(t, tok.type, expected.type)
    testing.expect_value(t, tok.line, expected.line)
    testing.expect(t, tok.source == expected.source, "wrong error message")
}
