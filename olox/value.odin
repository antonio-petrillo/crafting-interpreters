package main

import "core:fmt"

Value :: union {
    f64,
}


print_value :: proc(value: Value) {
    switch v in value  {
    case f64:
        fmt.printf("%f", v)
    }
}
