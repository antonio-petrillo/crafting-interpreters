package main

import "core:fmt"
import "core:testing"

@(test)
test_compiler_had_error_on_invalid_source :: proc(t: ^testing.T) {
    source := ")("
    chunk := &Chunk{}
    vm := &VM{}
    testing.expect_value(t, compile(source, chunk, vm), false)
    free_chunk(chunk)
    free_vm(vm)
}

@(test)
test_compiler_compile_number :: proc(t: ^testing.T) {
    source := "1234"
    chunk := &Chunk{}
    vm := &VM{}
    testing.expect_value(t, compile(source, chunk, vm), true)
    testing.expect_value(t, chunk.constants[0], 1234)
    testing.expect_value(t, chunk.code[0], byte(OpCode.OP_CONSTANT))
    free_chunk(chunk)
    free_vm(vm)
}

@(test)
test_compiler_compile_negative_number :: proc(t: ^testing.T) {
    source := "-1234"
    chunk := &Chunk{}
    vm := &VM{}
    testing.expect_value(t, compile(source, chunk, vm), true)
    testing.expect_value(t, chunk.constants[0], 1234)
    testing.expect_value(t, chunk.code[0], byte(OpCode.OP_CONSTANT))
    testing.expect_value(t, chunk.code[1], 0x00) // address of the first constant
    testing.expect_value(t, chunk.code[2], byte(OpCode.OP_NEGATE))
    free_chunk(chunk)
    free_vm(vm)
}

@(test)
test_compiler_compile_sum :: proc(t: ^testing.T) {
    source := "1+2"
    chunk := &Chunk{}
    vm := &VM{}
    testing.expect_value(t, compile(source, chunk, vm), true)
    testing.expect_value(t, chunk.constants[0], 1)
    testing.expect_value(t, chunk.constants[1], 2)
    testing.expect_value(t, chunk.code[0], byte(OpCode.OP_CONSTANT))
    testing.expect_value(t, chunk.code[1], 0x00) // address of the first constant
    testing.expect_value(t, chunk.code[2], byte(OpCode.OP_CONSTANT))
    testing.expect_value(t, chunk.code[3], 0x01) // address of the first constant
    testing.expect_value(t, chunk.code[4], byte(OpCode.OP_ADD))
    free_chunk(chunk)
    free_vm(vm)
}

@(test)
test_compiler_greater_expr :: proc(t: ^testing.T) {
    source := "1>2"
    chunk := &Chunk{}
    vm := &VM{}
    testing.expect_value(t, compile(source, chunk, vm), true)
    testing.expect_value(t, chunk.constants[0], 1)
    testing.expect_value(t, chunk.constants[1], 2)
    testing.expect_value(t, chunk.code[0], byte(OpCode.OP_CONSTANT))
    testing.expect_value(t, chunk.code[1], 0x00) // address of the first constant
    testing.expect_value(t, chunk.code[2], byte(OpCode.OP_CONSTANT))
    testing.expect_value(t, chunk.code[3], 0x01) // address of the first constant
    testing.expect_value(t, chunk.code[4], byte(OpCode.OP_GREATER))
    free_chunk(chunk)
    free_vm(vm)
}

@(test)
test_compiler_greater_equal_expr :: proc(t: ^testing.T) {
    source := "1>=2"
    chunk := &Chunk{}
    vm := &VM{}
    testing.expect_value(t, compile(source, chunk, vm), true)
    testing.expect_value(t, chunk.constants[0], 1)
    testing.expect_value(t, chunk.constants[1], 2)
    testing.expect_value(t, chunk.code[0], byte(OpCode.OP_CONSTANT))
    testing.expect_value(t, chunk.code[1], 0x00) // address of the first constant
    testing.expect_value(t, chunk.code[2], byte(OpCode.OP_CONSTANT))
    testing.expect_value(t, chunk.code[3], 0x01) // address of the first constant
    testing.expect_value(t, chunk.code[4], byte(OpCode.OP_LESS))
    testing.expect_value(t, chunk.code[5], byte(OpCode.OP_NOT))
    free_chunk(chunk)
    free_vm(vm)
}

@(test)
test_compiler_less_expr :: proc(t: ^testing.T) {
    source := "1<2"
    chunk := &Chunk{}
    vm := &VM{}
    testing.expect_value(t, compile(source, chunk, vm), true)
    testing.expect_value(t, chunk.constants[0], 1)
    testing.expect_value(t, chunk.constants[1], 2)
    testing.expect_value(t, chunk.code[0], byte(OpCode.OP_CONSTANT))
    testing.expect_value(t, chunk.code[1], 0x00) // address of the first constant
    testing.expect_value(t, chunk.code[2], byte(OpCode.OP_CONSTANT))
    testing.expect_value(t, chunk.code[3], 0x01) // address of the first constant
    testing.expect_value(t, chunk.code[4], byte(OpCode.OP_LESS))
    free_chunk(chunk)
    free_vm(vm)
}

@(test)
test_compiler_less_equal_expr :: proc(t: ^testing.T) {
    source := "1<=2"
    chunk := &Chunk{}
    vm := &VM{}
    testing.expect_value(t, compile(source, chunk, vm), true)
    testing.expect_value(t, chunk.constants[0], 1)
    testing.expect_value(t, chunk.constants[1], 2)
    testing.expect_value(t, chunk.code[0], byte(OpCode.OP_CONSTANT))
    testing.expect_value(t, chunk.code[1], 0x00) // address of the first constant
    testing.expect_value(t, chunk.code[2], byte(OpCode.OP_CONSTANT))
    testing.expect_value(t, chunk.code[3], 0x01) // address of the first constant
    testing.expect_value(t, chunk.code[4], byte(OpCode.OP_GREATER))
    testing.expect_value(t, chunk.code[5], byte(OpCode.OP_NOT))
    free_chunk(chunk)
    free_vm(vm)
}

@(test)
test_compiler_string :: proc(t: ^testing.T) {
    source := "\"asdfasdfa\""
    chunk := &Chunk{}
    vm := &VM{}
    testing.expect_value(t, compile(source, chunk, vm), true)
    testing.expect_value(t, chunk.code[0], byte(OpCode.OP_CONSTANT))
    free_chunk(chunk)
    free_vm(vm)
}
