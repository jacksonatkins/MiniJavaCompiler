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
    private int exitValue;
    private Node identifierNode;
    private boolean outputTable;

    public TestVisitor(boolean shouldOutput) {
        this.outputTable = shouldOutput;
    }

    public void visit(Display n) {

    }

    public void visit(Program n) {
        // Initialize the global symbol table
        TypeVisitor t = new TypeVisitor();
        n.accept(t);
        if (outputTable) {
            t.showTable();
        }
        this.table = t.symbolTable();
        Map<Integer, String> errors = t.returnErrors();
        n.exitValue = 0; // Initialize to no failure
        this.exitValue = 0;
        // Initialize the current scope
        this.currentScope = new HashMap<>();
        this.dependency = new HashMap<>();
        ClassNode out = null;
        if (n.cl.size() > 0) {
            out = dependencyChecker();
        }
        if (out != null) {
            int line = -1;
            this.exitValue = 1;
            for (int i = 0; i < n.cl.size(); i++) {
                ClassDeclSimple cds = (ClassDeclSimple) n.cl.get(i);
                if (cds.i.s.equals(out.name)) {
                    line = cds.line_number;
                }
            }
            System.err.println("(Line " + line + ") Cycle detected in dependency graph. Check your class extensions.");
        }
        // Loop through the classes in the Program
        for (int i = 0; i < n.cl.size(); i++) {
            n.cl.get(i).accept(this);
        }
        n.m.accept(this);
        n.exitValue = this.exitValue;
        for (int line : errors.keySet()) {
            System.err.println(errors.get(line));
        }
    }

    public ClassNode dependencyChecker() {
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
        queue.add(start);

        while (queue.size() != 0) {
            String s = queue.poll();
            if (!visited.containsKey(s)) {
                visited.put(s, 1);
                if (this.dependency.containsKey(s)) {
                    queue.addAll(this.dependency.get(s));
                }
            } else {
                return this.table.get(s);
            }
        }
        return null;
    }

    public void visit(MainClass n) {
        n.s.accept(this);
    }

    public void visit(ClassDeclSimple n) {
        if (!this.table.containsKey(n.i.s)) {
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Undeclared class " + n.i.s);
        }

        // Initialize the current node
        this.currentNode = this.table.get(n.i.s);
        this.currentType = NodeType.CLASS;

        ClassNode node = (ClassNode) this.currentNode;
        int pos = 0;
        for (String localVar : node.getFields().keySet()) {
            Node type = node.getFields().get(localVar);
            if (type.getType().equals(NodeType.IDENTIFIER)) {
                if (!this.table.containsKey(type.idType())) {
                    this.exitValue = 1;
                    this.currentType = NodeType.UNKNOWN;
                    System.err.println("(Line " + (n.vl.line_number+pos) + ") Type " + type.idType() + " not defined in the current scope.");
                    return;
                }
            }
            pos += 1;
        }
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
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Undeclared class " + n.i.s + ".");
            return;
        }
        if (!this.table.containsKey(n.j.s)) {
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Undeclared parent class " + n.j.s + ".");
            return;
        }

        // Initialize the current node
        this.currentNode = this.table.get(n.i.s);
        this.currentType = NodeType.CLASS;

        ClassExtendedNode node = (ClassExtendedNode) this.currentNode;
        for (String localVar : node.getFields().keySet()) {
            Node type = node.getFields().get(localVar);
            if (type.getType().equals(NodeType.IDENTIFIER)) {
                if (!this.table.containsKey(type.idType())) {
                    this.exitValue = 1;
                    this.currentType = NodeType.UNKNOWN;
                    System.err.println("(Line " + n.vl.line_number + ") Type " + type.idType() + " not defined in the current scope.");
                    return;
                }
            }
        }
        // Add variables from class to current scope
        this.currentScope.putAll(((ClassNode) this.currentNode).getFields());
        for (String s : this.dependency.get(n.i.s)) {
            ClassNode c = this.table.get(s);
            for (String field : c.getFields().keySet()) {
                if (!this.currentScope.containsKey(field)) {
                    this.currentScope.put(field, c.getFields().get(field));
                }
            }
        }

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
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Method must be declared inside of a class.");
        }

        // Initialize current node to be this node
        MethodNode m = ((ClassNode) this.previousNode).getMethods().get(n.i.s);
        this.currentNode = m;

        // Add all variables from parameter list and local decls to the current scope
        for (String parameter : m.getParameters().keySet()) {
            Node type = m.getParameters().get(parameter);
            if (type.getType().equals(NodeType.IDENTIFIER)) {
                if (!this.table.containsKey(type.idType())) {
                    this.exitValue = 1;
                    this.currentType = NodeType.UNKNOWN;
                    System.err.println("(Line " + n.fl.line_number + ") Type " + type.idType() + " not defined in the current scope.");
                    return;
                }
            }
        }

        for (String localVar : m.getLocalVars().keySet()) {
            Node type = m.getLocalVars().get(localVar);
            if (type.getType().equals(NodeType.IDENTIFIER)) {
                if (!this.table.containsKey(type.idType())) {
                    this.exitValue = 1;
                    this.currentType = NodeType.UNKNOWN;
                    System.err.println("(Line " + n.vl.line_number + ") Type " + type.idType() + " not defined in the current scope.");
                    return;
                }
            }
        }

        this.currentScope.putAll(m.getParameters());
        this.currentScope.putAll(m.getLocalVars());

        // If the method overrides another method from a parent class, we need to verify two things
        if (this.previousNode instanceof ClassExtendedNode) {
            String parent = ((ClassExtendedNode)this.previousNode).getExtendsName();
            ClassNode parentNode = this.table.get(parent);
            if (parentNode.getMethods().containsKey(n.i.s)) {
                MethodNode parentMethod = parentNode.getMethods().get(n.i.s);
                // Does this method have the same return type?
                if (parentMethod.getReturnType().getType().equals(NodeType.IDENTIFIER)) {
                    String idName = parentMethod.getReturnType().idType();
                    if (!idName.equals(m.getReturnType().idType()) && !this.dependency.get(m.getReturnType().idType()).contains(idName)) {
                        this.exitValue = 1;
                        this.currentType = NodeType.UNKNOWN;
                        System.err.println("(Line " + n.line_number + ") Overrode methods must have same return type.");
                        return;
                    }
                }
                if (!m.getReturnType().getType().equals(parentMethod.getReturnType().getType())) {
                    this.exitValue = 1;
                    this.currentType = NodeType.UNKNOWN;
                    System.err.println("(Line " + n.line_number + ") Overrode methods must have same return type.");
                    return;
                }
                // Does this method have the same #/type of parameters?
                List<Node> parentParams = parentMethod.getParameterTypes();
                List<Node> childParams = m.getParameterTypes();
                if (parentParams.size() != childParams.size()) {
                    this.exitValue = 1;
                    this.currentType = NodeType.UNKNOWN;
                    System.err.println("(Line " + n.line_number + ") Overrode methods must have same number of parameters.");
                    return;
                }
                for (int i = 0; i < parentParams.size(); i++) {
                    if (parentParams.get(i).getType().equals(NodeType.IDENTIFIER)) {
                        String paramName = parentParams.get(i).idType();
                        if (!paramName.equals(childParams.get(i).idType())) {
                            this.exitValue = 1;
                            this.currentType = NodeType.UNKNOWN;
                            System.err.println("(Line " + n.line_number + ") Overrode methods must have same parameter types.");
                            return;
                        }
                    }
                    if (!parentParams.get(i).getType().equals(childParams.get(i).getType())) {
                        this.exitValue = 1;
                        this.currentType = NodeType.UNKNOWN;
                        System.err.println("(Line " + n.line_number + ") Overrode methods must have same parameter types.");
                        return;
                    }
                }
            }
        }
        // Check through the statement list
        for (int i = 0; i < n.sl.size(); i++) {
            n.sl.get(i).accept(this);
        }

        if (getType(n.t).equals(NodeType.CLASS)) {
            IdentifierType it = (IdentifierType) n.t;
            if (!this.table.containsKey(it.s)) {
                this.exitValue = 1;
                System.err.println("(Line " + n.t.line_number + ") Return type of " + it.s + " not defined in current scope.");
            }
        }
        // Make sure that declared return type is the same as the actual returned type
        n.e.accept(this);
        if (!getType(n.t).equals(this.currentType)) {
            this.exitValue = 1;
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
        if (this.currentType.equals(NodeType.UNKNOWN)) {
            return;
        }
        for (int i = 0; i < n.sl.size(); i++) {
            n.sl.get(i).accept(this);
        }
    }

    public void visit(If n) {
        // Verify that the expression is a boolean
        n.e.accept(this);
        if (!this.currentType.equals(NodeType.BOOLEAN)) {
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") If statement must contain a boolean expression.");
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
            this.exitValue = 1;
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
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Cannot print non-integer values.");
            this.currentType = NodeType.UNKNOWN;
        }
    }

    public void visit(Assign n) {
        // Verify that both things are declared
        n.i.accept(this); // Other verifications come later

        // Verify that left type = right type
        Node leftNode = null;
        Node rightNode = null;
        NodeType left = this.currentType;
        if (left.equals(NodeType.CLASS)) {
            leftNode = this.identifierNode;
        }
        n.e.accept(this);
        NodeType right = this.currentType;
        if (right.equals(NodeType.CLASS)) {
            rightNode = this.identifierNode;
        } else if (right.equals(NodeType.IDENTIFIER)) {
            rightNode = this.identifierNode;
        }
        if (left.equals(NodeType.UNKNOWN) || right.equals(NodeType.UNKNOWN)) {
            return;
        }

        if (leftNode != null && rightNode != null) {
            String leftName = leftNode.idType();
            String rightName = rightNode.idType();
            if (!leftName.equals(rightName) && !this.dependency.get(rightName).contains(leftName)) {
                this.exitValue = 1;
                System.err.println("(Line " + n.line_number + ") Cannot assign variable of type " + rightName + " to variable of type "
                        + leftName);
                this.currentType = NodeType.UNKNOWN;
                return;
            }
        }

        if (!left.equals(right)) {
            this.exitValue = 1;
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
        } else {
            this.exitValue = 1;
        }
    }

    public void visit(And n) {
        n.e1.accept(this);
        NodeType left = this.currentType;
        n.e2.accept(this);
        NodeType right = this.currentType;

        if (!left.equals(NodeType.BOOLEAN) || !right.equals(NodeType.BOOLEAN)) {
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Cannot compare non-boolean variables.");
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
            this.exitValue = 1;
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
            this.exitValue = 1;
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
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Cannot index array with non-integer variable.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        this.currentType = NodeType.INTEGER;

    }

    public void visit(ArrayLength n) {
        n.e.accept(this);
        if (!this.currentType.equals(NodeType.ARRAY)) {
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Cannot get length of non-array variable.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        this.currentType = NodeType.INTEGER;
    }

    public void visit(Call n) {
        n.e.accept(this);
        if (this.currentType.equals(NodeType.UNKNOWN)) {
            return;
        }
        if (!this.currentType.equals(NodeType.CLASS)) {
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Cannot call method on non-class variable.");
            this.currentType = NodeType.UNKNOWN;
        }

        MethodNode calledMethod = null;
        for (String m : ((ClassNode) this.previousNode).getMethods().keySet()) {
            if (m.equals(n.i.s)) {
                calledMethod = ((ClassNode) this.previousNode).getMethods().get(m);
                break;
            }
        }
        if (calledMethod == null) {
            for (String c : this.table.keySet()) {
                for (String m : this.table.get(c).getMethods().keySet()) {
                    if (m.equals(n.i.s)) {
                        calledMethod = this.table.get(c).getMethods().get(m);
                        break;
                    }
                }
            }
        }
        MethodNode parentMethod = null;
        if (calledMethod == null) {
            if (!(this.previousNode instanceof ClassExtendedNode)) {
                System.err.println("(Line " + n.line_number + ") Method " + n.i.s + " not found in current scope.");
                this.currentType = NodeType.UNKNOWN;
                return;
            }
            ClassExtendedNode c = (ClassExtendedNode) this.previousNode;
            for (String parent : this.dependency.get(c.className)) {
                for (String method : this.table.get(parent).getMethods().keySet()) {
                    if (method.equals(n.i.s)) {
                        calledMethod = this.table.get(parent).getMethods().get(method);
                        break;
                    }
                }
            }
        } else {
            if (this.previousNode instanceof ClassExtendedNode) {
                ClassExtendedNode c = (ClassExtendedNode) this.previousNode;
                for (String parent : this.dependency.get(c.className)) {
                    for (String method : this.table.get(parent).getMethods().keySet()) {
                        if (method.equals(n.i.s)) {
                            parentMethod = this.table.get(parent).getMethods().get(method);
                            break;
                        }
                    }
                }
            }
        }

        if (calledMethod == null) {
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Method " + n.i.s + " not found in current scope.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }

        // Checks for overridden methods
        if (parentMethod != null) {
            // Check return type is the same
            boolean flag = false;
            if (!calledMethod.getReturnType().getType().equals(parentMethod.getReturnType().getType())) {
                System.err.println("(Line " + n.line_number + ") Overrode methods must have same return type.");
                flag = true;
            }
            // Check that the parameters are the same
            if (calledMethod.getParameterTypes().size() != parentMethod.getParameterTypes().size()) {
                System.err.println("(Line " + n.line_number + ") Overrode methods must have same number of parameters.");
                flag = true;
            }

            for (int i = 0; i < calledMethod.getParameterTypes().size(); i++) {
                if (!calledMethod.getParameterTypes().get(i).getType().equals(parentMethod.getParameterTypes().get(i).getType())) {
                    System.err.println("(Line " + n.line_number + ") Overrode methods must have same parameter types.");
                    flag = true;
                    break;
                }
            }
            if (flag) {
                this.exitValue = 1;
                this.currentType = NodeType.UNKNOWN;
                return;
            }

        }

        List<Node> parameterTypes = calledMethod.getParameterTypes();
        if (parameterTypes.size() != n.el.size()) {
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Incorrect number of parameters.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        boolean flag = false;
        for (int i = 0; i < n.el.size(); i++) {
            n.el.get(i).accept(this);
            if (parameterTypes.get(i).getType().equals(NodeType.IDENTIFIER)) {
                String expectedType = parameterTypes.get(i).idType(); // type = A
                assert this.identifierNode != null; // need this assertion, as it must be IdentifierExp?
                String actualType = this.identifierNode.idType();
                if (!actualType.equals(expectedType) && !this.dependency.get(actualType).contains(expectedType)) {
                    this.exitValue = 1;
                    System.err.println("(Line " + n.line_number + ") Incorrect parameter type at position " + (i+1) + ".");
                    flag = true;
                }
            } else if (!parameterTypes.get(i).getType().equals(this.currentType)) {
                this.exitValue = 1;
                System.err.println("(Line " + n.line_number + ") Incorrect parameter type at position " + (i+1) + ".");
                flag = true;
            }
        }
        if (!flag) {
            if (calledMethod.getReturnType().getType().equals(NodeType.IDENTIFIER)) {
                this.currentType = NodeType.CLASS;
            } else {
                this.currentType = calledMethod.getReturnType().getType();
            }
        } else {
            this.currentType = NodeType.UNKNOWN;
        }
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
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Variable " + n.s + " not defined in the current scope.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        // If it exists, set the current type to the type associated with the identifier
        if (this.currentScope.get(n.s).getType().equals(NodeType.IDENTIFIER)) {
            this.currentType = NodeType.CLASS;
            this.identifierNode = this.currentScope.get(n.s);
        } else {
            this.currentType = this.currentScope.get(n.s).getType();
        }
    }

    public void visit(This n) {
        this.currentType = NodeType.CLASS;
    }

    public void visit(NewArray n) {
        n.e.accept(this);
        if (!this.currentType.equals(NodeType.INTEGER)) {
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Cannot instantiate array with non-integer value.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        this.currentType = NodeType.ARRAY;
    }

    public void visit(NewObject n) {
        if (!this.table.containsKey(n.i.s)) {
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Cannot instantiate object of unknown class.");
            return;
        }

        if (this.currentNode == null && this.previousNode == null) { // i.e. coming from main class
            this.previousNode = this.table.get(n.i.s);
        }
        this.identifierNode = new Node(NodeType.CLASS);
        this.identifierNode.idType = n.i.s;
        this.currentType = NodeType.CLASS;
    }

    public void visit(Not n) {
        n.e.accept(this);
        if (!this.currentType.equals(NodeType.BOOLEAN)) {
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Cannot negate non-boolean variable.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        this.currentType = NodeType.BOOLEAN;
    }

    public void visit(Identifier n) {
        // Verify that the identifier is declared in the current scope
        if (!this.currentScope.containsKey(n.s)) {
            this.exitValue = 1;
            System.err.println("(Line " + n.line_number + ") Variable " + n.s + " not defined in the current scope.");
            this.currentType = NodeType.UNKNOWN;
            return;
        }
        if (this.currentScope.get(n.s).getType().equals(NodeType.IDENTIFIER)) {
            this.currentType = NodeType.CLASS;
            this.identifierNode = this.currentScope.get(n.s);
        } else {
            this.currentType = this.currentScope.get(n.s).getType();
        }
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
