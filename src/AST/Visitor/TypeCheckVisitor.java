package AST.Visitor;

import AST.*;
import TypeNode.*;
import java.util.*;

public class TypeCheckVisitor implements Visitor{

    private HashMap<String, ClassNode> globalTable;
    private TypeVisitor t;

    private Node currentNode;
    private Node parentNode;
    private NodeType currentType;
    private Map<String, Node> currentScope;

    public void visit(Display n) {

    }

    public void visit(Program n) {
        this.t = new TypeVisitor(); // Is this the proper way?
        n.accept(t);
        globalTable = t.symbolTable();
        this.currentScope = new HashMap<>();
        for (int i = 0; i < n.cl.size(); i++) {
            n.cl.get(i).accept(this);
        }
    }

    public void visit(MainClass n) {

    }

    public void visit(ClassDeclSimple n) {
        this.currentNode = globalTable.get(n.i.s);
        if (currentNode == null) {
            System.err.println("(Line " + n.line_number + ") Class " + n.i.s + " not defined.");
        }

        for (int i = 0; i < n.vl.size(); i++) {
            this.currentScope.put(n.vl.get(i).i.s, ((ClassNode)this.currentNode).getFields().get(n.vl.get(i).i.s));
            n.vl.get(i).accept(this);
        }

        this.parentNode = this.currentNode;
        for (int i = 0; i < n.ml.size(); i++) {
            n.ml.get(i).accept(this);
        }

        this.parentNode = null;
        this.currentNode = null;
        this.currentScope = new HashMap<>();
    }

    public void visit(ClassDeclExtends n) {

    }

    public void visit(VarDecl n) {

    }

    public void visit(MethodDecl n) {
        // Does Type t match the Type in the globalTable?
        // Are all of the statements ok?
        // Is the expression e (return expression) the same Type as t?
        if (!(this.parentNode instanceof ClassNode)) {
            // Return an error
        }

        if (!((ClassNode)this.parentNode).getMethods().containsKey(n.i.s)) {
            // Return an error
        }

        this.currentNode = ((ClassNode) this.parentNode).getMethods().get(n.i.s);
        MethodNode currentMethod = ((MethodNode) this.currentNode);
        this.currentScope.putAll(currentMethod.getLocalVars());
        this.currentScope.putAll(currentMethod.getParameters());

        for (int i = 0; i < n.fl.size(); i++) {
            n.fl.get(i).accept(this);
        }

        for (int i = 0; i < n.sl.size(); i++) {
            n.sl.get(i).accept(this);
        }

        for (String s : currentMethod.getLocalVars().keySet()) {
            this.currentScope.remove(s);
        }
        for (String s : currentMethod.getParameters().keySet()) {
            this.currentScope.remove(s);
        }

    }

    public void visit(Formal n) {
        currentType = ((MethodNode) this.currentNode).getParameters().get(n.i.s).getType();
    }

    public void visit(IntArrayType n) {
        currentType = getType(n);
    }

    public void visit(BooleanType n) {
        currentType = getType(n);
    }

    public void visit(IntegerType n) {
        currentType = getType(n);
    }

    public void visit(IdentifierType n) {
        currentType = getType(n);
    }

    public void visit(Block n) {
        for (int i = 0; i < n.sl.size(); i++) {
            n.sl.get(i).accept(this);
        }
    }

    public void visit(If n) {
        n.e.accept(this);
        if (!this.currentType.equals(NodeType.BOOLEAN)) {
            System.err.println("(Line " + n.line_number + ") If statement must contain a boolean type.");
        }
        n.s1.accept(this);
        n.s2.accept(this);
    }

    public void visit(While n) {
        n.e.accept(this);
        if (!this.currentType.equals(NodeType.BOOLEAN)) {
            System.err.println("(Line " + n.line_number + ") While statement must contain a boolean type.");
        }
        n.s.accept(this);
    }

    public void visit(Print n) {
        n.e.accept(this);
    }

