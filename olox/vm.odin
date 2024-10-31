package main

import "core:fmt"
import "core:strings"
import "base:runtime"

STACK_SIZE :: 256

VM :: struct {
    chunk: ^Chunk,
    ip: uint,
    stack: [STACK_SIZE]Value,
    stack_index: byte,
    objects: ^Obj,
    globals: map[string]Value,
}

vm := VM{}

reset_stack :: proc(vm: ^VM) {
   vm.stack = 0
}

stack_push :: proc(vm: ^VM, v: Value) {
    vm.stack[vm.stack_index] = v
    vm.stack_index += 1
}

stack_pop :: proc(vm: ^VM) -> Value {
    vm.stack_index -= 1
    return vm.stack[vm.stack_index]
}

stack_peek :: proc(vm: ^VM) -> Value {
    return vm.stack[vm.stack_index - 1]
}

init_vm :: proc(vm: ^VM) {
    vm.ip = 0
    reset_stack(vm)
    vm.globals = make(map[string]Value)
}

free_vm :: proc(vm: ^VM) {
    for vm.objects != nil {
        tmp := vm.objects
        vm.objects = vm.objects.next
        switch v in tmp.variant {
        case ^ObjString:
            delete(v.str)
        }
        free(tmp)
    }
    if vm.objects != nil {
        free(vm.objects)
    }
    delete(vm.globals)
    vm.ip = 0
}

InterpretResult :: enum {
    INTERPRET_OK,
    INTERPRET_COMPILE_ERROR,
    INTERPRET_ERROR,
}

interpret :: proc(vm: ^VM, source: string) -> InterpretResult {

    chunk := Chunk{}
    init_chunk(&chunk)

    /* if !compile(source, &vm.chunk, vm) { */
    vm.chunk = &chunk
    if !compile(source, vm.chunk, vm) {
        free_chunk(vm.chunk)
        return .INTERPRET_COMPILE_ERROR
    }

    /* vm.ip = uint(vm.chunk.code) */
    vm.ip = 0 // TODO: understand the shitty code in the book (too risky with this pointers!)

    result := run(vm)
    free_chunk(&chunk)
    return result
}

read_byte :: proc(vm: ^VM) -> byte {
    instr := vm.chunk.code[vm.ip]
    vm.ip += 1
    return instr
}

read_constant :: proc(vm: ^VM) -> Value {
    return vm.chunk.constants[read_byte(vm)]
}

