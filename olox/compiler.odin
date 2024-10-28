package main

import "core:fmt"
import "core:strconv"
import "base:runtime"

Parser :: struct {
    current:         Token,
    previous:        Token,
    had_error:       bool,
    panic_mode:      bool,
    scanner:         ^Scanner,
    compiling_chunk: ^Chunk,
    vm: ^VM,
}

Precedence :: enum u8 {
    NONE      ,
    ASSIGNMENT, // =
    OR        , // or
    AND       , // and
    EQUALITY  , // == !=
    COMPARISON, // < > >= <=
    TERM      , // + -
    FACTOR    , // * /
    UNARY     , // ! -
    CALL      , // . ()
    PRIMARY   , //
}

ParseFn ::proc(parser: ^Parser)

ParseRule :: struct {
    prefix:     ParseFn,
    infix:      ParseFn,
    precedence: Precedence,
}

compile :: proc(source: string, c: ^Chunk, vm: ^VM) -> bool {
    scanner := &Scanner{}
    init_scanner(scanner, source)

    parser := &Parser{}
    parser.scanner = scanner
    parser.compiling_chunk = c
    parser.vm = vm

    advance_compiler(parser)
    expression(parser)
    consume_token(parser, TokenType.EOF, "Expected end of expression")

    end_compiler(parser)
    return !parser.had_error
}

expression :: proc(parser: ^Parser) {
    parse_precedence(parser, .ASSIGNMENT)
}

literal :: proc(parser: ^Parser) {
    #partial switch parser.previous.type {
        case .FALSE:
        emit_byte(parser, byte(OpCode.OP_FALSE))
        case .TRUE:
        emit_byte(parser, byte(OpCode.OP_TRUE))
        case .NIL:
        emit_byte(parser, byte(OpCode.OP_NIL))
        case:
        return
    }
}

// string is reserved keyword
// note that str_obj must be freed elsewhere
str :: proc(parser: ^Parser) {
    str_obj, alloc_err := new(ObjString)
    if alloc_err != runtime.Allocator_Error.None {
        error_at_current(parser, "Can't allocate constant, not enough memory")
        return
    }

    str_obj.str = parser.previous.source[1:len(parser.previous.source)-1] // note: string tokens are quoted
    str_obj.next = parser.vm.objects
    parser.vm.objects = str_obj
    emit_constant(parser, str_obj)
}

number :: proc(parser: ^Parser) {
    value, ok := strconv.parse_f64(parser.previous.source)
    if !ok {
        error_at_current(parser, "Input is not a valid number")
        return
    }

    emit_constant(parser, value)
}

grouping :: proc(parser: ^Parser) {
    expression(parser)
    consume_token(parser, TokenType.RIGHT_PAREN, "Expect ')' after expression.")
}

unary :: proc(parser: ^Parser) {
    operator_type := parser.previous.type

    parse_precedence(parser, .UNARY)

    #partial switch operator_type {
    case .MINUS:
        emit_byte(parser, byte(OpCode.OP_NEGATE))
    case .BANG:
        emit_byte(parser, byte(OpCode.OP_NOT))
    case:
        return
    }

}

make_constant :: proc(parser: ^Parser, value: Value) -> byte {
    constant := add_constant(current_chunk(parser), value)
    if constant > 0xFF {
        error(parser, "Too many constants in one chunk.")
        return 0
    }

    return constant
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
    write_chunk(chunk, b, uint(parser.previous.line))
}

emit_return :: proc(parser: ^Parser) {
    emit_byte(parser, byte(OpCode.OP_RETURN))
}

emit_bytes :: proc(parser: ^Parser, b1, b2: byte) {
    emit_byte(parser, b1)
    emit_byte(parser, b2)
}

emit_constant:: proc(parser: ^Parser, value: Value) {
    emit_bytes(parser, byte(OpCode.OP_CONSTANT), make_constant(parser, value))
}

end_compiler :: proc(parser: ^Parser) {
    emit_return(parser)

    when DEBUG_PRINT_CODE {
        if !parser.had_error {
            disassemble_chunk(current_chunk(parser), "code")
        } 
    }
}

current_chunk :: proc(parser: ^Parser) -> ^Chunk {
    return parser.compiling_chunk
}

parse_precedence :: proc(parser: ^Parser, precedence: Precedence) {
    advance_compiler(parser)
    prefix_rule := get_rule(parser.previous.type).prefix

    if prefix_rule == nil {
        error(parser, "Expect expression.")
        return
    }

    prefix_rule(parser)

    for precedence <= get_rule(parser.current.type).precedence {
        advance_compiler(parser)
        infix_rule := get_rule(parser.previous.type).infix
        infix_rule(parser)
    }
}

