package main

import "core:fmt"

STACK_SIZE :: 256

VM :: struct {
    chunk: ^Chunk,
    ip: uint,
    stack: [STACK_SIZE]Value,
    stack_index: byte,
}

vm := VM{}

reset_stack :: proc() {
   vm.stack = 0
}

stack_push :: proc(v: Value) {
    vm.stack[vm.stack_index] = v
    vm.stack_index += 1
}

stack_pop :: proc() -> Value {
    vm.stack_index -= 1
    return vm.stack[vm.stack_index]
}

init_vm :: proc() {
    vm.ip = 0
    reset_stack()
}

free_vm :: proc() {

}

InterpretResult :: enum {
    INTERPRET_OK,
    INTERPRET_COMPILE_ERROR,
    INTERPRET_ERROR,
}

interpret :: proc(c: ^Chunk) -> InterpretResult {
    vm.chunk = c
    return run()
}

read_byte :: proc() -> byte {
    instr := vm.chunk.code[vm.ip]
    vm.ip += 1
    return instr
}

read_constant :: proc() -> Value {
    return vm.chunk.constants[read_byte()]
}

run :: proc() -> InterpretResult {
    for {
        when DEBUG_TRACE_EXECUTION {
            fmt.printf("Stack := ( ")
            for index : u8 = 0; index < vm.stack_index; index += 1 {
                fmt.printf("[")
                print_value(vm.stack[index])
                fmt.printf("]")
            }
            fmt.printf(" )\n")
            disassemble_instruction(vm.chunk, vm.ip)
        }

        instr := read_byte()

        switch instr {
        case byte(OpCode.OP_RETURN):
            print_value(stack_pop())
            fmt.printf("\n")
            return .INTERPRET_OK

        case byte(OpCode.OP_NEGATE):
            v, ok := stack_pop().(f64)
            if !ok {
                return .INTERPRET_ERROR
            }
            stack_push(-v)

        case byte(OpCode.OP_CONSTANT):
            constant := read_constant()
            stack_push(constant)

        case byte(OpCode.OP_ADD):
            b, a := stack_pop(), stack_pop()
            b_num, b_ok := b.(f64)
            a_num, a_ok := a.(f64)

            if !b_ok || !a_ok {
                return .INTERPRET_ERROR
            }

            sum := a_num + b_num
            stack_push(sum)

        case byte(OpCode.OP_SUBTRACT):
            b, a := stack_pop(), stack_pop()
            b_num, b_ok := b.(f64)
            a_num, a_ok := a.(f64)

            if !b_ok || !a_ok {
                return .INTERPRET_ERROR
            }

            subtract := a_num - b_num
            stack_push(subtract)

        case byte(OpCode.OP_MULTIPLY):
            b, a := stack_pop(), stack_pop()
            b_num, b_ok := b.(f64)
            a_num, a_ok := a.(f64)

            if !b_ok || !a_ok {
                return .INTERPRET_ERROR
            }

            multiply := a_num * b_num
            stack_push(multiply)

        case byte(OpCode.OP_DIVIDE):
            b, a := stack_pop(), stack_pop()
            b_num, b_ok := b.(f64)
            a_num, a_ok := a.(f64)

            if !b_ok || !a_ok {
                return .INTERPRET_ERROR
            }

            division := a_num / b_num
            stack_push(division)
        }
    } 
}
