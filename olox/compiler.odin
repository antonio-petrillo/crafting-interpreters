package main

import "core:fmt"
import "core:strconv"
import "base:runtime"
import "core:strings"

Parser :: struct {
    current:         Token,
    previous:        Token,
    had_error:       bool,
    panic_mode:      bool,
    scanner:         ^Scanner,
    compiling_chunk: ^Chunk,
    vm:              ^VM,
    compiler:        ^Compiler,
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

ParseFn ::proc(parser: ^Parser, can_assign: bool)

ParseRule :: struct {
    prefix:     ParseFn,
    infix:      ParseFn,
    precedence: Precedence,
}

UINT8_COUNT :: 256
UINT16_COUNT :: 0xFFFFFFFF

Compiler :: struct {
    localCount: int,
    scopeDepth: int,
    locals: [UINT8_COUNT]Local,
}

Local :: struct {
    name: Token,
    depth: int,
}

init_compiler :: proc(c: ^Compiler) {
    c.localCount = 0
    c.scopeDepth  = 0
}

compile :: proc(source: string, c: ^Chunk, vm: ^VM) -> bool {
    scanner := &Scanner{}
    init_scanner(scanner, source)
    compiler := &Compiler{}
    init_compiler(compiler)

    parser := &Parser{}
    parser.scanner = scanner
    parser.compiling_chunk = c
    parser.vm = vm
    parser.compiler = compiler

    advance_compiler(parser)

    for !match_token(parser, TokenType.EOF) {
        declaration(parser)
    }

    end_compiler(parser)
    return !parser.had_error
}

match_token :: proc(parser: ^Parser, token_type: TokenType) -> bool {
    if !check(parser, token_type) {
       return false
    }
    advance_compiler(parser)
    return true
}

check :: proc(parser: ^Parser, token_type: TokenType) -> bool {
    return parser.current.type == token_type
}

declaration :: proc(parser: ^Parser) {
    if match_token(parser, TokenType.VAR) {
        var_declaration(parser)
    } else {
        statement(parser)
    }

    if parser.panic_mode {
        synchronize(parser)
    }
}

synchronize :: proc(parser: ^Parser) {
    parser.panic_mode = false
    for parser.current.type != TokenType.EOF {
        if parser.previous.type != TokenType.SEMICOLON {
           return
        }
        #partial switch parser.current.type {
        case .CLASS:
            return
        case .FUN:
            return
        case .VAR:
            return
        case .FOR:
            return
        case .IF:
            return
        case .WHILE:
            return
        case .RETURN:
            return
        case .PRINT:
            return

        case:
            return
        }
        advance_compiler(parser)
    }
}

var_declaration :: proc(parser: ^Parser) {
    global := parse_variable(parser, "Expect variable name.")

    if match_token(parser, TokenType.EQUAL) {
        expression(parser)
    } else {
        emit_byte(parser, byte(OpCode.OP_NIL))
    }

    consume_token(parser, TokenType.SEMICOLON, "Expect ';' after variable declaration.")

    define_variable(parser, global)
}

parse_variable :: proc(parser: ^Parser, error_msg: string) -> byte {
    consume_token(parser, TokenType.IDENTIFIER, error_msg)

    declare_variable(parser)

    if parser.compiler.scopeDepth > 0 {
        return 0
    }

    return identifier_constant(parser, parser.previous)
}

declare_variable :: proc(parser: ^Parser) {
    if parser.compiler.scopeDepth == 0 {
        return
    }

    name := &parser.previous

    for i := parser.compiler.localCount - 1; i >= 0; i -= 1 {
        local := &parser.compiler.locals[i]
        if local.depth != -1 && local.depth < parser.compiler.scopeDepth {
            break
        }

        if identifiers_equals(name, &local.name) {
            error(parser, "Already a variable with this name in this scope.")
        }
    }

    add_local(parser, name^)
}

