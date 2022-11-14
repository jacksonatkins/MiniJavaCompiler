package AST.Visitor;

import AST.*;
import TypeNode.*;
import java.util.*;

public class TestVisitor implements Visitor{

    private Map<String, ClassNode> table;
    private NodeType currentType;
    private Node currentNode;
    private Node previousNode;
    private Map<String, Node> currentScope;
    private Map<String, List<String>> dependency;

    public void visit(Display n) {

    }

    public void visit(Program n) {
        // Initialize the global symbol table
        TypeVisitor t = new TypeVisitor();
        n.accept(t);
        this.table = t.symbolTable();

        // Initialize the current scope
        this.currentScope = new HashMap<>();
        this.dependency = new HashMap<>();

        // Loop through the classes in the Program
        for (int i = 0; i < n.cl.size(); i++) {
            n.cl.get(i).accept(this);
        }

        if (dependencyChecker()) {
            System.err.println("(Line " + n.line_number + ") Cycle detected in dependency graph. Check your class extensions.");
        }
    }

    public boolean dependencyChecker() {
        // Add all classes to the dependency table
        for (String s : this.table.keySet()) {
            this.dependency.put(s, new ArrayList<>());
        }

        for (String s : this.table.keySet()) {
            Node current = this.table.get(s);
            if (current instanceof ClassExtendedNode) {
                this.dependency.get(s).add(((ClassExtendedNode)current).getExtendsName());
            }
        }
        String start = new ArrayList<>(this.dependency.keySet()).get(0);
        Map<String, Integer> visited = new HashMap<>();
        LinkedList<String> queue = new LinkedList<>();
        visited.put(start, 1);
        queue.add(start);

        while (queue.size() != 0) {
            String s = queue.poll();
            if (!visited.containsKey(s)) {
                visited.put(s, 1);
                if (this.dependency.containsKey(s)) {
                    queue.addAll(this.dependency.get(s));
                }
            } else {
                return true;
            }
        }
        return false;
    }



    public void visit(MainClass n) {

    }

    public void visit(ClassDeclSimple n) {
        if (!this.table.containsKey(n.i.s)) {
            System.err.println("(Line " + n.line_number + ") Undeclared class " + n.i.s);
        }

        // Initialize the current node
        this.currentNode = this.table.get(n.i.s);
        this.currentType = NodeType.CLASS;

        // Add variables from class to current scope
        this.currentScope.putAll(((ClassNode) this.currentNode).getFields());

        // Search through the methods
        this.previousNode = this.currentNode;
        for (int i = 0; i < n.ml.size(); i++) {
            n.ml.get(i).accept(this);
        }

        // Reset the current scope - maybe
        // Reset current node?
        this.currentNode = null;
        this.previousNode = null;
        this.currentScope = new HashMap<>();
    }

    public void visit(ClassDeclExtends n) {
        if (!this.table.containsKey(n.i.s)) {
            System.err.println("(Line " + n.line_number + ") Undeclared class " + n.i.s);
        }

        //if (!this.dependency.containsKey(n.i.s)) {
        //    this.dependency.put(n.i.s, new ArrayList<>());
        //}
        //this.dependency.get(n.i.s).add(n.j.s);
        //this.dependency.get(n.i.s).addAll(this.dependency.get(n.j.s));

        // Initialize the current node
        this.currentNode = this.table.get(n.i.s);
        this.currentType = NodeType.CLASS;

        // Add variables from class to current scope
        this.currentScope.putAll(((ClassNode) this.currentNode).getFields());


        // Search through the methods
        this.previousNode = this.currentNode;
        for (int i = 0; i < n.ml.size(); i++) {
            n.ml.get(i).accept(this);
        }

        // Reset the current scope - maybe
        // Reset current node?
        this.currentNode = null;
        this.previousNode = null;
        this.currentScope = new HashMap<>();
    }

    public void visit(VarDecl n) {

    }

