package AST.Visitor;

import AST.*;
import java.util.*;

import CodeGeneration.Dependency;
import CodeGeneration.Method;
import TypeNode.*;

public class CodeGenerationVisitor implements Visitor{
    private List<String> code;
    private List<String> data;

    private String methodClass;
    private String methodName;

    private Map<String, List<Method>> vtables;
    //private Map<String, Map<String, Integer>> vTable; // Maps class name -> map (methods, method offsets)
    private Map<String, Integer> objectSizes;
    private Map<String, Map<String, Integer>> methodVariableOffsets;
    private Map<String, Map<String, Integer>> classVariableOffsets;
    private Map<String, Map<String, String>> parameterRegisters;
    private List<String> registers;

    private int labels;
    private int frameSize;
    private int currentSize;
    private TypeVisitor tv;

    public CodeGenerationVisitor(TypeVisitor t) {
        this.tv = t;
        this.frameSize = 1;
        this.labels = 1;
        this.code = new ArrayList<>();
        this.data = new ArrayList<>();
        this.vtables = new HashMap<>();
        this.objectSizes = new HashMap<>();
        this.methodVariableOffsets = new HashMap<>();
        this.classVariableOffsets = new HashMap<>();
        this.parameterRegisters = new HashMap<>();
        this.registers = new ArrayList<>(Arrays.asList("%rsi", "%rcx", "%rdx", "%r8", "%r9"));
    }

    public List<String> getCode() {
        return this.code;
    }

