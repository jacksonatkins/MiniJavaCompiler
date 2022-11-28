package AST.Visitor;

import AST.*;
import java.util.*;
import TypeNode.*;

public class CodeGenerationVisitor implements Visitor{
    private List<String> code;
    private List<String> data;

    private String methodClass;
    private String methodName;

    private Map<String, Map<String, Integer>> vTable; // Maps class name -> map (methods, method offsets)
    private Map<String, Integer> objectSizes;
    private Map<String, Map<String, Integer>> methodVariableOffsets;
    private Map<String, Map<String, Integer>> classVariableOffsets;
    private List<String> pushed;
    private List<String> registers;

    private int index;
    private int frameSize;
    private int currentSize;
    private TypeVisitor tv;

    public CodeGenerationVisitor(TypeVisitor t) {
        this.tv = t;
        this.frameSize = 1;
        this.code = new ArrayList<>();
        this.data = new ArrayList<>();
        this.vTable = new HashMap<>();
        this.objectSizes = new HashMap<>();
        this.methodVariableOffsets = new HashMap<>();
        this.classVariableOffsets = new HashMap<>();
        this.registers = new ArrayList<>(Arrays.asList("%rsi", "%rdx", "%rcx", "%r8", "%r9"));
    }

    public List<String> getCode() {
        return this.code;
    }

    public void generateVTable(Map<String, ClassNode> table) {
        int methodOffset = 1;
        int localOffset = 1;
        int classOffset = 1;

        for (String className : table.keySet()) {
            this.vTable.put(className, new HashMap<>());
            int objectSize = table.get(className).getFields().size();
            this.objectSizes.put(className, 8 + 8 * objectSize);
            this.classVariableOffsets.put(className, new HashMap<>());

            for (String variableName : table.get(className).getFields().keySet()) {
                this.classVariableOffsets.get(className).put(variableName, classOffset * 8);
                classOffset += 1;
            }

            Map<String, MethodNode> methods = table.get(className).getMethods();
            for (String method : methods.keySet()) {
                this.vTable.get(className).put(method, methodOffset * 8);
                this.methodVariableOffsets.put(method, new HashMap<>());

                Map<String, Node> localVars = methods.get(method).getLocalVars();
                for (String var : localVars.keySet()) {
                    this.methodVariableOffsets.get(method).put(var, localOffset * 8);
                    localOffset += 1;
                }
                localOffset = 1;
                methodOffset += 1;
            }
            methodOffset = 1;
            classOffset = 1;
        }
    }

    public void visit(Display n) {

    }

    public void visit(Program n) {
        n.accept(tv);
        generateVTable(tv.symbolTable());
        n.m.accept(this);
        gen("");

        // Starting the method table stuff
        this.data.add("    .data");
        this.data.add("");

        for (int i = 0; i < n.cl.size(); i++) {
            n.cl.get(i).accept(this);
        }
        // When everything visited, add the .data to bottom of output
        this.code.addAll(this.data);
    }

    public void visit(MainClass n) {
        // Prologue
        gen("    .text");
        gen("    .globl asm_main");
        gen("");
        gen("asm_main:");
        push("%rbp");
        gen("movq", "%rsp", "%rbp");

        n.s.accept(this);

        // Epilogue
        gen("movq", "%rbp", "%rsp");
        pop("%rbp");
        gen("    ret");
    }

    public String addSpace(String original, int spaces) {
        StringBuilder originalBuilder = new StringBuilder(original);
        for (int i = 0; i < spaces; i++) {
            originalBuilder.insert(0, " ");
        }
        original = originalBuilder.toString();
        return original;
    }

    public void visit(ClassDeclSimple n) {
        this.methodClass = n.i.s;
        this.vTable.put(n.i.s, new HashMap<>()); // Add class to vTable
        // String formatting for the method vtable
        String className = String.format("%s$$:", n.i.s);
        int space = className.length() + 2;
        this.data.add(className + "  .quad 0");

        for (int i = 0; i < n.ml.size(); i++) {
            // Adds method to the vtable
            String important = ".quad " + n.i.s + "$" + n.ml.get(i).i.s;
            this.data.add(addSpace(important, space));

            // Visits method
            n.ml.get(i).accept(this);
        }
        this.data.add("");
    }

    public void visit(ClassDeclExtends n) {
        this.methodClass = n.i.s;

        String className = String.format("%s$$", n.i.s);
        int space = className.length() + 2;
        this.data.add(className + "  .quad " + n.j.s + "$$");

        this.data.add("");
    }

    public void visit(VarDecl n) {

    }

    public void visit(MethodDecl n) {
        this.index = 0;
        this.methodName = n.i.s;
        gen(this.methodClass + "$" + n.i.s + ":");
        push("%rbp");
        gen("movq", "%rsp", "%rbp");
        gen("subq", "$" + this.frameSize * 16, "%rsp");
        this.currentSize = this.frameSize;
        this.frameSize += 1;
        for (int i = 0; i < n.sl.size(); i++) {
            n.sl.get(i).accept(this);
        }
        n.e.accept(this);

        gen("movq", "%rbp", "%rsp");
        pop("%rbp");
        gen("    ret");
        gen("");
    }