    public void visit(Assign n) {
        Node left;
        if (this.currentScope.containsKey(n.i.s)) {
            left = this.currentScope.get(n.i.s);
        } else {
            System.err.println("(Line " + n.line_number + ") Variable " + n.i.s + " not defined in current scope.");
            return;
        }
        n.e.accept(this);
        if (left.getType().equals(NodeType.INTEGER) && !this.currentType.equals(NodeType.INTEGER)) {
            System.err.println("(Line " + n.line_number + ") Cannot assign integer variable to non-integer value.");
            return;
        }
        if (!left.getType().equals(NodeType.INTEGER) && this.currentType.equals(NodeType.INTEGER)) {
            System.err.println("(Line " + n.line_number + ") Cannot assign integer value to non-integer variable.");
            return;
        }
        this.currentType = left.getType();
    }

    public void visit(ArrayAssign n) {

    }

    public void visit(And n) {
         booleanLogic(n.e1, n.e2, n.line_number);
    }

    public void visit(LessThan n) {
        math(n.e1, n.e2, "compare", n.line_number);
    }

    public void booleanLogic(Exp e1, Exp e2, int num) {
        e1.accept(this);
        NodeType left = this.currentType;
        e2.accept(this);
        NodeType right = this.currentType;
        if (!left.equals(NodeType.BOOLEAN) || !right.equals(NodeType.BOOLEAN)) {
            System.err.println("(Line " + num + ") Cannot compare non-boolean variables.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        this.currentType = NodeType.BOOLEAN;
    }

    public void math(Exp e1, Exp e2, String name, int num) {
        e1.accept(this);
        NodeType left = this.currentType;
        e2.accept(this);
        NodeType right = this.currentType;
        if (!left.equals(NodeType.INTEGER) || !right.equals(NodeType.INTEGER)) {
            System.err.println("(Line " + num + ") Cannot " + name + " non-integer values.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        List<String> names = Arrays.asList("add", "multiply", "subtract");
        if (names.contains(name)) {
            this.currentType = NodeType.INTEGER;
        } else {
            this.currentType = NodeType.BOOLEAN;
        }
    }

    public void visit(Plus n) {
        math(n.e1, n.e2, "add", n.line_number);
    }

    public void visit(Minus n) {
        math(n.e1, n.e2, "subtract", n.line_number);
    }

    public void visit(Times n) {
        math(n.e1, n.e2, "multiply", n.line_number);
    }

    public void visit(ArrayLookup n) {

    }

    public void visit(ArrayLength n) {
        currentType = NodeType.INTEGER;
    }

    public void visit(Call n) {

    }

    public void visit(IntegerLiteral n) {
        currentType = NodeType.INTEGER;

    }

    public void visit(True n) {
        currentType = NodeType.BOOLEAN;
    }

    public void visit(False n) {
        currentType = NodeType.BOOLEAN;
    }

    public void visit(IdentifierExp n) {
        if (this.currentScope.containsKey(n.s)) {
            currentType = this.currentScope.get(n.s).getType();
        } else {
            System.err.println("(Line " + n.line_number + ") Variable " + n.s + " not defined in current scope.");
        }
    }

    public void visit(This n) {
        this.currentType = NodeType.CLASS;
    }

    public void visit(NewArray n) {

    }

    public void visit(NewObject n) {

    }

    public void visit(Not n) {
        n.e.accept(this);
        if (!this.currentType.equals(NodeType.BOOLEAN)) {
            System.err.println("(Line " + n.line_number + ") Cannot negate non-boolean variable.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        this.currentType = NodeType.BOOLEAN;
    }

    public void visit(Identifier n) {
        this.currentType = this.currentScope.get(n.s).getType();
    }


    public NodeType getType(Type t) {
        if (t instanceof BooleanType) {
            return NodeType.BOOLEAN;
        } else if (t instanceof IntegerType) {
            return NodeType.INTEGER;
        } else if (t instanceof IntArrayType) {
            return NodeType.ARRAY;
        } else if (t instanceof IdentifierType) {
            return NodeType.CLASS;
        } else {
            return NodeType.UNKNOWN;
        }
    }
}