run :: proc(vm: ^VM) -> InterpretResult {
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

        instr := read_byte(vm)

        switch instr {
        case byte(OpCode.OP_NIL):
            stack_push(vm, LoxNilValue)
        case byte(OpCode.OP_TRUE):
            stack_push(vm, true)
        case byte(OpCode.OP_FALSE):
            stack_push(vm, false)
        case byte(OpCode.OP_RETURN):
            return .INTERPRET_OK

        case byte(OpCode.OP_DEFINE_GLOBAL):
            obj_str, ok := read_constant(vm).(^ObjString)
            if !ok {
                runtime_error(vm, "Constant is not a string")
                return .INTERPRET_ERROR
            }
            vm.globals[obj_str.str] = stack_peek(vm)
            stack_pop(vm)

        case byte(OpCode.OP_GET_GLOBAL):
            obj_str, ok := read_constant(vm).(^ObjString)
            if !ok {
                runtime_error(vm, "Constant is not a string")
                return .INTERPRET_ERROR
            }

            val, found := vm.globals[obj_str.str]
            if !found {
                runtime_error(vm, "Undefined variable %q", obj_str.str)
                return .INTERPRET_ERROR
            }
            stack_push(vm, val)


        case byte(OpCode.OP_POP):
            stack_pop(vm)
            return .INTERPRET_OK

        case byte(OpCode.OP_PRINT):
            print_value(stack_pop(vm))
            fmt.printf("\n")

        case byte(OpCode.OP_EQUAL):
            v1, v2 := stack_pop(vm), stack_pop(vm)
            are_equals := values_equal(v1, v2)
            stack_push(vm, are_equals)

        case byte(OpCode.OP_NOT):
            v, ok := stack_pop(vm).(bool)
            _, is_nil := stack_pop(vm).(LoxNil)
            b := ok ? !v : false;
            stack_push(vm, is_nil ? true : b)

        case byte(OpCode.OP_NEGATE):
            v, ok := stack_pop(vm).(f64)
            if !ok {
                runtime_error(vm, "Operand must be a number")
                return .INTERPRET_ERROR
            }
            stack_push(vm, -v)

        case byte(OpCode.OP_CONSTANT):
            constant := read_constant(vm)
            stack_push(vm, constant)

        case byte(OpCode.OP_LESS):
            b, a := stack_pop(vm), stack_pop(vm)
            b_num, b_ok := b.(f64)
            a_num, a_ok := a.(f64)

            if !b_ok || !a_ok {
                runtime_error(vm, "Operands must be a number")
                reset_stack(vm)
                return .INTERPRET_ERROR
            }

            less := a_num < b_num
            stack_push(vm, less)

        case byte(OpCode.OP_GREATER):
            b, a := stack_pop(vm), stack_pop(vm)
            b_num, b_ok := b.(f64)
            a_num, a_ok := a.(f64)

            if !b_ok || !a_ok {
                runtime_error(vm, "Operands must be a number")
                reset_stack(vm)
                return .INTERPRET_ERROR
            }

            greater := a_num > b_num
            stack_push(vm, greater)

        case byte(OpCode.OP_ADD):
            b, a := stack_pop(vm), stack_pop(vm)

            if type_of(a) != type_of(b) {
                runtime_type_error(vm, "Operands must have the same type")
            }

            #partial switch v in a {
            case f64:
                b_num, ok := b.(f64)
                if !ok {
                    runtime_type_error(vm, "First operand is number but second is not")
                }
                sum := v + b_num
                stack_push(vm, sum)
            case ^ObjString:
                b_obj, ok := b.(^ObjString)
                if !ok {
                    runtime_type_error(vm, "First operand is string but second is not")
                }

                concatenated, alloc_err := allocate_string(strings.concatenate([]string{v.str, b_obj.str}), vm)
                if alloc_err != runtime.Allocator_Error.None {
                    runtime_error(vm, "[Allocation failed] String concatenation error")
                    reset_stack(vm)
                    return .INTERPRET_ERROR
                }
                stack_push(vm, concatenated)
            }

        case byte(OpCode.OP_SUBTRACT):
            b, a := stack_pop(vm), stack_pop(vm)
            b_num, b_ok := b.(f64)
            a_num, a_ok := a.(f64)

            if !b_ok || !a_ok {
                runtime_error(vm, "Operands must be a number")
                reset_stack(vm)
                return .INTERPRET_ERROR
            }

            subtract := a_num - b_num
            stack_push(vm, subtract)

        case byte(OpCode.OP_MULTIPLY):
            b, a := stack_pop(vm), stack_pop(vm)
            b_num, b_ok := b.(f64)
            a_num, a_ok := a.(f64)

            if !b_ok || !a_ok {
                runtime_error(vm, "Operands must be a number")
                reset_stack(vm)
                return .INTERPRET_ERROR
            }

            multiply := a_num * b_num
            stack_push(vm, multiply)

        case byte(OpCode.OP_DIVIDE):
            b, a := stack_pop(vm), stack_pop(vm)
            b_num, b_ok := b.(f64)
            a_num, a_ok := a.(f64)

            if !b_ok || !a_ok {
                runtime_error(vm, "Operands must be a number")
                reset_stack(vm)
                return .INTERPRET_ERROR
            }

            if b_num == 0 {
                runtime_error(vm, "Cannot divide by 0")
                reset_stack(vm)
                return .INTERPRET_ERROR
            }

            division := a_num / b_num
            stack_push(vm, division)
        }
    } 
}

runtime_type_error :: proc(vm: ^VM, format: string, args: ..any) -> InterpretResult {
    runtime_error(vm, "Operands must have the same type")
    return .INTERPRET_ERROR
}

runtime_error :: proc(vm: ^VM, format: string,  args: ..any) {
    fmt.eprintf("\n")
    fmt.eprintf(format, ..args)
    fmt.eprintf(" at [line %d] in script\n", vm.chunk.lines[len(vm.chunk.lines) - 1])
    reset_stack(vm)
}
