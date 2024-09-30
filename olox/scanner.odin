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

init_scanner :: proc(s: ^Scanner, source: string) {
    s.start = 0
    s.current = 0
    s.line = 1
    s.source = source
    s.length = len(source)
}

skip_whitespace :: proc(s: ^Scanner) {
    for !is_at_end(s) {
        switch peek(s) {
        case ' ':
            advance_scanner(s)
        case '\t':
            advance_scanner(s)
        case '\n':
            scanner.line += 1
            advance_scanner(s)
        case '/':
            c, ok := peek_next(s)
            if ok && c == '/' {
                for peek(s) != '\n' && !is_at_end(s) {
                    advance_scanner(s)
                }
            } else {
                return
            }
        case:
            return
        }
    }
}

peek :: proc(s: ^Scanner) -> u8 {
   return s.source[s.current]
}

peek_next :: proc(s: ^Scanner) -> (u8, bool) {
    if is_at_end(s) {
        return '0', false
    }
    return s.source[s.current + 1], true
}

is_digit :: proc(c: u8) -> bool {
    return c >= '0' && c <= '9'
}

is_alpha :: proc(c: u8) -> bool {
    return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_'
}

make_token_identifier :: proc(s: ^Scanner) -> Token {
    for !is_at_end(s) && (is_alpha(peek(s)) || is_digit(peek(s))) {
        advance_scanner(s)
    }
    return make_token(s, identifier_type(s))
}

identifier_type :: proc(s: ^Scanner) -> TokenType {

    switch s.source[s.start:s.current] {
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

check_keyword :: proc(s: ^Scanner, length: int, rest: string, on_match: TokenType) -> TokenType {
    src := s.source[s.start:s.start + length]
    return src == rest ? on_match : .IDENTIFIER
}

scan_token :: proc(s: ^Scanner) -> Token {
    skip_whitespace(s)
    s.start = s.current
    if is_at_end(s) {
        return Token{
            line = s.line,
            type = .EOF,
        }
    }

    c := advance_scanner(s)
    if is_digit(c) {
        return make_token_number(s)
    }

    if is_alpha(c) {
        return make_token_identifier(s)
    }

    switch c {
    case '(':
        return make_token(s, .LEFT_PAREN)
    case ')':
        return make_token(s, .RIGHT_PAREN)
    case '{':
        return make_token(s, .LEFT_BRACE)
    case '}':
        return make_token(s, .RIGHT_BRACE)
    case ';':
        return make_token(s, .SEMICOLON)
    case ',':
        return make_token(s, .COMMA)
    case '.':
        return make_token(s, .DOT)
    case '-':
        return make_token(s, .MINUS)
    case '+':
        return make_token(s, .PLUS)
    case '/':
        return make_token(s, .SLASH)
    case '*':
        return make_token(s, .STAR)
    case '!':
        return make_token(s, match(s, '=') ? .BANG_EQUAL : .BANG)
    case '=':
        return make_token(s, match(s, '=') ? .EQUAL_EQUAL : .EQUAL)
    case '<':
        return make_token(s, match(s, '=') ? .LESS_EQUAL : .LESS)
    case '>':
        return make_token(s, match(s, '=') ? .GREATER_EQUAL : .GREATER)
    case '"': //" // workaraound bug in odin-mode
        return make_token_string(s)

    }

    return error_token(s, "Unexpected character.")
}

is_at_end :: proc(s: ^Scanner) -> bool {
    return s.length <= s.current
}

make_token :: proc(s: ^Scanner, t: TokenType) -> Token {
    return Token{
        type = t,
        source = s.source[s.start:s.current],
        line = s.line,
    }
}

error_token :: proc(s: ^Scanner, message: string) -> Token {
    return Token{
        type = .ERROR,
        source = message,
        line = s.line,
    }
}

make_token_string :: proc(s: ^Scanner) -> Token {
    for !is_at_end(s) && peek(s) != '"'{ //" // workaraound bug in odin-mode
        if peek(s) == '\n' {
            scanner.line += 1
        }
        advance_scanner(s)
    }
    if is_at_end(s) {
        return error_token(s, "Unterminated string.")
    }
    advance_scanner(s)
    return make_token(s, .STRING)
}

make_token_number :: proc(s: ^Scanner) -> Token {
    for is_digit(peek(s)) {
        advance_scanner(s)
    }
    if peek(s) == '.' {
        c, ok := peek_next(s)
        if ok && is_digit(c) {
            advance_scanner(s)
            for is_digit(peek(s)) {
                advance_scanner(s)
            }
        }
    }
    return make_token(s, .NUMBER)
}

advance_scanner :: proc(s: ^Scanner) -> u8 {
    s.current += 1;
    return s.source[s.current - 1]
}

match :: proc(s: ^Scanner, expected: u8) -> bool {
    if is_at_end(s) {
        return false
    }
    if s.source[s.current] != expected {
        return false
    }
    s.current += 1
    return true
}
