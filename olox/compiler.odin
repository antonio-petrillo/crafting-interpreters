package main

import "core:fmt"

// AT 17.4 parsing prefix expression

Parser :: struct {
    current:         Token,
    previous:        Token,
    had_error:       bool,
    panic_mode:      bool,
    scanner:         ^Scanner,
    compiling_chunk: ^Chunk,
}

compile :: proc(vm: ^VM, source: string, c: ^Chunk) -> bool {
    scanner := &Scanner{}
    init_scanner(scanner, source)

    parser := &Parser{}
    parser.scanner = scanner
    parser.compiling_chunk = c

    advance_compiler(parser)
    /* expression(parser) */
    consume_token(parser, TokenType.EOF, "Expected end of expression")

    end_compiler(parser)
    return !parser.had_error
}

advance_compiler :: proc(parser: ^Parser) {
    parser.previous = parser.current

    for {
        parser.current = scan_token(parser.scanner)

        if parser.current.type != TokenType.ERROR {
            break
        }

        error_at_current(parser, parser.current.source)
    }
}

error_at_current :: proc(parser: ^Parser, message: string) {
    error_at(parser, parser.current, message)
}

error :: proc(parser: ^Parser, message: string) {
    error_at(parser, parser.previous, message)
}

error_at :: proc(parser: ^Parser, token: Token, message: string) {
    if parser.panic_mode {
        return
    }
    parser.panic_mode = true
    fmt.eprintf("[line %d] Error ", token.line)

    if token.type == TokenType.EOF {
        fmt.eprintf("at end")
    } else if token.type == TokenType.ERROR {
        // Nothing.
    } else {
        fmt.eprintf("at '%s'", token.source)
    }

    fmt.eprintf(": %s\n", message)
    parser.had_error = true
}

consume_token :: proc(parser: ^Parser, token_type: TokenType, message: string) {
    if parser.current.type == token_type {
        advance_compiler(parser)
        return
    }

    error_at_current(parser, message)
}

emit_byte :: proc(parser: ^Parser, b: byte) {
    chunk := current_chunk(parser)
    write_chunk(chunk, b, parser.previous.line)
}

emit_return :: proc(parser: ^Parser) {
    emit_byte(parser, OpCode.OP_RETURN)
}

emit_bytes :: proc(parser: ^Parser, b1, b2: byte) {
    emit_byte(parser, b1)
    emit_byte(parser, b2)
}

end_compiler :: proc(parser: ^Parser) {
    emit_return(parser)
}

current_chunk :: proc(parser: ^Parser) -> ^Chunk {
    return parser.compiling_chunk
}
