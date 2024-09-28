package main

Scanner :: struct {
    start: int,
    current: int,
    line: int,
    source: string,
    length: int,
}

Token :: struct {
    type: TokenType,
    source: string,
    line: int,
}

TokenType :: enum {
    // SINGLE CHARACTERS TOKENS
    LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS,
    SEMICOLON, SLASH, STAR,

    // ONE OR TWO CHARACTERS TOKENS
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // LITERALS
    IDENTIFIER, STRING, NUMBER,

    // KEYWORDS
    AND, CLASS, ELSE, FALSE,
    FOR, FUN, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS,
    TRUE, VAR, WHILE,

    ERROR, EOF
}

scanner := Scanner{}

init_scanner :: proc(source: string) {
    scanner.start = 0
    scanner.current = 0
    scanner.line = 1
    scanner.source = source
    scanner.length = len(source)
}

skip_whitespace :: proc() {
    for {
        switch peek() {
        case ' ':
            advance()
        case '\t':
            advance()
        case '\n':
            scanner.line += 1
            advance()
        case '/':
            c, ok := peek_next()
            if ok && c == '/' {
                for peek() != '\n' && !is_at_end() {
                    advance()
                }
            } else {
                return
            }
        case:
            return
        }
    }
}

peek :: proc() -> u8 {
   return scanner.source[scanner.current]
}

peek_next :: proc() -> (u8, bool) {
    if is_at_end() {
        return '0', false
    }
    return scanner.source[scanner.current + 1], true
}

is_digit :: proc(c: u8) -> bool {
    return c >= '0' && c <= '9'
}

is_alpha :: proc(c: u8) -> bool {
    return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_'
}

make_token_identifier :: proc() -> Token {
    for !is_at_end() && (is_alpha(peek()) || is_digit(peek())) {
        advance()
    }
    return make_token(identifier_type())
}

identifier_type :: proc() -> TokenType {

    switch scanner.source[scanner.start:scanner.current] {
    case "and":
        return .AND
    case "class":
        return .CLASS
    case "else":
        return .ELSE
    case "if":
        return .IF
    case "nil":
        return .NIL
    case "or":
        return .OR
    case "print":
        return .PRINT
    case "return":
        return .RETURN
    case "super":
        return .SUPER
    case "var":
        return .VAR
    case "while":
        return .WHILE
    case "for":
        return .FOR
    case "false":
        return .FALSE
    case "true":
        return .TRUE
    case "fun":
        return .FUN
    case "this":
        return .THIS

    case:
        return .IDENTIFIER
    }

}

check_keyword :: proc(length: int, rest: string, on_match: TokenType) -> TokenType {
    if string(scanner.source[scanner.start:scanner.start + length]) == rest {
        return on_match
    }
    return .IDENTIFIER
}

scan_token :: proc() -> Token {
    skip_whitespace()
    scanner.start = scanner.current
    if is_at_end() {
        return make_token(.EOF)
    }

    c := advance()
    if is_digit(c) {
        return make_token_number()
    }

    if is_alpha(c) {
        return make_token_identifier()
    }

    switch c {
    case '(':
        return make_token(.LEFT_PAREN)
    case ')':
        return make_token(.RIGHT_PAREN)
    case '{':
        return make_token(.LEFT_BRACE)
    case '}':
        return make_token(.RIGHT_BRACE)
    case ';':
        return make_token(.SEMICOLON)
    case ',':
        return make_token(.COMMA)
    case '.':
        return make_token(.DOT)
    case '-':
        return make_token(.MINUS)
    case '+':
        return make_token(.PLUS)
    case '/':
        return make_token(.SLASH)
    case '*':
        return make_token(.STAR)
    case '!':
        return make_token(match('=') ? .BANG_EQUAL : .BANG)
    case '=':
        return make_token(match('=') ? .EQUAL_EQUAL : .EQUAL)
    case '<':
        return make_token(match('=') ? .LESS_EQUAL : .LESS)
    case '>':
        return make_token(match('=') ? .GREATER_EQUAL : .GREATER)
    case '"':
        return make_token_string()


    }

    return error_token("Unexpected character.")
}

is_at_end :: proc() -> bool {
    return scanner.length <= scanner.current
}

make_token :: proc(t: TokenType) -> Token {
    return Token{
        type = t,
        source = scanner.source[scanner.start:scanner.current],
        line = scanner.line,
    }
}

error_token :: proc(message: string) -> Token {
    return Token{
        type = .ERROR,
        source = message,
        line = scanner.line,
    }
}

make_token_string :: proc() -> Token {
    for peek() != '"' && !is_at_end() {
        if peek() == '\n' {
            scanner.line += 1
        }
        advance()
    }
    if is_at_end() {
        return error_token("Unterminated string.")
    }
    advance()
    return make_token(.STRING)
}

make_token_number :: proc () -> Token {
    for is_digit(peek()) {
        advance()
    }
    if peek() == '.' {
        c, ok := peek_next()
        if ok && is_digit(c) {
            advance()
            for is_digit(peek()) {
                advance()
            }
        }
    }
    return make_token(.NUMBER)
}

advance :: proc() -> u8 {
    scanner.current += 1;
    return scanner.source[scanner.current - 1]
}

match :: proc(expected: u8) -> bool {
    if is_at_end() {
        return false
    }
    if scanner.source[scanner.current] != expected {
        return false
    }
    scanner.current += 1
    return true
}