identifiers_equals :: proc(a, b: ^Token) -> bool {
    return a.source == b.source
}

// TODO: wtf? why the book pass around Token like this
// this may not work
add_local :: proc(parser: ^Parser, name: Token) {
    if parser.compiler.localCount == UINT8_COUNT {
        error(parser, "Too many local variables in function.")
        return
    }
    /* parser.compiler.locals[parser.compiler.localCount].name.line = name.line */
    /* parser.compiler.locals[parser.compiler.localCount].name.source = strings.clone(name.source) */
    /* parser.compiler.locals[parser.compiler.localCount].name.type = name.type */
    parser.compiler.locals[parser.compiler.localCount].name = name
    parser.compiler.locals[parser.compiler.localCount].depth = -1
    parser.compiler.localCount += 1
}

identifier_constant :: proc(parser: ^Parser, token: Token) -> byte {
    str_obj, alloc_err := allocate_string(token.source, parser.vm)
    if alloc_err != runtime.Allocator_Error.None {
        error_at_current(parser, "Can't allocate constant, not enough memory")
        return 0
    }
    return make_constant(parser, str_obj)
}

and_ :: proc(parser: ^Parser, can_assign: bool) {
    end_jump := emit_jump(parser, byte(OpCode.OP_JUMP_IF_FALSE))
    emit_byte(parser, byte(OpCode.OP_POP))
    parse_precedence(parser, .AND)
    patch_jump(parser, end_jump)
}

or_ :: proc(parser: ^Parser, can_assign: bool) {
    else_jump := emit_jump(parser, byte(OpCode.OP_JUMP_IF_FALSE))
    end_jump := emit_jump(parser, byte(OpCode.OP_JUMP))

    patch_jump(parser, else_jump)
    emit_byte(parser, byte(OpCode.OP_POP))
    parse_precedence(parser, .OR)
    patch_jump(parser, end_jump)
}

mark_initialized :: proc(current: ^Compiler) {
    current.locals[current.localCount - 1].depth = current.scopeDepth
}

define_variable :: proc(parser: ^Parser, global: byte) {
    if parser.compiler.scopeDepth > 0 {
        mark_initialized(parser.compiler)
        return
    }

    emit_bytes(parser, byte(OpCode.OP_DEFINE_GLOBAL), global)
}

statement :: proc(parser: ^Parser) {
    if match_token(parser, TokenType.PRINT) {
        print_statement(parser)
    } else if match_token(parser, TokenType.WHILE) {
        while_statement(parser)
    } else if match_token(parser, TokenType.FOR) {
        for_statement(parser)
    } else if match_token(parser, TokenType.IF) {
        if_statement(parser)
    } else if match_token(parser, TokenType.LEFT_BRACE) {
        begin_scope(parser)
        block(parser)
        end_scope(parser)
    } else {
        expression_statement(parser)
    }
}

// this is a little too shite
for_statement :: proc(parser: ^Parser) {
    begin_scope(parser)
    consume_token(parser, .LEFT_PAREN, "Expect '(' after for.")
    if match_token(parser, .SEMICOLON) {

    } else if match_token(parser, .VAR) {
        var_declaration(parser)
    } else {
        expression_statement(parser)
    }
    loop_start := len(current_chunk(parser).code)
    exit_jump := -1
    if !match_token(parser, .SEMICOLON) {
        expression(parser)
        consume_token(parser, .SEMICOLON, "Expect ';' after loop condition.")
        exit_jump = emit_jump(parser, byte(OpCode.OP_JUMP_IF_FALSE))
        emit_byte(parser, byte(OpCode.OP_POP))
    }
    if !match_token(parser, .RIGHT_PAREN) {
        body_jump := emit_jump(parser, byte(OpCode.OP_JUMP))
        increment_start := len(current_chunk(parser).code)
        expression(parser)
        emit_byte(parser, byte(OpCode.OP_POP))
        consume_token(parser, .RIGHT_PAREN, "Expect ')' after for clauses.")
        emit_loop(parser, loop_start)
        loop_start = increment_start
        patch_jump(parser, body_jump)
    }

    statement(parser)
    emit_loop(parser, loop_start)

    if exit_jump != -1 {
        patch_jump(parser, exit_jump)
        emit_byte(parser, byte(OpCode.OP_POP))
    }

    end_scope(parser)
}

