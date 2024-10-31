package main

import "core:fmt"

disassemble_chunk :: proc(c: ^Chunk, name: string) {
    fmt.printf("== %s ==\n", name)

    for offset : uint = 0; offset < len(c.code);  {
        offset = disassemble_instruction(c, offset)
    }
}

disassemble_instruction :: proc(c: ^Chunk, offset: uint) -> uint {
    fmt.printf("%04d\t", offset)
    if offset > 0 && c.lines[offset] == c.lines[offset - 1] {
        fmt.printf("\t| ")
    } else {
        fmt.printf("%4d ", c.lines[offset])
    }

    instruction := c.code[offset]
    switch instruction {
    case byte(OpCode.OP_RETURN):
        return simple_instruction("OP_RETURN", offset)

    case byte(OpCode.OP_CONSTANT):
        return constant_instruction("OP_CONSTANT", c, offset)

    case byte(OpCode.OP_NEGATE):
        return simple_instruction("OP_NEGATE", offset)

    case byte(OpCode.OP_EQUAL):
        return simple_instruction("OP_EQUAL", offset)

    case byte(OpCode.OP_GREATER):
        return simple_instruction("OP_GREATER", offset)

    case byte(OpCode.OP_LESS):
        return simple_instruction("OP_LESS", offset)

    case byte(OpCode.OP_PRINT):
        return simple_instruction("OP_PRINT", offset)

    case byte(OpCode.OP_POP):
        return simple_instruction("OP_POP", offset)

    case byte(OpCode.OP_DEFINE_GLOBAL):
        return constant_instruction("OP_DEFINE_GLOBAL", c, offset)

    case byte(OpCode.OP_GET_GLOBAL):
        return constant_instruction("OP_GET_GLOBAL", c, offset)

    case byte(OpCode.OP_ADD):
        return simple_instruction("OP_ADD", offset)

    case byte(OpCode.OP_SUBTRACT):
        return simple_instruction("OP_SUBTRACT", offset)

    case byte(OpCode.OP_MULTIPLY):
        return simple_instruction("OP_MULTIPLY", offset)

    case byte(OpCode.OP_DIVIDE):
        return simple_instruction("OP_DIVIDE", offset)

    case byte(OpCode.OP_NIL):
        return simple_instruction("OP_NIL", offset)

    case byte(OpCode.OP_FALSE):
        return simple_instruction("OP_FALSE", offset)

    case byte(OpCode.OP_TRUE):
        return simple_instruction("OP_TRUE", offset)

    case byte(OpCode.OP_NOT):
        return simple_instruction("OP_NOT", offset)

    case:
        fmt.printf("Unknown opcode %d\n", instruction)
        return offset + 1
    }
}

simple_instruction :: proc(name: string, offset: uint) -> uint {
    fmt.printf("%s\n", name)
    return offset + 1
}

constant_instruction :: proc(name: string, c: ^Chunk, offset: uint) -> uint {
    constant := c.code[offset + 1]
    fmt.printf("%-16s %4d '", name, constant)
    print_value(c.constants[constant])
    fmt.printf("'\n")
    return offset + 2
}
