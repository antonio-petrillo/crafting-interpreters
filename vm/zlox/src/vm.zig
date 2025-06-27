const std = @import("std");
const Allocator = std.mem.Allocator;
const common = @import("common.zig");

const chunk = @import("chunk.zig");

const InterpretResult = enum {
    Ok,
    CompileErr,
    RuntimeErr,
};

pub const VM = struct {
    alloc: Allocator,

    chunk: ?*chunk.Chunk,
    ip: usize,

    stack: [256]common.Value,
    sp: usize,

    fn push(vm: *VM, v: common.Value) void {
        if (vm.sp == 256) unreachable;
        vm.stack[vm.sp] = v;
        vm.sp += 1;
    }

    fn pop(vm: *VM) common.Value {
        if (vm.sp == 0) unreachable;
        vm.sp -= 1;
        return vm.stack[vm.sp];
    }

    pub fn init(alloc: Allocator) VM {
        return .{
            .alloc = alloc,

            .chunk = null,
            .ip = 0,

            .stack = [1]common.Value{undefined} ** 256,
            .sp = 0,
        };
    }

    pub fn deinit(vm: *VM) void {
        if (vm.chunk) |c| {
            c.deinit();
        }
        vm.chunk = null;
    }

    pub fn interpret(vm: *VM, c: *chunk.Chunk) !InterpretResult {
        vm.chunk = c;
        vm.ip = 0;
        return try vm.run();
    }

    fn run(vm: *VM) !InterpretResult {
        if (vm.chunk == null) {
            return .RuntimeErr;
        }
        while (true) {
            if (common.DEBUG_TRACE_EXCECUTION) {
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

const testing = std.testing;

test "simple vm execution" {
    var c = chunk.Chunk.init(testing.allocator);

    try c.constants.append(.{ .number = 1.2 });
    try c.writeChunk(.{ .Constant = 0 }, 123);
    try c.writeChunk(.Negate, 123);
    try c.writeChunk(.Return, 123);

    var vm = VM.init(testing.allocator);
    defer vm.deinit();

    _ = try vm.interpret(&c);
}
