package main

import "core:fmt"

VM :: struct {
    chunk: ^Chunk,
    ip: uint,
}

vm := VM{}

init_vm :: proc() {
   vm.ip = 0
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
            disassemble_instruction(vm.chunk, vm.ip)
        }

        instr := read_byte()

        switch instr {
        case byte(OpCode.OP_RETURN):
            return .INTERPRET_OK
        case byte(OpCode.OP_CONSTANT):
            constant := read_constant()
            print_value(constant)
            fmt.printf("\n")
        }
    } 
}
