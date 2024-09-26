package main

import "core:fmt"

DEBUG_TRACE_EXECUTION :: true

main :: proc() {
    init_vm()

    c : Chunk = {}

    init_chunk(&c)

    constant := add_constant(&c, 1.2)
    write_chunk(&c, byte(OpCode.OP_CONSTANT), 123)
    write_chunk(&c, byte(constant), 123) // this is uint!!!


    write_chunk(&c, byte(OpCode.OP_RETURN), 124)

    interpret(&c)

    free_vm()
    free_chunk(&c)

}
