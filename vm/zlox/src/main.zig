const std = @import("std");
const zlox = @import("zlox.zig");

const PROMPT = "zlox>";

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    defer _ = gpa.deinit();

    var vm = zlox.VM.init(gpa.allocator());

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

fn repl(vm: *zlox.VM) !void {
    var buf = std.io.bufferedReader(std.io.getStdIn().reader());
    var reader = buf.reader();
    var line_buf: [4096]u8 = undefined;

    while (true) {
        std.debug.print("{s} ", .{PROMPT});
        defer std.debug.print("\n", .{});
        if (try reader.readUntilDelimiterOrEof(&line_buf, '\n')) |line| {
            std.debug.print("line := {s}", .{line});
            vm.interpret(line) catch |err| switch (err) {
                error.RuntimeErr => std.process.exit(70),
                error.CompileErr => std.process.exit(65),
            };
        }
    }
}

fn runFile(vm: *zlox.VM, path: []const u8) !void {
    const file = std.fs.cwd().openFile(path, .{ .mode = .read_only }) catch |err| switch (err) {
        error.FileNotFound => {
            std.debug.print("Could not open '{s}' to run.\n", .{path});
            std.process.exit(74);
        },
        else => return err,
    };
    defer file.close();

    const size = try file.getEndPos();
    const source = if (size <= 1) {
        std.debug.print("File to small to contain any meaninful program: {d}\n", .{size});
        std.process.exit(65);
    } else try vm.alloc.alloc(u8, size);
    defer vm.alloc.free(source);

    vm.interpret(source) catch |err| switch (err) {
        error.RuntimeErr => std.process.exit(70),
        error.CompileErr => std.process.exit(65),
    };
}
