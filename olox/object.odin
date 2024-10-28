package main

Obj :: struct {
    next: ^Obj,
}

ObjString :: struct {
    using obj: Obj,
    str: string,
}
