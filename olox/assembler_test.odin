package main

import "core:fmt"
import "core:testing"

@(test)
test_assembler_chapter14 :: proc(t: ^testing.T) {
    c := Chunk{}
    init_chunk(&c)

    constant := add_constant(&c, 1.2)
    write_chunk(&c, byte(OpCode.OP_CONSTANT), 123)
    write_chunk(&c, byte(constant), 123)
    write_chunk(&c, byte(OpCode.OP_RETURN), 124)

    expect : [3]byte = { byte(OpCode.OP_CONSTANT), byte(constant), byte(OpCode.OP_RETURN) }
    index := 0
    for code in c.code {
        testing.expect(t, code == expect[index],  "bytes don't match")
        index += 1
    }
    val, ok := c.constants[constant].(f64)
    testing.expect(t, ok,  "constant is not a f64")
    testing.expect_value(t, val, 1.2)

    free_chunk(&c)
}
