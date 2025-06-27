const std = @import("std");

pub const DEBUG_TRACE_EXCECUTION = true;

pub const Value = union(enum) {
    number: f64,

    pub fn debugPrint(v: *const Value) void {
        switch (v.*) {
            .number => |n| std.debug.print("{d}", .{n}),
        }
    }
};
