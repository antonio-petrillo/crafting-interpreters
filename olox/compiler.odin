package main

compile :: proc(vm: ^VM, source: string) {
    scanner := &Scanner{}
    init_scanner(scanner, source)
}
