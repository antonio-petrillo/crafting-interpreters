package main

import "core:fmt"

Value :: union {
    f64, // number
    LoxNil,
    bool,
}

LoxNil :: #type struct{}

LoxNilValue :: LoxNil{}

ValueType :: distinct enum u8 {
    BOOL,
    NIL,
    NUMBER,
}

print_value :: proc(value: Value) {
    switch v in value  {
    case f64:
        fmt.printf("%f", v)
    case bool:
        fmt.printf("%t", v)
    case LoxNil:
        fmt.printf("nil")
    }
}

values_equal :: proc(a, b: Value) -> bool {
    if type_of(a) != type_of(b) {
        return false
    }
    switch v in a {
    case bool:
        return a == b.(bool)
    case f64:
        return a == b.(f64)
    case LoxNil:
        return true
    case:
        return false
    }
}
