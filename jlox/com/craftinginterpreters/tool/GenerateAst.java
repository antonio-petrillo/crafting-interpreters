package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }

        String outputDir = args[0];
        defineAst(outputDir, "Expr",
                  List.of("Assign   : Token name, Expr value",
                          "Binary   : Expr left, Token operator, Expr right",
                          "Grouping : Expr expression",
                          "Literal  : Object value",
                          "Unary    : Token operator, Expr right",
                          "Variable : Token name"
                          ));

        defineAst(outputDir, "Stmt",
                  List.of("Expression : Expr expression",
                          "Block      : List<Stmt> statements",
                          "Print      : Expr expression",
                          "Var        : Token name, Expr initializer"
                          ));
    }

    private static void defineAst(String outputDir, String basename, List<String> types) throws IOException {
       String path = outputDir + "/" + basename + ".java";
       PrintWriter writer = new PrintWriter(path, "UTF-8");

       writer.println("package com.craftinginterpreters.lox;");
       writer.println();
       writer.println("import java.util.List;");
       writer.println();
       writer.println("public abstract class " + basename + "{");

       defineVisitor(writer, basename, types);

       // the base accept() method
       writer.println();
       writer.println("    public abstract <R> R accept(Visitor<R> visitor);");

       for (String type : types) {
           String classname = type.split(":")[0].trim();
           String fields =  type.split(":")[1].trim();
           defineType(writer, basename, classname, fields);
       }

       writer.println("}");
       writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String basename, List<String> types) {
        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        public R visit" + typeName + basename + "(" + typeName + " " + basename.toLowerCase() + ");");
        }

        writer.println("    }");
        writer.println();
    }

    private static void defineType(PrintWriter writer, String basename, String className, String fieldList) {
        // class definition
        writer.println("    static class " + className + " extends " + basename + " {");

        // constructor
        writer.println("        public " + className + "(" + fieldList + ") {");

        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("        this." + name + " = " + name + ";");
        }
        writer.println("        }");

        writer.println();
        writer.println("        @Override");
        writer.println("        public <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + basename + "(this);");
        writer.println("        }");
        writer.println();

        // fields
        writer.println();
        for (String field : fields) {
            writer.println("        final " + field + ";");
        }

        writer.println("    }");
        writer.println();
    }
}
