const std = @import("std");
const chunk_ = @import("chunk.zig");
const vm_ = @import("vm.zig");

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    defer _ = gpa.deinit();

    var vm = vm_.VM.init(gpa.allocator());

    var args = std.process.args();
    _ = args.skip();

    if (args.next()) |path| {
        if (args.skip()) {
            std.process.exit(64);
        }
        try runFile(&vm, path);
    } else {
        try repl(&vm);
    }
}

fn repl(vm: *vm_.VM) !void {
    _ = vm;
    var buf = std.io.bufferedReader(std.io.getStdIn().reader());
    var reader = buf.reader();
    var line_buf: [4096]u8 = undefined;

    while (true) {
        if (try reader.readUntilDelimiterOrEof(&line_buf, '\n')) |line| {
            std.debug.print("line := {s}\n", .{line});
        }
    }
}

fn runFile(vm: *vm_.VM, path: []const u8) !void {
    _ = vm;
    _ = path;
}