while_statement :: proc(parser: ^Parser) {
    loop_start := len(current_chunk(parser).code)
    consume_token(parser, .LEFT_PAREN, "Expect '(' after while.")
    expression(parser)
    consume_token(parser, .RIGHT_PAREN, "Expect ')' after while condition.")

    exit_jump := emit_jump(parser, byte(OpCode.OP_JUMP_IF_FALSE))
    emit_byte(parser, byte(OpCode.OP_POP))

    statement(parser)
    emit_loop(parser, loop_start)
    patch_jump(parser, exit_jump)
    emit_byte(parser, byte(OpCode.OP_POP))
}

emit_loop :: proc(parser: ^Parser, loop_start: int) {
    emit_byte(parser, byte(OpCode.OP_LOOP))

    offset := len(current_chunk(parser).code) - loop_start + 2
    if offset > UINT16_COUNT {
        error(parser, "Loop body too large.")
    }

    emit_byte(parser, byte((offset >> 8) & 0xFF))
    emit_byte(parser, byte(offset & 0xFF))
}

if_statement :: proc(parser: ^Parser) {
    consume_token(parser, TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
    expression(parser)
    consume_token(parser, TokenType.RIGHT_PAREN, "Expect ')' after 'if'.")

    then_jump := emit_jump(parser, byte(OpCode.OP_JUMP_IF_FALSE))
    emit_byte(parser, byte(OpCode.OP_POP))

    statement(parser)

    else_jump := emit_jump(parser, byte(OpCode.OP_JUMP))

    patch_jump(parser, then_jump)
    emit_byte(parser, byte(OpCode.OP_POP))

    if match_token(parser, TokenType.ELSE) {
        statement(parser)
    }
    patch_jump(parser, else_jump)
}

emit_jump :: proc(parser: ^Parser, instruction: byte) -> int {
    emit_byte(parser, instruction)
    emit_byte(parser, 0xFF)
    emit_byte(parser, 0xFF)
    return  len(current_chunk(parser).code) - 2
}

patch_jump :: proc(parser: ^Parser, offset: int) {
    jump := len(current_chunk(parser).code) - offset - 2

    if jump > UINT16_COUNT {
        error(parser, "Too much code to jump over.")
    }

    current_chunk(parser).code[offset] = byte(jump >> 8 & 0xFF)
    current_chunk(parser).code[offset + 1] = byte(jump & 0xFF)
}

begin_scope :: proc(parser: ^Parser) {
    parser.compiler.scopeDepth += 1
}

end_scope :: proc(parser: ^Parser) {
    parser.compiler.scopeDepth -= 1

    current := parser.compiler
    for current.localCount > 0 && current.locals[current.localCount - 1].depth > current.scopeDepth {
        emit_byte(parser, byte(OpCode.OP_POP))
        current.localCount -= 1
    }
}

block :: proc(parser: ^Parser) {
    for !check(parser, TokenType.RIGHT_BRACE) && !check(parser, TokenType.EOF){
        declaration(parser)
    }
    consume_token(parser, TokenType.RIGHT_BRACE, "Expect '}' after block")
}

expression_statement :: proc(parser: ^Parser) {
    expression(parser)
    consume_token(parser, TokenType.SEMICOLON, "Expected ';' after expression.")
    emit_byte(parser, byte(OpCode.OP_POP))
}
 
print_statement :: proc(parser: ^Parser) {
    expression(parser)
    consume_token(parser, TokenType.SEMICOLON, "Expected ';' after value.")
    emit_byte(parser, byte(OpCode.OP_PRINT))
}

expression :: proc(parser: ^Parser) {
    parse_precedence(parser, .ASSIGNMENT)
}

literal :: proc(parser: ^Parser, can_assign: bool) {
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
str :: proc(parser: ^Parser, can_assign: bool) {
    str_obj, alloc_err := allocate_string(parser.previous.source, parser.vm)
    if alloc_err != runtime.Allocator_Error.None {
        error_at_current(parser, "Can't allocate constant, not enough memory")
        return
    }

    emit_constant(parser, str_obj)
}

variable :: proc(parser: ^Parser, can_assign: bool) {
    named_variable(parser, parser.previous, can_assign)
}

resolve_local :: proc(parser: ^Parser, name: Token) -> int {
    compiler := parser.compiler
    for i := compiler.localCount - 1; i >= 0 ; i -= 1 {
        if name.source == compiler.locals[i].name.source {

            if compiler.locals[i].depth == - 1 {
                error(parser, "Can't read local variables in its own initializer.")
            }

            return i
        }
    }
    return -1
}

named_variable :: proc(parser: ^Parser, tok: Token, can_assign: bool) {

    arg := resolve_local(parser, tok)
    get_op, set_op : byte

    if arg != -1 {
        get_op = byte(OpCode.OP_GET_LOCAL)
        set_op = byte(OpCode.OP_SET_LOCAL)
    } else {
        arg = int(identifier_constant(parser, tok))
        get_op = byte(OpCode.OP_GET_GLOBAL)
        set_op = byte(OpCode.OP_SET_GLOBAL)
    }

    if (can_assign && match_token(parser, TokenType.EQUAL)) {
        expression(parser)
        emit_bytes(parser, set_op, byte(arg))
    } else {
        emit_bytes(parser, get_op, byte(arg))
    }
}

number :: proc(parser: ^Parser, can_assign: bool) {
    value, ok := strconv.parse_f64(parser.previous.source)
    if !ok {
        error_at_current(parser, "Input is not a valid number")
        return
    }

    emit_constant(parser, value)
}

grouping :: proc(parser: ^Parser, can_assign: bool) {
    expression(parser)
    consume_token(parser, TokenType.RIGHT_PAREN, "Expect ')' after expression.")
}

unary :: proc(parser: ^Parser, can_assign: bool) {
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

    can_assign := precedence <= Precedence.ASSIGNMENT

    prefix_rule(parser, can_assign)

    for precedence <= get_rule(parser.current.type).precedence {
        advance_compiler(parser)
        infix_rule := get_rule(parser.previous.type).infix
        infix_rule(parser, can_assign)
    }

    if can_assign && match_token(parser, TokenType.EQUAL) {
        error(parser, "Invalid assignment target.")
    }
}

binary :: proc(parser: ^Parser, can_assign: bool) {
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
        .IDENTIFIER    = { prefix=variable, infix=nil,    precedence=.NONE },
        .STRING        = { prefix=str,      infix=nil,    precedence=.NONE },
        .NUMBER        = { prefix=number,   infix=nil,    precedence=.NONE },
        .AND           = { prefix=nil,      infix=and_,   precedence=.AND },
        .CLASS         = { prefix=nil,      infix=nil,    precedence=.NONE },
        .ELSE          = { prefix=nil,      infix=nil,    precedence=.NONE },
        .FALSE         = { prefix=literal,  infix=nil,    precedence=.NONE },
        .FOR           = { prefix=nil,      infix=nil,    precedence=.NONE },
        .FUN           = { prefix=nil,      infix=nil,    precedence=.NONE },
        .IF            = { prefix=nil,      infix=nil,    precedence=.NONE },
        .NIL           = { prefix=literal,  infix=nil,    precedence=.NONE },
        .OR            = { prefix=nil,      infix=or_,    precedence=.OR   },
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
