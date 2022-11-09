package AST.Visitor;

import AST.*;
import TypeNode.*;

import java.util.*;

public class TypeVisitor implements Visitor {

    private HashMap<String, ClassNode> globalTable;

    private MethodNode currentMethod;
    private ClassNode currentClass;

    public void visit(Display n) {

    }

    public HashMap<String, ClassNode> symbolTable() {
        return this.globalTable;
    }

    public void printSymbolTable() {
        Map<String, MethodNode> globalMethods = new HashMap<>();
        for (String className : this.globalTable.keySet()) {
            ClassNode classNode = this.globalTable.get(className);
            String extendsName = "";
            if (classNode instanceof ClassExtendedNode) {
                extendsName = ((ClassExtendedNode) classNode).getExtendsName();
            }
            System.out.print("Class " + className);
            if (!extendsName.equals("")) {
                System.out.print(" extends " + extendsName);
            }
            if (classNode.getFields().size() > 0) {
                System.out.print(" | Fields: ");
                Map<String, Node> fields = classNode.getFields();
                List<String> fieldNames = new ArrayList<>(fields.keySet());
                String currentName = fieldNames.get(0);
                Node currentNode = fields.get(currentName);
                if (currentNode.getType().equals(NodeType.IDENTIFIER)) {
                    System.out.print(currentNode.idType() + " " + currentName);
                } else {
                    System.out.print(currentNode.getType() + " " + currentName);
                }
                for (int i = 1; i < fieldNames.size(); i++) {
                    currentName = fieldNames.get(i);
                    currentNode = fields.get(currentName);
                    if (currentNode.getType().equals(NodeType.IDENTIFIER)) {
                        System.out.print(", " + currentNode.idType() + " " + currentName);
                    } else {
                        System.out.print(", " + currentNode.getType() + " " + currentName);
                    }
                }
            }
            globalMethods.putAll(classNode.getMethods());
            System.out.print(" | Methods: ");
            List<String> methodNames = new ArrayList<>(classNode.getMethods().keySet());
            System.out.print(methodNames.get(0));
            for (int i = 1; i < methodNames.size(); i++) {
                System.out.print(", " + methodNames.get(i));
            }
            System.out.println();
        }
        System.out.println();
        for (String methodName : globalMethods.keySet()) {
            MethodNode methodNode = globalMethods.get(methodName);
            if (methodNode.getReturnType().getType().equals(NodeType.IDENTIFIER)) {
                System.out.print("Method " + methodName + " | Returns " + methodNode.getReturnType().idType());
            } else {
                System.out.print("Method " + methodName + " | Returns " + methodNode.getReturnType().getType());
            }
            if (methodNode.getParameters().size() > 0) {
                System.out.print(" | Parameters: ");
                Map<String, Node> parameters = methodNode.getParameters();
                List<String> paramNames = new ArrayList<>(parameters.keySet());
                String currentName = paramNames.get(0);
                Node currentNode = parameters.get(currentName);
                if (currentNode.getType().equals(NodeType.IDENTIFIER)) {
                    System.out.print(currentNode.idType() + " " + currentName);
                } else {
                    System.out.print(currentNode.getType() + " " + currentName);
                }
                for (int i = 1; i < paramNames.size(); i++) {
                    currentName = paramNames.get(i);
                    currentNode = parameters.get(currentName);
                    if (currentNode.getType().equals(NodeType.IDENTIFIER)) {
                        System.out.print(", " + currentNode.idType() + " " + currentName);
                    } else {
                        System.out.print(", " + currentNode.getType() + " " + currentName);
                    }
                }
            }

            if (methodNode.getLocalVars().size() > 0) {
                System.out.print(" | Local Vars: ");
                Map<String, Node> localVars = methodNode.getLocalVars();
                List<String> varNames = new ArrayList<>(localVars.keySet());
                String currentName = varNames.get(0);
                Node currentNode = localVars.get(currentName);
                if (currentNode.getType().equals(NodeType.IDENTIFIER)) {
                    System.out.print(currentNode.idType() + " " + currentName);
                } else {
                    System.out.print(currentNode.getType() + " " + currentName);
                }
                for (int i = 1; i < varNames.size(); i++) {
                    currentName = varNames.get(i);
                    currentNode = localVars.get(currentName);
                    if (currentNode.getType().equals(NodeType.IDENTIFIER)) {
                        System.out.print(", " + currentNode.idType() + " " + currentName);
                    } else {
                        System.out.print(", " + currentNode.getType() + " " + currentName);
                    }
                }
            }
            System.out.println();
        }
    }

