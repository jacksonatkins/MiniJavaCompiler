package AST.Visitor;

import AST.*;
import java.util.*;

import CodeGeneration.Dependency;
import TypeNode.*;

public class CodeGenerationVisitor implements Visitor{
    private List<String> code;
    private List<String> data;
    private String methodClass;
    private String methodName;
    private Map<String, List<String>> vtables;
    //private Map<String, Map<String, Integer>> vTable; // Maps class name -> map (methods, method offsets)
    private Map<String, Integer> objectSizes;
    private Map<String, Map<String, Integer>> methodVariableOffsets;
    private int frameSize;
    private int currentSize;
    private TypeVisitor tv;

    public CodeGenerationVisitor(TypeVisitor t) {
        this.tv = t;
        this.frameSize = 1;
        this.code = new ArrayList<>();
        this.data = new ArrayList<>();
        this.vtables = new HashMap<>();
        //this.vTable = new HashMap<>();
        this.objectSizes = new HashMap<>();
        this.methodVariableOffsets = new HashMap<>();
    }

    public List<String> getCode() {
        return this.code;
    }

    public void generateVTable(Map<String, ClassNode> table) {
        int methodOffset = 1;
        int localOffset = 1;

        for (String className : table.keySet()) {
            //this.vTable.put(className, new HashMap<>());
            int objectSize = table.get(className).getFields().size();
            this.objectSizes.put(className, 8 + objectSize);

            Map<String, MethodNode> methods = table.get(className).getMethods();
            for (String method : methods.keySet()) {
                //this.vTable.get(className).put(method, methodOffset * 8);
                this.methodVariableOffsets.put(method, new HashMap<>());

                Map<String, Node> localVars = methods.get(method).getLocalVars();
                for (String var : localVars.keySet()) {
                    this.methodVariableOffsets.get(method).put(var, localOffset * 8);
                    localOffset += 1;
                }
                methodOffset += 1;
            }
        }
    }

    public void vtables(Map<String, ClassNode> symbolTable, Map<String, Dependency> dependencyGraph) {
        for (String symbol : symbolTable.keySet()) {
            vtable(symbol, dependencyGraph);
        }
    }

    public List<String> vtable(String classIdentifier, Map<String, Dependency> dependencyGraph) {
        List<String> vtable = new ArrayList<>();

        if (dependencyGraph.get(classIdentifier).dependencies != null) {
            String dependencyIdentifier = dependencyGraph.get(classIdentifier).dependencies;

            if (this.vtables.containsKey(dependencyIdentifier)) {
                for (String methodIdentifier : this.vtables.get(dependencyIdentifier)) {
                    vtable.add(methodIdentifier);
                }
            } else {
                for (String methodIdentifier : vtable(dependencyIdentifier, dependencyGraph)) {
                    vtable.add(methodIdentifier);
                }
            }
        }

        for (String methodIdentifier : dependencyGraph.get(classIdentifier).methods) {
            vtable.add(methodIdentifier);
        }

        if (!this.vtables.containsKey(classIdentifier)) {
            this.vtables.put(classIdentifier, vtable);
        }

        return vtable;
    }

    public void visit(Display n) { }

    public void visit(Program n) {
        DependencyVisitor dependencyVisitor = new DependencyVisitor();
        n.accept(dependencyVisitor);

        n.accept(tv);
        vtables(tv.symbolTable(), dependencyVisitor.dependencyGraph);
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
        //this.vTable.put(n.i.s, new HashMap<>()); // Add class to vTable
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
        int offset = this.methodVariableOffsets.get(this.methodName).get(n.i.s);
        gen("movq", "%rax", ((-16 * this.currentSize) - offset) + "(%rbp)"); // -24(%rbp), 8 = offset, -16 = method thang
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
        // push("%rsi");
        // push("%rdx");
        // push("%rcx");
        // push("%r8");
        // push("%r9");
        List<String> registers = new ArrayList<>(Arrays.asList("%rsi", "%rdx", "%rcx", "%r8", "%r9"));
        gen("movq", "%rax", "%rdi"); // "this" pointer is first argument
        for (int i = 0; i < n.el.size(); i++) {
            n.el.get(i).accept(this);
            gen("movq", "%rax", registers.get(i)); // fill in parameters
        }
        gen("movq", "0(%rdi)", "%rax");
        gen("addq", "$" + 8 * (this.vtables.get(this.methodClass).indexOf(n.i.s) + 1), "%rax");
        gen("movq", "(%rax)", "%rax");
        gen("    call    *%rax");
        // put stuff back to registers
        // pop("%r9");
        // pop("%r8");
        // pop("%rcx");
        // pop("%rdx");
        // pop("%rsi");
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
        int offset = this.methodVariableOffsets.get(this.methodName).get(n.s);
        gen("movq", ((-16 * this.currentSize) - offset) + "(%rbp)", "%rax");
    }

    public void visit(This n) {

    }

    public void visit(NewArray n) {

    }

    public void visit(NewObject n) {//store this
        push("%rdi");
        gen("movq", "$8", "%rdi");
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
