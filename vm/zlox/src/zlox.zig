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

const TokenType = enum(u8) {
    // Single Character Tokens.
    LeftParen,
    RightParen,
    LeftBrace,
    RightBrace,
    Comma,
    Dot,
    Minus,
    Plus,
    Semicolon,
    Slash,
    Star,

    // One or Two Character Tokens.
    Bang,
    BangEqual,
    Equal,
    EqualEqual,
    Greater,
    GreaterEqual,
    Less,
    LessEqual,

    // Literals.
    Identifier,
    String,
    Number,

    // Keywords.
    And,
    Class,
    Else,
    False,
    For,
    Fun,
    If,
    Nil,
    Or,
    Print,
    Return,
    Super,
    This,
    True,
    Var,
    While,

    pub fn str(tok: TokenType) []const u8 {
        return switch (tok) {
            .LeftParen => "LeftParen",
            .RightParen => "RightParen",
            .LeftBrace => "LeftBrace",
            .RightBrace => "RightBrace",
            .Comma => "Comma",
            .Dot => "Dot",
            .Minus => "Minus",
            .Plus => "Plus",
            .Semicolon => "Semicolon",
            .Slash => "Slash",
            .Star => "Star",
            .Bang => "Bang",
            .BangEqual => "BangEqual",
            .Equal => "Equal",
            .EqualEqual => "EqualEqual",
            .Greater => "Greater",
            .GreaterEqual => "GreaterEqual",
            .Less => "Less",
            .LessEqual => "LessEqual",
            .Identifier => "Identifier",
            .String => "String",
            .Number => "Number",
            .And => "And",
            .Class => "Class",
            .Else => "Else",
            .False => "False",
            .For => "For",
            .Fun => "Fun",
            .If => "If",
            .Nil => "Nil",
            .Or => "Or",
            .Print => "Print",
            .Return => "Return",
            .Super => "Super",
            .This => "This",
            .True => "True",
            .Var => "Var",
            .While => "While",
        };
    }
};

const Token = struct {
    type_: TokenType,
    line: usize,
    source: []const u8,
};

const ScannerErr = error{
    EOF,
    UnclosedString,
};