    public void generateVTable(Map<String, ClassNode> table) {
        int methodOffset = 1;
        int localOffset = 1;
        int classOffset = 1;

        for (String className : table.keySet()) {
            //this.vTable.put(className, new HashMap<>());
            int objectSize = table.get(className).getFields().size();
            this.objectSizes.put(className, 8 * objectSize);
            this.classVariableOffsets.put(className, new HashMap<>());

            for (String variableName : table.get(className).getFields().keySet()) {
                this.classVariableOffsets.get(className).put(variableName, classOffset * 8);
                classOffset += 1;
            }

            Map<String, MethodNode> methods = table.get(className).getMethods();
            for (String method : methods.keySet()) {
                this.vtables.get(className).get(methodOffset - 1).setOffset(methodOffset * 8);
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

    public void vtables(Map<String, ClassNode> symbolTable, Map<String, Dependency> dependencyGraph) {
        for (String symbol : symbolTable.keySet()) {
            vtable(symbol, dependencyGraph);
        }
    }

    public List<Method> vtable(String classIdentifier, Map<String, Dependency> dependencyGraph) {
        List<Method> vtable = new ArrayList<>();

        if (dependencyGraph.get(classIdentifier).dependencies != null) {
            String dependencyIdentifier = dependencyGraph.get(classIdentifier).dependencies;

            if (this.vtables.containsKey(dependencyIdentifier)) {
                for (Method method : this.vtables.get(dependencyIdentifier)) {
                    vtable.add(method);
                }
            } else {
                for (Method method : vtable(dependencyIdentifier, dependencyGraph)) {
                    vtable.add(method);
                }
            }
        }

        for (String methodIdentifier : dependencyGraph.get(classIdentifier).methods) {
            boolean hasMethod = false;
            Method existingMethod = null;

            for (Method method : vtable) {
                if (methodIdentifier.equals(method.identifier)) {
                    existingMethod = method;
                    hasMethod = true;
                }
            }

            if (!hasMethod) {
                vtable.add(new Method(methodIdentifier, classIdentifier));
            } else {
                int index = vtable.indexOf(existingMethod);
                vtable.add(index, new Method(methodIdentifier, classIdentifier));
                vtable.remove(existingMethod);
            }
        }

        if (!this.vtables.containsKey(classIdentifier)) {
            this.vtables.put(classIdentifier, vtable);
        }

        return vtable;
    }


    public void visit(Display n) {

    }

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

        for (int i = 0; i < vtables.get(n.i.s).size(); i++) {
            Method method = vtables.get(n.i.s).get(i);
            String important = ".quad " + method.implementorIdentifier + "$" + method.identifier;
            this.data.add(addSpace(important, space));
        }

        this.data.add("");
    }

    public void visit(VarDecl n) {

    }

    public void visit(MethodDecl n) {
        this.parameterRegisters.put(n.i.s, new HashMap<>());
        this.methodName = n.i.s;
        gen(this.methodClass + "$" + n.i.s + ":");
        push("%rbp");
        gen("movq", "%rsp", "%rbp");
        gen("subq", "$" + this.frameSize * 16, "%rsp");
        this.currentSize = this.frameSize;
        this.frameSize += 1;
        for (int i = 0; i < n.fl.size(); i++) {
            this.parameterRegisters.get(n.i.s).put(n.fl.get(i).i.s, this.registers.get(i));
        }
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
        String falseLabel = getLabel();
        String endLabel = getLabel();

        n.e.accept(this);
        gen("cmpq", "$0", "%rax");
        gen("    je      " + falseLabel);

        n.s1.accept(this);
        jump(endLabel);

        label(falseLabel);
        n.s2.accept(this);
        label(endLabel);
    }

    public void visit(While n) {
        String testLabel = getLabel();
        String bodyLabel = getLabel();
        jump(testLabel);
        label(bodyLabel);
        n.s.accept(this);
        label(testLabel);
        n.e.accept(this);
        gen("cmpq", "$0", "%rax");
        gen("    jne     " + bodyLabel);
    }

    public void visit(Print n) {
        n.e.accept(this);
        gen("movq", "%rax", "%rdi");
        gen("    call    put");
    }

    public void visit(Assign n) {
        n.e.accept(this);
        int offset;
        gen("    # " + n.i.s);
        if (this.classVariableOffsets.get(this.methodClass).containsKey(n.i.s)) {
            offset = this.classVariableOffsets.get(this.methodClass).get(n.i.s);
            gen("movq", "%rax", ((8 * this.currentSize) + offset) + "(%rdi)"); // Class Var
        } else if (this.methodVariableOffsets.get(this.methodName).containsKey(n.i.s)){
            offset = this.methodVariableOffsets.get(this.methodName).get(n.i.s);
            //gen("movq", "%rax", ((-8 * this.currentSize) - offset) + "(%rbp)"); // Local Var
            gen("movq", "%rax", (-1 * offset) + "(%rbp)");
        } else {
            gen("movq", "%rax", this.parameterRegisters.get(this.methodName).get(n.i.s));
        }
    }

    public void visit(ArrayAssign n) {

    }

    public void visit(And n) {
        String labelFalse = getLabel();
        String labelEnd = getLabel();

        n.e1.accept(this);
        gen("cmpq", "$0", "%rax");
        gen("    je      " + labelFalse);

        n.e2.accept(this); // rax = b?
        gen("cmpq", "$0", "%rax");
        gen("    je      " + labelFalse);

        gen("movq", "$1", "%rax");
        jump(labelEnd);

        label(labelFalse);
        gen("movq", "$0", "%rax");
        label(labelEnd);
    }

    public void visit(LessThan n) {
        String labelTrue = getLabel();
        String labelFalse = getLabel();

        n.e1.accept(this);
        push("%rax");
        n.e2.accept(this); // rax = b?
        pop("%rdx"); // rdx = a?

        gen("cmpq", "%rax", "%rdx");
        gen("    jl      " + labelTrue);
        gen("movq", "$0", "%rax");
        jump(labelFalse);
        label(labelTrue);
        gen("movq", "$1", "%rax");
        label(labelFalse);
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
        n.e1.accept(this);
        push("%rax");
        n.e2.accept(this);
        pop("%rdx");
        gen("movq", "8(%rdx,%rax,8)", "%rax");
    }

    public void visit(ArrayLength n) {
        n.e.accept(this);
        gen("movq", "(%rax)", "%rax");
    }

    public void visit(Call n) {
        n.e.accept(this); // leave the pointer in %rax
        Stack<String> toPop = new Stack<>();
        push("%rdi"); // Save current rdi value
        gen("movq", "%rax", "%rdi"); // "this" pointer is first argument
        for (int i = 0; i < n.el.size(); i++) { // 2 parameters
            push(this.registers.get(i));
            toPop.push(this.registers.get(i));
            n.el.get(i).accept(this); // Puts it into rax
            gen("movq", "%rax", this.registers.get(i)); // fill in parameters
        }
        gen("movq", "0(%rdi)", "%rax");
        int offset = 0;
        for (int i = 0; i < this.vtables.get(this.methodClass).size(); i++) {
            if (this.vtables.get(this.methodClass).get(i).identifier.equals(n.i.s)) {
                offset = this.vtables.get(this.methodClass).get(i).offset;
                break;
            }
        }
        gen("movq", offset + "(%rax)", "%rax");
        gen("    call    *%rax");
        for (int i = 0; i < n.el.size(); i++) {
            pop(toPop.pop());
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
        gen("    # " + n.s);
        if (this.classVariableOffsets.get(this.methodClass).containsKey(n.s)) {
            offset = this.classVariableOffsets.get(this.methodClass).get(n.s);
            gen("movq", ((8 * this.currentSize) + offset) + "(%rdi)", "%rax");
        } else if (this.methodVariableOffsets.get(this.methodName).containsKey(n.s)) {
            offset = this.methodVariableOffsets.get(this.methodName).get(n.s);
            //gen("movq", ((-8 * this.currentSize) - offset) + "(%rbp)", "%rax"); // Local Var
            gen("movq", (-1 * offset) + "(%rbp)", "%rax");
        } else {
            gen("movq", this.parameterRegisters.get(this.methodName).get(n.s), "%rax");
        }
    }

    public void visit(This n) {

    }

    public void visit(NewArray n) {
        n.e.accept(this);
        push("%rax");
        // # of elements is initially stored in %rax, but we need space for n+1 elements
        // (the first is used to store the size of the array)
        gen("    incq    %rax");

        // need 8 bytes per element
        gen("shlq", "$2", "%rax");
        push("%rax");

        // allocate the space, addr of bytes returned in %rax
        gen("    call    mjcalloc");

        // move the size to the first pos
        pop("%rdx");
        gen("movq",  "%rdx", "(%rax)");
    }

    public void visit(NewObject n) {//store this
        push("%rdi");
        gen("movq", "$"  + (8 + this.objectSizes.get(n.i.s)), "%rdi");
        gen("    call    mjcalloc"); // addr of allocated bytes returned to %rax
        gen("leaq", n.i.s + "$$(%rip)", "%rdx");
        gen("movq", "%rdx", "(%rax)");
        pop("%rdi");
        this.methodClass = n.i.s;
    }

    public void visit(Not n) {
        String realTrue = getLabel();
        String realFalse = getLabel();
        n.e.accept(this);
        gen("cmpq", "$0", "%rax");
        gen("    je      " + realTrue);
        gen("movq", "$0", "%rax");
        jump(realFalse);
        label(realTrue);
        gen("movq", "$1", "%rax");
        label(realFalse);
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

    public void jump(String label) {
        this.code.add("    jmp     " + label);
    }

    public void label(String label) {
        this.code.add(label + ":");
    }

    public String getLabel() {
        labels += 1;
        return "L" + (labels - 1);
    }
}
