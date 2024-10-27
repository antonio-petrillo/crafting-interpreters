package main

import "core:fmt"
import "core:os"

// TODO: 19.5 freeing objects

DEBUG_TRACE_EXECUTION :: true
DEBUG_PRINT_CODE :: true

main :: proc() {
    vm := &VM{}
    init_vm(vm)

    if len(os.args) == 1 {
        repl(vm)
    } else if len(os.args) == 2 {
        run_file(vm, os.args[1])
    } else {
        fmt.eprintf("usage: olox [path]\n")
        os.exit(64)
    }

    free_vm(vm)
}

BUFFER_SIZE :: 1024

repl :: proc(vm: ^VM) {
    buffer: [BUFFER_SIZE]byte
    for {
        fmt.printf("olox => ")

        n, err := os.read(os.stdin, buffer[:])
        if err != os.ERROR_NONE {
            fmt.printf("** ERROR READING LINE **\n")
            continue
        }

        line := string(buffer[:n])

        interpret(vm, line)
    }
}

read_file :: proc(path: string) -> string {
    bytes, ok := os.read_entire_file_from_filename(path)
    if !ok {
        fmt.eprintf("Could not open|read file \"%s\". \n", path)
        os.exit(74)
    }
    return string(bytes)
}

run_file :: proc(vm: ^VM, path: string) {
    source := read_file(path)
    result := interpret(vm, source)

    #partial switch result {
    case .INTERPRET_COMPILE_ERROR:
        os.exit(65)
    case .INTERPRET_ERROR:
        os.exit(70)
    }
}