    public void visit(MethodDecl n) {
        // Make sure previous node is class
        if (!(this.previousNode instanceof ClassNode)) {
            System.err.println("(Line " + n.line_number + ") Method must be declared inside of a class.");
        }

        // Initialize current node to be this node
        MethodNode m = ((ClassNode) this.previousNode).getMethods().get(n.i.s);
        this.currentNode = m;

        // Add all variables from parameter list and local decls to the current scope
        this.currentScope.putAll(m.getParameters());
        this.currentScope.putAll(m.getLocalVars());

        // Do something else with these vars?

        // Check through the statement list
        for (int i = 0; i < n.sl.size(); i++) {
            n.sl.get(i).accept(this);
        }

        // Reset previous node to parent node (i.e. class node)
        // this.previousNode = parent;

        // Make sure that declared return type is the same as the actual returned type
        n.e.accept(this);
        if (!getType(n.t).equals(this.currentType)) {
            System.err.println("(Line " + n.e.line_number + ") Returns " + this.currentType + ", should return " + getType(n.t));
        }

        for (String s : m.getParameters().keySet()) {
            this.currentScope.remove(s);
        }
        for (String s : m.getLocalVars().keySet()) {
            this.currentScope.remove(s);
        }
    }

    public void visit(Formal n) {
        this.currentType = this.currentScope.get(n.i.s).getType();
    }

    public void visit(IntArrayType n) {
        this.currentType = NodeType.ARRAY;
    }

    public void visit(BooleanType n) {
        this.currentType = NodeType.BOOLEAN;
    }

    public void visit(IntegerType n) {
        this.currentType = NodeType.INTEGER;
    }

    public void visit(IdentifierType n) {
        this.currentType = NodeType.IDENTIFIER;
    }

    public void visit(Block n) {

    }

