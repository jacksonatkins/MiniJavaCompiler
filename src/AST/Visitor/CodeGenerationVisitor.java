package AST.Visitor;

import AST.*;

import java.util.*;

import CodeGeneration.Dependency;
import CodeGeneration.Method;
import TypeNode.*;

public class CodeGenerationVisitor implements Visitor {
    private List<String> code;
    private List<String> data;

    private String currentClass;
    private String currentMethod;
    private String lastType;

    private Map<String, List<Method>> vtables;
    private Map<String, Integer> sizeMap;
    private Map<String, Integer> objectSizes;
    private Map<String, Map<String, Integer>> methodVariableOffsets;
    private Map<String, Map<String, Integer>> classVariableOffsets;
    private Map<String, Map<String, String>> parameterRegisters;
    private List<String> registers;

    private int labels;
    private TypeVisitor tv;

    public CodeGenerationVisitor(TypeVisitor t) {
        this.tv = t;
        this.labels = 1;
        this.code = new ArrayList<>();
        this.data = new ArrayList<>();
        this.vtables = new HashMap<>();
        this.sizeMap = new HashMap<>();
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
        int localOffset = 0;
        int classOffset = 0;

        for (String className : table.keySet()) {
            ClassNode parent = null;
            if (table.get(className) instanceof ClassExtendedNode) {
                ClassExtendedNode child = (ClassExtendedNode) table.get(className);
                parent = table.get(child.getExtendsName());
            }
            int objectSize = sizeMap.get(className);
            objectSize = parent != null ? objectSize + table.get(parent.name).getFields().size() : objectSize;
            this.objectSizes.put(className, 8 + 8 * objectSize);
            this.classVariableOffsets.put(className, new HashMap<>());

            // if extended class, add parent instance variables and methods
            if (parent != null) {
                for (String parentVar : table.get(parent.name).getFields().keySet()) {
                    this.classVariableOffsets.get(className).put(parentVar, classOffset * 8);
                    classOffset += 1;
                }

                Map<String, MethodNode> inherited = table.get(parent.name).getMethods();
                for (String method : inherited.keySet()) {
                    this.vtables.get(className).get(methodOffset - 1).setOffset(methodOffset * 8);
                    if (!this.methodVariableOffsets.containsKey(method)) {
                        this.methodVariableOffsets.put(method, new HashMap<>());
                        Map<String, Node> localVars = inherited.get(method).getLocalVars();
                        for (String var : localVars.keySet()) {
                            this.methodVariableOffsets.get(method).put(var, localOffset * 8);
                            localOffset += 1;
                        }
                    }
                    localOffset = 0;
                    if (!table.get(className).getMethods().containsKey(method)) {
                        methodOffset += 1;
                    }
                }
            }

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
                localOffset = 0;
                methodOffset += 1;
            }
            methodOffset = 1;
            classOffset = 0;
        }
    }

    public void vtables(Map<String, ClassNode> symbolTable, Map<String, Dependency> dependencyGraph) {
        for (String symbol : symbolTable.keySet()) {
            vtable(symbol, dependencyGraph);
            sizeMap.put(symbol, classSize(symbol, dependencyGraph));
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

    private int classSize(String classIdentifier, Map<String, Dependency> dependencyGraph) {
        if (classIdentifier == null) {
            return 0;
        }

        return dependencyGraph.get(classIdentifier).fields.size() + classSize(dependencyGraph.get(classIdentifier).dependencies, dependencyGraph);
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
        this.currentClass = n.i.s;
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
        this.currentClass = n.i.s;
        String className = String.format("%s$$:", n.i.s);
        int space = className.length() + 2;
        this.data.add(className + "  .quad " + n.j.s + "$$");

        int idx = 0;
        for (int i = 0; i < vtables.get(n.i.s).size(); i++) {
            Method method = vtables.get(n.i.s).get(i);
            String important = ".quad " + method.implementorIdentifier + "$" + method.identifier;
            this.data.add(addSpace(important, space));

            if (method.implementorIdentifier.equals(n.i.s)) {
                n.ml.get(idx).accept(this);
                idx += 1;
            }
        }
        this.data.add("");
    }

    public void visit(VarDecl n) {

    }

    public void visit(MethodDecl n) {
        this.parameterRegisters.put(n.i.s, new HashMap<>());
        this.currentMethod = n.i.s;
        gen(this.currentClass + "$" + n.i.s + ":");
        push("%rbp");
        gen("movq", "%rsp", "%rbp");
        int size = 8 * this.methodVariableOffsets.get(this.currentMethod).size();
        gen("subq", "$" + size, "%rsp");
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
        push("%rdi");
        push("%rsi");
        gen("movq", "%rax", "%rdi");
        gen("    call    put");
        pop("%rsi");
        pop("%rdi");
    }

    public void visit(Assign n) {
        n.e.accept(this);
        int offset;
        gen("    # " + n.i.s);
        if (this.classVariableOffsets.get(this.currentClass).containsKey(n.i.s)) {
            offset = this.classVariableOffsets.get(this.currentClass).get(n.i.s);
            gen("movq", "%rax", (8 + offset) + "(%rdi)"); // Class Var
        } else if (this.methodVariableOffsets.get(this.currentMethod).containsKey(n.i.s)) {
            offset = this.methodVariableOffsets.get(this.currentMethod).get(n.i.s);
            gen("movq", "%rax", (-8 - offset) + "(%rbp)");
        } else {
            gen("movq", "%rax", this.parameterRegisters.get(this.currentMethod).get(n.i.s));
        }
    }

    public void visit(ArrayAssign n) { // i[e1] = e2;
        n.e1.accept(this); // Index
        push("%rax");
        n.e2.accept(this); // Value
        pop("%rdx"); // rdx = e1, rax = e2
        int offset;
        gen("    # " + n.i.s);
        if (this.classVariableOffsets.get(this.currentClass).containsKey(n.i.s)) {
            offset = this.classVariableOffsets.get(this.currentClass).get(n.i.s);
            push("%rcx");
            push("%rdi");
            push("%rsi");
            gen("movq", (8 + offset) + "(%rdi)", "%rcx");
            gen("movq", "%rdx", "%rdi");
            gen("movq", "0(%rcx)", "%rsi");
            gen("    call    arrayCheck");
            pop("%rsi");
            pop("%rdi");
            // (rcx, rdx, 8) is address of the element!
            gen("movq", "%rax", "8(%rcx,%rdx,8)");
            pop("%rcx");
        } else if (this.methodVariableOffsets.get(this.currentMethod).containsKey(n.i.s)) {
            offset = this.methodVariableOffsets.get(this.currentMethod).get(n.i.s);
            push("%rcx");
            push("%rdi");
            push("%rsi");
            gen("movq", (-8 - offset) + "(%rbp)", "%rcx"); // gets address of array
            gen("movq", "%rdx", "%rdi");
            gen("movq", "0(%rcx)", "%rsi");
            gen("    call    arrayCheck");
            pop("%rsi");
            pop("%rdi");
            // (rcx, rdx, 8) is address of the element!
            gen("movq", "%rax", "8(%rcx,%rdx,8)");
            pop("%rcx");
        } else {
            gen("movq", "%rax", "8(" + this.parameterRegisters.get(this.currentMethod).get(n.i.s) + ",%rdx,8)");
        }
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
        pop("%rdx"); // rdx = i, rax = addr of array
        push("%rdi");
        push("%rsi");
        gen("movq", "0(%rdx)", "%rdi");
        gen("movq", "%rax", "%rsi");
        gen("    call    arrayCheck");
        pop("%rsi");
        pop("%rdi");
        gen("movq", "8(%rdx,%rax,8)", "%rax");
    }

    public void visit(ArrayLength n) {
        n.e.accept(this);
        gen("movq", "(%rax)", "%rax");
    }

    public void visit(Call n) {
        n.e.accept(this); // leave the pointer in %rax
        if (n.e instanceof NewObject) {
            this.currentClass = this.lastType;
        }
        String savedClass = this.currentClass;
        String savedType = this.lastType;
        Stack<String> popEL = new Stack<>();
        Stack<String> popEnd = new Stack<>();
        push("%rdi"); // Save current rdi value
        for (int i = 0; i < n.el.size(); i++) {
            popEnd.push(this.registers.get(i));
            push(this.registers.get(i));
        }
        gen("movq", "%rax", "%rdi"); // "this" pointer is first argument
        for (int i = 0; i < n.el.size(); i++) { // 2 parameters
            popEL.push(this.registers.get(i));
            n.el.get(i).accept(this); // Puts it into rax
            push("%rax");
        }
        this.lastType = savedType;
        this.currentClass = this.lastType;
        gen("movq", "0(%rdi)", "%rax");
        int offset = 0;
        for (int i = 0; i < this.vtables.get(this.currentClass).size(); i++) {
            if (this.vtables.get(this.currentClass).get(i).identifier.equals(n.i.s)) {
                offset = this.vtables.get(this.currentClass).get(i).offset;
                break;
            }
        }
        this.currentClass = savedClass;
        gen("movq", offset + "(%rax)", "%rax");
        for (int i = 0; i < n.el.size(); i++) {
            pop(popEL.pop());
        }
        gen("    call    *%rax");
        for (int i = 0; i < n.el.size(); i++) {
            pop(popEnd.pop());
        }
        pop("%rdi");
        this.currentClass = savedClass;
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
        if (this.classVariableOffsets.get(this.currentClass).containsKey(n.s)) {
            offset = this.classVariableOffsets.get(this.currentClass).get(n.s);
            gen("movq", (8 + offset) + "(%rdi)", "%rax");
            Node variable = tv.symbolTable().get(this.currentClass).getFields().get(n.s);
            if (variable != null && variable.idType() != null) {
                this.lastType = variable.idType();
            }
        } else if (this.methodVariableOffsets.get(this.currentMethod).containsKey(n.s)) {
            offset = this.methodVariableOffsets.get(this.currentMethod).get(n.s);
            gen("movq", (-8 - offset) + "(%rbp)", "%rax");
            Node variable = tv.symbolTable().get(this.currentClass).getMethods().get(this.currentMethod).getLocalVars().get(n.s);
            if (variable.idType() != null) {
                this.lastType = variable.idType();
            }
        } else {
            gen("movq", this.parameterRegisters.get(this.currentMethod).get(n.s), "%rax");
            Node node = tv.symbolTable().get(this.currentClass).getMethods().get(this.currentMethod).getParameters().get(n.s);
            if (node.idType() != null) {
                this.lastType = node.idType();
            }
        }
    }

    public void visit(This n) {
        gen("movq", "%rdi", "%rax");
    }

    public void visit(NewArray n) {
        n.e.accept(this);
        push("%rax");
        // # of elements is initially stored in %rax, but we need space for n+1 elements
        // (the first is used to store the size of the array)
        gen("    incq    %rax");

        // need 8 bytes per element
        gen("shlq", "$3", "%rax");
        push("%rdi");
        gen("movq", "%rax", "%rdi");
        // allocate the space, addr of bytes returned in %rax
        gen("    call    mjcalloc");
        pop("%rdi");
        pop("%rdx");
        gen("movq", "%rdx", "0(%rax)");
    }

    public void visit(NewObject n) {//store this
        push("%rdi");
        gen("movq", "$" + (this.objectSizes.get(n.i.s)), "%rdi");
        gen("    call    mjcalloc"); // addr of allocated bytes returned to %rax
        gen("leaq", n.i.s + "$$(%rip)", "%rdx");
        gen("movq", "%rdx", "(%rax)");
        pop("%rdi");
        this.lastType = n.i.s;
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
