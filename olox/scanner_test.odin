package main

import "core:fmt"
import "core:testing"

@(test)
test_scanner_token_single_or_two :: proc(t: ^testing.T) {

    source := "(){},.-+;/*!!====>>=<<="
    expecteds := [?]TokenType{.LEFT_PAREN, .RIGHT_PAREN, .LEFT_BRACE, .RIGHT_BRACE, .COMMA, .DOT, .MINUS, .PLUS, .SEMICOLON, .SLASH, .STAR, .BANG, .BANG_EQUAL, .EQUAL_EQUAL, .EQUAL, .GREATER, .GREATER_EQUAL, .LESS, .LESS_EQUAL}
    init_scanner(source)
    for expected in expecteds {
        tok := scan_token()
        testing.expect_value(t, tok.type, expected)
    }

    source_with_whitespaces := "( ) { } , . - + ; / * ! != == = > >= < <="
    init_scanner(source_with_whitespaces)
    for expected in expecteds {
        tok := scan_token()
        testing.expect_value(t, tok.type, expected)
    }

    source_keywords := "and class else false for fun if nil or print return super this true var while"
    expecteds_keywords := [?]TokenType{
            .AND, .CLASS, .ELSE, .FALSE,
            .FOR, .FUN, .IF, .NIL, .OR,
            .PRINT, .RETURN, .SUPER, .THIS,
            .TRUE, .VAR, .WHILE,
    }
    init_scanner(source_keywords)

    for expected in expecteds_keywords {
        tok := scan_token()
        testing.expect_value(t, tok.type, expected)
    }

    identifiers_or_literals := "12323 123.12341324 \"sdfasdfasdfad\" sucuzzone"
    expecteds_tok := [?]Token {
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
    init_scanner(identifiers_or_literals)
    for expected in expecteds_tok {
        tok := scan_token()
        testing.expect_value(t, tok.type, expected.type)
        testing.expect_value(t, tok.line, expected.line)
        testing.expect_value(t, tok.source, expected.source)
    }
}