    public void visit(Formal n) {

    }

    public void visit(IntArrayType n) {

    }

    public void visit(BooleanType n) {

    }

    public void visit(IntegerType n) {

    }

    public void visit(IdentifierType n) {

    }

    public void visit(Block n) {
        for (int i = 0; i < n.sl.size(); i++) {
            n.sl.get(i).accept(this);
        }
    }

    public void visit(If n) {

    }

    public void visit(While n) {

    }

    public void visit(Print n) {
        n.e.accept(this);
        gen("movq", "%rax", "%rdi");
        gen("    call    put");
    }

    public void visit(Assign n) {
        n.e.accept(this);
        if (this.classVariableOffsets.get(this.methodClass).containsKey(n.i.s)) {
            int offset = this.classVariableOffsets.get(this.methodClass).get(n.i.s);
            gen("movq", "%rax", ((8 * this.currentSize) + offset) + "(%rbp)"); // Class Var
        } else if (this.methodVariableOffsets.get(this.methodName).containsKey(n.i.s)){
            int offset = this.methodVariableOffsets.get(this.methodName).get(n.i.s);
            gen("movq", "%rax", ((-8 * this.currentSize) - offset) + "(%rbp)"); // Local Var
        }
    }

    public void visit(ArrayAssign n) {

    }

    public void visit(And n) {

    }

    public void visit(LessThan n) {

    }

    public void visit(Plus n) {
        n.e1.accept(this);
        push("%rax");
        n.e2.accept(this);
        pop("%rdx");
        gen("addq", "%rdx", "%rax");
    }

    public void visit(Minus n) {
        n.e1.accept(this);
        push("%rax");
        n.e2.accept(this);
        gen("movq", "%rax", "%rdx");
        pop("%rax");
        gen("subq", "%rdx", "%rax");
    }

    public void visit(Times n) {
        n.e1.accept(this);
        push("%rax");
        n.e2.accept(this);
        pop("%rdx");
        gen("imulq", "%rdx", "%rax");
    }

    public void visit(ArrayLookup n) {

    }

    public void visit(ArrayLength n) {

    }

    public void visit(Call n) {
        n.e.accept(this); // leave the pointer in %rax
        push("%rdi"); // Save current rdi value
        this.pushed = new ArrayList<>();
        gen("movq", "%rax", "%rdi"); // "this" pointer is first argument
        for (int i = 0; i < n.el.size(); i++) { // 2 parameters
            push(this.registers.get(i));
            this.pushed.add(this.registers.get(i));
            n.el.get(i).accept(this);
            gen("movq", "%rax", this.registers.get(i)); // fill in parameters
        }
        gen("movq", "0(%rdi)", "%rax");
        gen("addq", "$" + this.vTable.get(this.methodClass).get(n.i.s), "%rax");
        gen("movq", "(%rax)", "%rax");
        gen("    call    *%rax");
        // put stuff back to registers
        for (int i = 0; i < n.el.size(); i++) {
            pop(this.pushed.get(i));
        }
        pop("%rdi");
    }

    public void visit(IntegerLiteral n) {
        gen("movq", "$" + n.i, "%rax");
    }

    public void visit(True n) {
        gen("movq", "$1", "%rax");
    }

    public void visit(False n) {
        gen("movq", "$0", "%rax");
    }

    public void visit(IdentifierExp n) {
        int offset;
        if (this.classVariableOffsets.get(this.methodClass).containsKey(n.s)) {
            offset = this.classVariableOffsets.get(this.methodClass).get(n.s);
            gen("movq", ((8 * this.currentSize) + offset) + "(%rbp)", "%rax");
        } else if (this.methodVariableOffsets.get(this.methodName).containsKey(n.s)) {
            offset = this.methodVariableOffsets.get(this.methodName).get(n.s);
            gen("movq", ((-8 * this.currentSize) - offset) + "(%rbp)", "%rax"); // Local Var
        } else {
            gen("movq", this.registers.get(this.index), "%rax");
            this.index += 1;
        }
    }

    public void visit(This n) {

    }

    public void visit(NewArray n) {

    }

    public void visit(NewObject n) {//store this
        push("%rdi");
        gen("movq", "$"  + (8 + this.objectSizes.get(n.i.s)), "%rdi");
        gen("    call    mjcalloc"); // addr of allocated bytes returned to %rax
        gen("leaq", n.i.s + "$$(%rip)", "%rdx"); // (%rip)
        gen("movq", "%rdx", "(%rax)");
        pop("%rdi");
        this.methodClass = n.i.s;
    }

    public void visit(Not n) {

    }

    public void visit(Identifier n) {

    }

    public void gen(String op, String src, String dest) {
        this.code.add("    " + op + "    " + src + "," + dest);
    }

    public void gen(String s) {
        this.code.add(s);
    }

    public void push(String src) {
        this.code.add("    pushq   " + src);
    }

    public void pop(String dest) {
        this.code.add("    popq    " + dest);
    }
}
