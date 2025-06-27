const std = @import("std");
const Allocator = std.mem.Allocator;
const common = @import("common.zig");

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
    constants: std.ArrayList(common.Value),
    lines: std.ArrayList(usize),

    pub fn init(alloc: Allocator) Chunk {
        return .{
            .code = std.ArrayList(OpCode).init(alloc),
            .constants = std.ArrayList(common.Value).init(alloc),
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

const testing = std.testing;

test "simple chunk test" {
    var c = Chunk.init(testing.allocator);
    defer c.deinit();

    try c.constants.append(.{ .number = 1.2 });
    try c.writeChunk(.{ .Constant = 0 }, 123);
    try c.writeChunk(.Return, 123);

    c.disassemble("test chunk");
}
