package main

import "core:fmt"
import "core:strconv"

Parser :: struct {
    current: Token,
    previous: Token,
    scanner: ^Scanner,
    had_error: bool,
    panic_mode: bool,
    compiling: ^Chunk,
    vm: ^VM
}

Precedece :: enum u8 {
    NONE,
    ASSIGNMENT,
    OR,
    AND,
    EQUALITY,
    COMPARISON,
    TERM,
    FACTOR,
    UNARY,
    CALL,
    PRIMARY,
}

compile :: proc(vm: ^VM, source: string, c: ^Chunk) -> bool {
    scanner := &Scanner{}
    init_scanner(scanner, source)

    parser := &Parser{
        scanner = scanner,
        compiling: c,
        vm: vm
    }

    advance_scanner(scanner)
    expression()
    consume(parser, TokenType.EOF, "Expected end of expression")

    end_compiler(parser);

    return !parser.had_error
}

advance_compiler :: proc(vm: ^VM, parser: ^Parser) {
    parser.previous = parser.current

    for {
        parser.current = scan_token(parser.scanner)
        if parser.current.type != TokenType.ERROR {
            break
        }

        error_at(parser, parser.current)
    }
}

error_at :: proc(p: ^Parser, t: Token, message: string) {
    fmt.eprintf("[line %d] Error ", t.line)

    if t.type == TokenType.EOF {
        fmt.eprintf("at end.")
    } else if t.type == TokenType.ERROR {
        // nothing for now
    } else {
        fmt.eprintf("at '%s'", token.source)
    }

    fmt.eprintf(": %s\n", message)
    p.had_error = true
}

consume :: proc(p: ^Parser, t: TokenType, message: string) {
    if (p.current.type == t) {
        advance_scanner(p.scanner)
        return
    }

    error_at(p, message)
}

current_chunk :: proc(p: ^Parser) -> ^Chunk {
    return p.compiling
}

emit_byte :: proc(p: ^Parser, b: byte) {
    write_chunk(current_chunk(p), b, p.previous.line)
}

emit_bytes :: proc(p: ^Parser, b1, b2: byte) {
    emit_byte(p, b1)
    emit_byte(p, b2)
}


emit_return :: proc(p: ^Parser) {
    emit_byte(p.vm, p, byte(OpCode.OP_RETURN))
}

emit_constant :: proc(p: ^Parser, v: Value) {
    emit_bytes(p, byte(OpCode.OP_CONSTANT), make_constant(current_chunk(p), value))
}

make_constat :: proc(c: ^Chunk, v: Value) -> byte {
    constant := add_constant(c, v)
    if constant > 0xffff {
        error_at(p, p.previous, "Too many constants in one chunk.")
        return 0
    }

    return byte(constant)
}

grouping :: proc(p: ^Parser) {
    expression(p)
    consume(p, TokenType.RIGHT_PAREN, "Expect ')' after expression")
}

expression :: proc(p: ^Parser) {
    parse_precedence(.ASSIGNMENT)
}

number :: proc(p: ^Parser) {
    value, _ := strconv.parse_f64()
    emit_constant(p, value)
}

unary :: proc(p: ^Parser) {
    op_type := p.previous.type

    parse_precedence(.UNARY)

    #partial switch op_type {
    case .MINUS:
        emit_byte(p, byte(OpCode.OP_NEGATE))
    case:
        return
    }
}

end_compiler :: proc(p: ^Parser) {
    emit_return(p)
}

parse_precedence :: proc(precedence: Precedence) {

}
