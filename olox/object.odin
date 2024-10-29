package main

import "base:runtime"

Obj :: struct {
    next: ^Obj,
}

ObjString :: struct {
    using obj: Obj,
    str: string,
    hash: uint,
}

allocate_string :: proc(str: string, vm: ^VM) -> (^ObjString, runtime.Allocator_Error) #optional_allocator_error {
    str_obj, alloc_err := new(ObjString)
    if alloc_err != runtime.Allocator_Error.None {
        return nil, alloc_err
    }
    str_obj.str = str
    str_obj.hash = hash(str)
    str_obj.next = vm.objects
    vm.objects = str_obj
    return str_obj, alloc_err
}

hash :: proc(str: string) -> uint {
    h : uint = 2166136261
    for ch in str {
        h ~= uint(ch)
        h *= 16777619
    }
    return h
}