const Scanner = struct {
    source: []const u8,
    start: usize,
    current: usize,
    line: usize,

    fn init(source: []const u8) Scanner {
        return .{
            .source = source,
            .start = 0,
            .current = 0,
            .line = 1,
        };
    }

    fn advance(s: *Scanner) ScannerErr!u8 {
        if (s.current >= s.source.len) return ScannerErr.EOF;
        defer s.current += 1;
        return s.source[s.current];
    }

    fn lookup(s: *Scanner, offset: usize) ?u8 {
        if (s.current + offset >= s.source.len) return null;
        return s.source[s.current + offset];
    }

    fn match(s: *Scanner, offset: usize, target: u8) bool {
        if (s.current + offset >= s.source.len) return false;
        return s.source[s.current + offset] == target;
    }

    fn token(s: *Scanner, tt: TokenType) Token {
        return .{
            .source = s.source[s.start..s.current],
            .line = s.line,
            .type_ = tt,
        };
    }

    fn skipWhitespaces(s: *Scanner) void {
        if (s.current >= s.source.len) return;
        var index: usize = s.current;
        blk: switch (s.source[index]) {
            ' ', '\r', '\t' => {
                index += 1;
                if (index < s.source.len) continue :blk s.source[index];
            },
            '\n' => {
                s.line += 1;
                index += 1;
                if (index < s.source.len) continue :blk s.source[index];
            },
            '/' => {
                if (s.match(index + 1, '/')) {
                    while (index < s.source.len and s.source[index] != '\n') : (index += 1) {}
                    if (index < s.source.len) continue :blk s.source[index];
                }
            },
            else => {},
        }
        s.current = index;
    }

    fn string(s: *Scanner) ScannerErr!Token {
        while (s.current < s.source.len) : (s.current += 1) {
            if (s.source[s.current] == '"') {
                s.current += 1;
                return s.token(.String);
            }
        }
        return ScannerErr.UnclosedString;
    }

    fn number(s: *Scanner) Token {
        while (s.current < s.source.len and isDigit(s.source[s.current])) : (s.current += 1) {}
        if (s.current < s.source.len and s.source[s.current] == '.') {
            s.current += 1;
            while (s.current < s.source.len and isDigit(s.source[s.current])) : (s.current += 1) {}
        }
        return s.token(.Number);
    }

    fn checkKeyword(src: []const u8, keyword: []const u8, typeOnMatch: TokenType) TokenType {
        return if (std.mem.eql(u8, src, keyword)) typeOnMatch else .Identifier;
    }

    fn identifier(s: *Scanner, start: u8) Token {
        var index: usize = s.current + 1;
        while (index < s.source.len and isAlpha(s.source[index])) : (index += 1) {}
        const str = s.source[s.current..index];
        const tt = switch (start) {
            'a' => checkKeyword(str, "nd", .And),
            'c' => checkKeyword(str, "lass", .Class),
            'e' => checkKeyword(str, "lse", .Else),
            'i' => checkKeyword(str, "f", .If),
            'n' => checkKeyword(str, "il", .Nil),
            'o' => checkKeyword(str, "r", .Or),
            'p' => checkKeyword(str, "rint", .Print),
            'r' => checkKeyword(str, "eturn", .Return),
            's' => checkKeyword(str, "uper", .Super),
            'v' => checkKeyword(str, "ar", .Var),
            'w' => checkKeyword(str, "hile", .While),
            'f' => blk1: {
                if (str.len > 0) {
                    switch (str[0]) {
                        'a' => break :blk1 checkKeyword(str[1..], "lse", .False),
                        'o' => break :blk1 checkKeyword(str[1..], "r", .For),
                        'u' => break :blk1 checkKeyword(str[1..], "n", .Fun),
                        else => break :blk1 .Identifier,
                    }
                } else break :blk1 .Identifier;
            },
            't' => blk2: {
                if (str.len > 0) {
                    switch (str[0]) {
                        'h' => break :blk2 checkKeyword(str[1..], "is", .This),
                        'r' => break :blk2 checkKeyword(str[1..], "ue", .True),
                        else => break :blk2 .Identifier,
                    }
                } else break :blk2 .Identifier;
            },
            else => .Identifier,
        };

        s.current = index;
        return s.token(tt);
    }

    fn nextToken(s: *Scanner) ScannerErr!Token {
        s.skipWhitespaces();
        s.start = s.current;
        const c = try s.advance();

        return switch (c) {
            '(' => s.token(.LeftParen),
            ')' => s.token(.RightParen),
            '{' => s.token(.LeftBrace),
            '}' => s.token(.RightBrace),
            ';' => s.token(.Semicolon),
            ',' => s.token(.Comma),
            '.' => s.token(.Dot),
            '+' => s.token(.Plus),
            '-' => s.token(.Minus),
            '*' => s.token(.Star),
            '/' => s.token(.Slash),
            '!' => s.token(if (s.match(1, '=')) .BangEqual else .Bang),
            '=' => s.token(if (s.match(1, '=')) .EqualEqual else .Equal),
            '<' => s.token(if (s.match(1, '=')) .LessEqual else .Less),
            '>' => s.token(if (s.match(1, '=')) .GreaterEqual else .Greater),
            '"' => try s.string(),
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' => s.number(),
            else => s.identifier(c),
        };
    }
};

fn isAlphaNumeric(c: u8) bool {
    return isAlpha(c) or isDigit(c);
}

fn isAlpha(c: u8) bool {
    return c >= 'a' and c <= 'z' or c >= 'A' and c <= 'Z' or c == '_';
}

fn isDigit(c: u8) bool {
    return switch (c) {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' => true,
        else => false,
    };
}
const code =
    \\( ) {  } + - * / . , = == != ! > >= < <=
    \\0 00 01 10 11 1.1 121 3.14159 42
    \\ "" "ciao" "Hello, World!" "00 01 10 11 > < y_y"
    \\ var if else this super false true var variableName class
    \\ and or while for fun nil return print
;

test "simple test scanner (just too see the output)" {
    var s = Scanner.init(code);

    var i: usize = 0;
    while (true) : (i += 1) {
        const tk = s.nextToken() catch |err| switch (err) {
            error.EOF => break,
            else => {
                try testing.expect(false);
                break;
            }, // Fail
        };

        std.debug.print("Token {d}-th =>\n\tsource: '{s}'\n\tline: {d}\n\ttype: '{s}'\n", .{ i, tk.source, tk.line, tk.type_.str() });
    }
}
