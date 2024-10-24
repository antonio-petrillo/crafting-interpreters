package main

OpCode :: enum byte {
    OP_CONSTANT,
    OP_ADD,
    OP_SUBTRACT,
    OP_MULTIPLY,
    OP_DIVIDE,
    OP_NEGATE,
    OP_RETURN,
}

Chunk :: struct {
    code: [dynamic]byte,
    constants: [dynamic]Value, // assume max lenght of this is 256
    lines: [dynamic]uint,
}

init_chunk :: proc(c: ^Chunk) {

}

write_chunk :: proc(c: ^Chunk, b: byte, line: uint) {
    append(&c.code, b)
    append(&c.lines, line)
}

free_chunk :: proc(c: ^Chunk) {
    delete(c.code)
    delete(c.constants)
    delete(c.lines)
}

add_constant :: proc(c: ^Chunk, v: Value) -> byte {
    append(&c.constants, v)
    return byte(len(&c.constants) - 1) // see precondition/assumption on constants
}