    public void visit(If n) {
        // Verify that the expression is a boolean
        n.e.accept(this);
        if (!this.currentType.equals(NodeType.BOOLEAN)) {
            System.err.println("(Line " + n.line_number + ") If statement must be a boolean expression.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        this.currentType = NodeType.BOOLEAN;

        // Type check the if/else branches' statements
        n.s1.accept(this);
        n.s2.accept(this);
    }

    public void visit(While n) {
        // Verify that the expression is a boolean
        n.e.accept(this);
        if (!this.currentType.equals(NodeType.BOOLEAN)) {
            System.err.println("(Line " + n.line_number + ") While statement must be a boolean expression.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }

        this.currentType = NodeType.BOOLEAN;

        // Type check the statement within the loop
        n.s.accept(this);
    }

    public void visit(Print n) {
        n.e.accept(this);
        if (!this.currentType.equals(NodeType.INTEGER)) {
            System.err.println("(Line " + n.line_number + ") Cannot print non-integer values.");
            this.currentType = NodeType.UNKNOWN;
        }
        // Do we do anything here?
    }

    public void visit(Assign n) {
        // Verify that both things are declared
        n.i.accept(this); // Other verifications come later

        // Verify that left type = right type
        NodeType left = this.currentType;
        n.e.accept(this);
        NodeType right = this.currentType;
        if (!left.equals(right)) {
            System.err.println("(Line " + n.line_number + ") Cannot assign variable of type " + left + " to variable of type "
                + right);
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        this.currentType = left;
    }

    public void visit(ArrayAssign n) {
        n.i.accept(this);
        // if not array, error
        NodeType arr = this.currentType;
        n.e1.accept(this);
        NodeType index = this.currentType;
        n.e2.accept(this);
        NodeType val = this.currentType;
        boolean flag = false;
        if (!arr.equals(NodeType.ARRAY)) {
            flag = true;
            System.err.println("(Line " + n.line_number + ") Cannot index non-array variable.");
            this.currentType = NodeType.UNKNOWN;
        }
        if (!index.equals(NodeType.INTEGER)) {
            flag = true;
            System.err.println("(Line " + n.line_number + ") Cannot use non-integer variable to index array.");
            this.currentType = NodeType.UNKNOWN;
        }
        if (!val.equals(NodeType.INTEGER)) {
            flag = true;
            System.err.println("(Line " + n.line_number + ") Cannot assign array value to non-integer variable.");
            this.currentType = NodeType.UNKNOWN;
        }
        if (!flag) {
            this.currentType = NodeType.ARRAY;
        }
    }

    public void visit(And n) {
        n.e1.accept(this);
        NodeType left = this.currentType;
        n.e2.accept(this);
        NodeType right = this.currentType;

        if (!left.equals(NodeType.BOOLEAN) || !right.equals(NodeType.BOOLEAN)) {
            System.err.println("(Line " + n.line_number + ") Cannot use && operator on non-boolean variables.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        this.currentType = NodeType.BOOLEAN;
    }

    public void visit(LessThan n) {
        n.e1.accept(this);
        NodeType left = this.currentType;
        n.e2.accept(this);
        NodeType right = this.currentType;

        if (!left.equals(NodeType.INTEGER) || !right.equals(NodeType.INTEGER)) {
            System.err.println("(Line " + n.line_number + ") Cannot compare non-integer variables.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        this.currentType = NodeType.BOOLEAN;
    }

    private void math(Exp e1, Exp e2, String type, int line_number) {
        e1.accept(this);
        NodeType left = this.currentType;
        e2.accept(this);
        NodeType right = this.currentType;
        if (!left.equals(right)) {
            System.err.println("(Line " + line_number + ") Cannot " + type + " non-integer variables.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        this.currentType = NodeType.INTEGER;
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
        // Make sure e1 is defined in the scope as an array
        n.e1.accept(this);

        // Make sure that e2 is an integer
        n.e2.accept(this);
        if (!this.currentType.equals(NodeType.INTEGER)) {
            System.err.println("(Line " + n.line_number + ") Cannot index array with non-integer variable.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        this.currentType = NodeType.INTEGER;

    }

    public void visit(ArrayLength n) {
        n.e.accept(this);
        if (!this.currentType.equals(NodeType.ARRAY)) {
            System.err.println("(Line " + n.line_number + ") Cannot get length of non-array variable.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        this.currentType = NodeType.INTEGER;
    }

    public void visit(Call n) {
        n.e.accept(this);
        if (!this.currentType.equals(NodeType.CLASS)) {
            System.err.println("(Line " + n.line_number + ") Cannot call method on non-class variable.");
            this.currentType = NodeType.UNKNOWN;
        }

        MethodNode calledMethod = null;
        for (String m : ((ClassNode) this.previousNode).getMethods().keySet()) {
            if (m.equals(n.i.s)) {
                calledMethod = ((ClassNode) this.previousNode).getMethods().get(m);
            }
        }
        if (calledMethod == null) {
            System.err.println("(Line " + n.line_number + ") Method " + n.i.s + " not found in current scope.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }

        List<Node> parameterTypes = calledMethod.getParameterTypes();
        for (int i = 0; i < n.el.size(); i++) {
            n.el.get(i).accept(this);
            if (!parameterTypes.get(i).getType().equals(this.currentType)) {
                System.err.println("(Line " + n.line_number + ") Incorrect parameter type at position " + (i+1) + ".");
                this.currentType = NodeType.UNKNOWN;
                return;
            }
        }
        this.currentType = calledMethod.getReturnType().getType();
    }

    public void visit(IntegerLiteral n) {
        this.currentType = NodeType.INTEGER;
    }

    public void visit(True n) {
        this.currentType = NodeType.BOOLEAN;
    }

    public void visit(False n) {
        this.currentType = NodeType.BOOLEAN;
    }

    public void visit(IdentifierExp n) {
        // Check that the identifier exists in the current scope
        if (!this.currentScope.containsKey(n.s)) {
            System.err.println("(Line " + n.line_number + ") Variable " + n.s + " not defined in the current scope.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        // If it exists, set the current type to the type associated with the identifier
        this.currentType = this.currentScope.get(n.s).getType();
    }

    public void visit(This n) {
        this.currentType = NodeType.CLASS;
    }

    public void visit(NewArray n) {
        n.e.accept(this);
        if (!this.currentType.equals(NodeType.INTEGER)) {
            System.err.println("(Line " + n.line_number + ") Cannot instantiate array with non-integer value.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        this.currentType = NodeType.ARRAY;
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
        // Verify that the identifier is declared in the current scope
        if (!this.currentScope.containsKey(n.s)) {
            System.err.println("(Line " + n.line_number + ") Variable " + n.s + " not defined in the current scope.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
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
