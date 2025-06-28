const std = @import("std");
const Allocator = std.mem.Allocator;

const testing = std.testing;
pub const DEBUG_TRACE_EXCECUTION = true;

pub const Value = union(enum) {
    number: f64,

    pub fn debugPrint(v: *const Value) void {
        switch (v.*) {
            .number => |n| std.debug.print("{d}", .{n}),
        }
    }
};

pub const OpCode = union(enum(u8)) {
    Return,
    Constant: u8,
    Negate,
    Add,
    Subtract,
    Multiply,
    Divide,
};

pub const Chunk = struct {
    code: std.ArrayList(OpCode),
    constants: std.ArrayList(Value),
    lines: std.ArrayList(usize),

    pub fn init(alloc: Allocator) Chunk {
        return .{
            .code = std.ArrayList(OpCode).init(alloc),
            .constants = std.ArrayList(Value).init(alloc),
            .lines = std.ArrayList(usize).init(alloc),
        };
    }

    pub fn deinit(c: *Chunk) void {
        c.code.deinit();
        c.constants.deinit();
        c.lines.deinit();
    }

    pub fn writeChunk(c: *Chunk, op: OpCode, line: usize) !void {
        try c.code.append(op);
        try c.lines.append(line);
    }

    pub fn disassembleInstruction(c: *Chunk, offset: usize) void {
        defer std.debug.print("\t[at line {d}]\n", .{c.lines.items[offset]});
        std.debug.print("{d:0>4} ", .{offset});

        switch (c.code.items[offset]) {
            .Return => std.debug.print(".Return", .{}),
            .Constant => |idx| {
                std.debug.print(".Constant at idx [{d}] => '", .{idx});
                c.constants.items[idx].debugPrint();
                std.debug.print("'", .{});
            },
            .Negate => std.debug.print(".Negate", .{}),
            .Add => std.debug.print(".Add", .{}),
            .Subtract => std.debug.print(".Subtract", .{}),
            .Multiply => std.debug.print(".Multiply", .{}),
            .Divide => std.debug.print(".Divide", .{}),
        }
    }

    pub fn disassemble(c: *Chunk, header: []const u8) void {
        std.debug.print("== {s} ==\n", .{header});
        for (0..c.code.items.len) |i| {
            c.disassembleInstruction(i);
        }
    }
};

test "simple chunk test" {
    var c = Chunk.init(testing.allocator);
    defer c.deinit();

    try c.constants.append(.{ .number = 1.2 });
    try c.writeChunk(.{ .Constant = 0 }, 123);
    try c.writeChunk(.Return, 123);

    c.disassemble("test chunk");
}

pub const VMError = error{
    RuntimeErr,
    CompileErr,
};

pub const VM = struct {
    alloc: Allocator,

    chunk: ?*Chunk,
    ip: usize,

    stack: [256]Value,
    sp: usize,

    fn push(vm: *VM, v: Value) void {
        if (vm.sp == 256) unreachable;
        vm.stack[vm.sp] = v;
        vm.sp += 1;
    }

    fn pop(vm: *VM) Value {
        if (vm.sp == 0) unreachable;
        vm.sp -= 1;
        return vm.stack[vm.sp];
    }

    pub fn init(alloc: Allocator) VM {
        return .{
            .alloc = alloc,

            .chunk = null,
            .ip = 0,

            .stack = [1]Value{undefined} ** 256,
            .sp = 0,
        };
    }

    pub fn deinit(vm: *VM) void {
        if (vm.chunk) |c| {
            c.deinit();
        }
        vm.chunk = null;
    }

    pub fn interpret(vm: *VM, source: []const u8) VMError!void {
        _ = vm;
        _ = source;
        return;
    }

    fn run(vm: *VM) VMError!void {
        if (vm.chunk == null) {
            return .RuntimeErr;
        }
        while (true) {
            if (DEBUG_TRACE_EXCECUTION) {
                std.debug.print("{s:*^20}\n", .{"start debug"});
                defer std.debug.print("{s:*^20}\n", .{"end debug"});
                std.debug.print("Stack = [", .{});
                for (0..vm.sp) |sp| {
                    std.debug.print("[", .{});
                    vm.stack[sp].debugPrint();
                    std.debug.print("], ", .{});
                }
                std.debug.print("]\n", .{});

                vm.chunk.?.disassembleInstruction(vm.ip);
            }

            const instr = vm.chunk.?.code.items[vm.ip];
            switch (instr) {
                .Return => {
                    vm.pop().debugPrint();
                    std.debug.print("\n", .{});
                    return .Ok;
                },
                .Constant => |idx| {
                    const v = vm.chunk.?.constants.items[idx];
                    vm.push(v);
                },
                .Negate => {
                    switch (vm.pop()) {
                        .number => |n| vm.push(.{ .number = -n }),
                    }
                },
                .Add => {
                    switch (vm.pop()) {
                        .number => |a| {
                            switch (vm.pop()) {
                                .number => |b| vm.push(.{ .number = a + b }),
                            }
                        },
                    }
                },
                .Subtract => {
                    switch (vm.pop()) {
                        .number => |a| {
                            switch (vm.pop()) {
                                .number => |b| vm.push(.{ .number = a - b }),
                            }
                        },
                    }
                },
                .Multiply => {
                    switch (vm.pop()) {
                        .number => |a| {
                            switch (vm.pop()) {
                                .number => |b| vm.push(.{ .number = a * b }),
                            }
                        },
                    }
                },
                .Divide => {
                    switch (vm.pop()) {
                        .number => |a| {
                            switch (vm.pop()) {
                                .number => |b| vm.push(.{ .number = a / b }),
                            }
                        },
                    }
                },
            }
            vm.ip += 1;
        }
    }
};