binary :: proc(parser: ^Parser) {
    operator_type := parser.previous.type
    rule := get_rule(operator_type)

    parse_precedence(parser, rule.precedence + Precedence(1))

    #partial switch operator_type {
    case .PLUS:
        emit_byte(parser, byte(OpCode.OP_ADD))
    case .MINUS:
        emit_byte(parser, byte(OpCode.OP_SUBTRACT))
    case .STAR:
        emit_byte(parser, byte(OpCode.OP_MULTIPLY))
    case .SLASH:
        emit_byte(parser, byte(OpCode.OP_DIVIDE))
    case .BANG_EQUAL:
        emit_bytes(parser, byte(OpCode.OP_EQUAL), byte(OpCode.OP_NOT))
    case .EQUAL_EQUAL:
        emit_byte(parser, byte(OpCode.OP_EQUAL))
    case .GREATER:
        emit_byte(parser, byte(OpCode.OP_GREATER))
    case .GREATER_EQUAL:
        emit_bytes(parser, byte(OpCode.OP_LESS), byte(OpCode.OP_NOT))
    case .LESS:
        emit_byte(parser, byte(OpCode.OP_LESS))
    case .LESS_EQUAL:
        emit_bytes(parser, byte(OpCode.OP_GREATER), byte(OpCode.OP_NOT))
    case:
        return
    }
}

rules := [TokenType]ParseRule{
        .LEFT_PAREN    = { prefix=grouping, infix=nil,    precedence=.NONE },
        .RIGHT_PAREN   = { prefix=nil,      infix=nil,    precedence=.NONE},
        .LEFT_BRACE    = { prefix=nil,      infix=nil,    precedence=.NONE},
        .RIGHT_BRACE   = { prefix=nil,      infix=nil,    precedence=.NONE},
        .COMMA         = { prefix=nil,      infix=nil,    precedence=.NONE},
        .DOT           = { prefix=nil,      infix=nil,    precedence=.NONE},
        .MINUS         = { prefix=unary,    infix=binary, precedence=.TERM},
        .PLUS          = { prefix=nil,      infix=binary, precedence=.TERM},
        .SEMICOLON     = { prefix=nil,      infix=nil,    precedence=.NONE},
        .SLASH         = { prefix=nil,      infix=binary, precedence=.FACTOR},
        .STAR          = { prefix=nil,      infix=binary, precedence=.FACTOR },
        .BANG          = { prefix=unary,    infix=nil,    precedence=.NONE },
        .BANG_EQUAL    = { prefix=nil,      infix=binary, precedence=.EQUALITY },
        .EQUAL         = { prefix=nil,      infix=binary, precedence=.NONE },
        .EQUAL_EQUAL   = { prefix=nil,      infix=binary, precedence=.EQUALITY },
        .GREATER       = { prefix=nil,      infix=binary, precedence=.COMPARISON },
        .GREATER_EQUAL = { prefix=nil,      infix=binary, precedence=.COMPARISON },
        .LESS          = { prefix=nil,      infix=binary, precedence=.COMPARISON },
        .LESS_EQUAL    = { prefix=nil,      infix=binary, precedence=.COMPARISON },
        .IDENTIFIER    = { prefix=nil,      infix=nil,     precedence=.NONE },
        .STRING        = { prefix=str,      infix=nil,    precedence=.NONE },
        .NUMBER        = { prefix=number,   infix=nil,    precedence=.NONE },
        .AND           = { prefix=nil,      infix=nil,    precedence=.NONE },
        .CLASS         = { prefix=nil,      infix=nil,    precedence=.NONE },
        .ELSE          = { prefix=nil,      infix=nil,    precedence=.NONE },
        .FALSE         = { prefix=literal,  infix=nil,    precedence=.NONE },
        .FOR           = { prefix=nil,      infix=nil,    precedence=.NONE },
        .FUN           = { prefix=nil,      infix=nil,    precedence=.NONE },
        .IF            = { prefix=nil,      infix=nil,    precedence=.NONE },
        .NIL           = { prefix=literal,  infix=nil,    precedence=.NONE },
        .OR            = { prefix=nil,      infix=nil,    precedence=.NONE },
        .PRINT         = { prefix=nil,      infix=nil,    precedence=.NONE },
        .RETURN        = { prefix=nil,      infix=nil,    precedence=.NONE },
        .SUPER         = { prefix=nil,      infix=nil,    precedence=.NONE },
        .THIS          = { prefix=nil,      infix=nil,    precedence=.NONE },
        .TRUE          = { prefix=literal,  infix=nil,    precedence=.NONE },
        .VAR           = { prefix=nil,      infix=nil,    precedence=.NONE },
        .WHILE         = { prefix=nil,      infix=nil,    precedence=.NONE },
        .ERROR         = { prefix=nil,      infix=nil,    precedence=.NONE },
        .EOF           = { prefix=nil,      infix=nil,    precedence=.NONE },
}

get_rule :: proc(operator_type: TokenType) -> ParseRule {
    return rules[operator_type]
}
