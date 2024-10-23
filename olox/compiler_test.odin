package main

import "core:fmt"
import "core:testing"

@(test)
test_compiler_had_error_on_invalid_source :: proc(t: ^testing.T) {
    source := ")("
    chunk := &Chunk{}
    testing.expect_value(t, compile(source, chunk), false)
    free_chunk(chunk)
}

@(test)
test_compiler_compile_number :: proc(t: ^testing.T) {
    source := "1234"
    chunk := &Chunk{}
    testing.expect_value(t, compile(source, chunk), true)
    testing.expect_value(t, chunk.constants[0], 1234)
    testing.expect_value(t, chunk.code[0], byte(OpCode.OP_CONSTANT))
    free_chunk(chunk)
}

// TODO: understand why this fail
@(test)
test_compiler_compile_negative_number :: proc(t: ^testing.T) {
    source := "-1234"
    chunk := &Chunk{}
    testing.expect_value(t, compile(source, chunk), true)
    testing.expect_value(t, chunk.constants[0], 1234)
    testing.expect_value(t, chunk.code[0], byte(OpCode.OP_CONSTANT))
    testing.expect_value(t, chunk.code[1], byte(OpCode.OP_NEGATE))
    free_chunk(chunk)
}
