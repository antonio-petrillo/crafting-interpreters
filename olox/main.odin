package main

import "core:fmt"

main :: proc() {
    c : Chunk = {}

    init_chunk(&c)

    constant := add_constant(&c, 1.2)
    write_chunk(&c, byte(OpCode.OP_CONSTANT), 123)
    write_chunk(&c, byte(constant), 123) // this is uint!!!


    write_chunk(&c, byte(OpCode.OP_RETURN), 124)
    disassemble_chunk(&c, "test chunk")

    free_chunk(&c)

}