    public void visit(Program n) {
        this.globalTable = new HashMap<>();
        // n.m.accept(this);
        for (int i = 0; i < n.cl.size(); i++) {
            n.cl.get(i).accept(this);
        }
        printSymbolTable();
    }

    public void visit(MainClass n) {

    }

    public void visit(ClassDeclSimple n) {
        this.globalTable.put(n.i.s, new ClassNode(n.i.s));
        this.currentClass = this.globalTable.get(n.i.s);
        for (int i = 0; i < n.vl.size(); i++) {
            n.vl.get(i).accept(this);
        }

        for (int i = 0; i < n.ml.size(); i++) {
            n.ml.get(i).accept(this);
        }
        this.currentClass = null;
    }

    public void visit(ClassDeclExtends n) {
        this.globalTable.put(n.i.s, new ClassExtendedNode(n.i.s, n.j.s));
        this.currentClass = this.globalTable.get(n.i.s);
        for (int i = 0; i < n.vl.size(); i++) {
            n.vl.get(i).accept(this);
        }

        for (int i = 0; i < n.ml.size(); i++) {
            n.ml.get(i).accept(this);
        }
        this.currentClass = null;
    }

    public void visit(VarDecl n) {
        Node current = new Node(getType(n.t));
        if (n.t instanceof IdentifierType) {
            current.idType = ((IdentifierType)n.t).s;
        }

        if (this.currentMethod != null) {
            this.currentMethod.getLocalVars().put(n.i.s, current);
        } else {
            this.currentClass.getFields().put(n.i.s, current);
        }
    }

    public void visit(MethodDecl n) {
        Node returnType = new Node(getType(n.t));
        if (n.t instanceof IdentifierType) {
            returnType.idType = ((IdentifierType)n.t).s;
        }
        this.currentMethod = new MethodNode(returnType);
        this.currentClass.getMethods().put(n.i.s, this.currentMethod);

        for (int i = 0; i < n.fl.size(); i++) {
            n.fl.get(i).accept(this);
        }

        for (int i = 0; i < n.vl.size(); i++) {
            n.vl.get(i).accept(this);
        }

        this.currentMethod = null;
    }

    public void visit(Formal n) {
        Node current = new Node(getType(n.t));

        if (n.t instanceof IdentifierType) {
            current.idType = ((IdentifierType)n.t).s;
        }

        this.currentMethod.getParameters().put(n.i.s, current);
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

    }

    public void visit(Assign n) {

    }

    public void visit(ArrayAssign n) {

    }

    public void visit(And n) {

    }

    public void visit(LessThan n) {

    }

    public void visit(Plus n) {

    }

    public void visit(Minus n) {

    }

    public void visit(Times n) {

    }

    public void visit(ArrayLookup n) {

    }

    public void visit(ArrayLength n) {

    }

    public void visit(Call n) {

    }

    public void visit(IntegerLiteral n) {

    }

    public void visit(True n) {

    }

    public void visit(False n) {

    }

    public void visit(IdentifierExp n) {

    }

    public void visit(This n) {

    }

    public void visit(NewArray n) {

    }

    public void visit(NewObject n) {

    }

    public void visit(Not n) {

    }

    public void visit(Identifier n) {

    }

    public NodeType getType(Type t) {
        if (t instanceof IntegerType) {
            return NodeType.INTEGER;
        } else if (t instanceof BooleanType) {
            return NodeType.BOOLEAN;
        } else if (t instanceof IdentifierType) {
            return NodeType.IDENTIFIER;
        } else if (t instanceof IntArrayType) {
            return NodeType.ARRAY;
        } else {
            return NodeType.UNKNOWN;
        }
    }
}
