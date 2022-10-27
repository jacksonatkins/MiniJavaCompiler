package AST.Visitor;

import AST.*;

public class UglyPrintVisitor implements Visitor{

    public int indentLevel = 0;
    public String indent = "  ";

    public void printIndent(int level) {
        for (int i = 0; i < level; i++) {
            System.out.print(indent);
        }
    }

    public void visit(Display n) {
        // Do nothing, there is no Display. Needed to still implement Visitor.
    }

    public void visit(Program n) {
        System.out.println("Program");
        indentLevel += 1;
        n.m.accept(this);
        for (int i = 0; i < n.cl.size(); i++) {
            n.cl.get(i).accept(this);
            if (i + 1 < n.cl.size()) {
                System.out.println();
            }
        }
    }

    public void visit(MainClass n) {
        printIndent(indentLevel);
        System.out.print("MainClass ");
        n.i1.accept(this);
        System.out.println();
        indentLevel += 1;
        printIndent(indentLevel);
        n.s.accept(this);
        System.out.println();
        indentLevel -= 1;
    }

    public void visit(ClassDeclSimple n) {
        printIndent(indentLevel);
        System.out.print("Class ");
        n.i.accept(this);
        indentLevel += 1;
        for (int i = 0; i < n.vl.size(); i++) {
            System.out.println();
            printIndent(indentLevel);
            n.vl.get(i).accept(this);
        }
        for (int i = 0; i < n.ml.size(); i++) {
            System.out.println();
            n.ml.get(i).accept(this);
        }
        //System.out.println();
    }

    public void visit(ClassDeclExtends n) {
        printIndent(indentLevel);
        System.out.print("Class ");
        n.i.accept(this);
        System.out.print(" extends ");
        n.j.accept(this);
        indentLevel += 1;
        for (int i = 0; i < n.vl.size(); i++) {
            n.vl.get(i).accept(this);
            if (i + 1 < n.vl.size()) {
                System.out.println();
            }
        }
        for (int i = 0; i < n.ml.size(); i++) {
            System.out.println();
            n.ml.get(i).accept(this);
        }
        //System.out.println();
    }

    // Type t;
    // Identifier i;
    public void visit(VarDecl n) {
        n.t.accept(this);
        System.out.print(" ");
        n.i.accept(this);
    }

    // Type t;
    // Identifier i;
    // FormalList fl;
    // VarDeclList vl;
    // StatementList sl;
    // Exp e;
    public void visit(MethodDecl n) {
        printIndent(indentLevel);
        System.out.print("MethodDecl ");
        n.i.accept(this);
        indentLevel += 1;
        System.out.println();
        printIndent(indentLevel);
        System.out.print("returns ");
        n.t.accept(this);
        System.out.println();
        if (n.fl.size() > 0) {
            printIndent(indentLevel);
            System.out.print("parameters:");
            System.out.println();
        }
        indentLevel += 1;
        for (int i = 0; i < n.fl.size(); i++ ) {
            printIndent(indentLevel);
            n.fl.get(i).accept(this);
            System.out.println();
        }
        indentLevel -= 1;
        for (int i = 0; i < n.vl.size(); i++) {
            printIndent(indentLevel);
            n.vl.get(i).accept(this);
            System.out.println();
        }
        for (int i = 0; i < n.sl.size(); i++) {
            printIndent(indentLevel);
            n.sl.get(i).accept(this);
            System.out.println();
        }
        // System.out.println();
        // indentLevel -= 1;
        printIndent(indentLevel);
        System.out.print("Return ");
        n.e.accept(this);
        indentLevel -= 1;
    }

    // Type t;
    // Identifier i;
    public void visit(Formal n) {
        n.t.accept(this);
        System.out.print(" ");
        n.i.accept(this);
    }

    public void visit(IntArrayType n) {
        System.out.print("int[]");
    }

    public void visit(BooleanType n) {
        System.out.print("boolean");
    }

    public void visit(IntegerType n) {
        System.out.print("int");
    }

    // String s;
    public void visit(IdentifierType n) {
        System.out.print(n.s);
    }

    // StatementList sl;
    public void visit(Block n) {
        for (int i = 0; i < n.sl.size(); i++) {
            n.sl.get(i).accept(this);
            if (i + 1 < n.sl.size()) {
                System.out.println();
                printIndent(indentLevel);
            }
        }
        //System.out.println();
    }

    // Exp e;
    // Statement s1,s2;
    public void visit(If n) {
        indentLevel += 1;
        System.out.print("if ");
        n.e.accept(this);
        System.out.println();
        printIndent(indentLevel);
        n.s1.accept(this);
        System.out.println();
        printIndent(indentLevel - 1);
        System.out.println("else");
        printIndent(indentLevel);
        n.s2.accept(this);
        indentLevel -= 1;
    }

    // Exp e;
    // Statement s;
    public void visit(While n) {
        System.out.print("while ");
        n.e.accept(this);
        System.out.println();
        indentLevel += 1;
        printIndent(indentLevel);
        n.s.accept(this);
        indentLevel -= 1;
    }

    // Exp e;
    public void visit(Print n) {
       // printIndent(indentLevel);
        System.out.print("Print");
        indentLevel += 1;
        System.out.println();
        printIndent(indentLevel);
        n.e.accept(this);
        indentLevel -= 1;
    }

    // Identifier i;
    // Exp e;
    public void visit(Assign n) {
        n.i.accept(this);
        System.out.print(" = ");
        n.e.accept(this);
    }

    // Identifier i;
    // Exp e1,e2;
    public void visit(ArrayAssign n) {
        n.i.accept(this);
        System.out.print("[");
        n.e1.accept(this);
        System.out.print("] = ");
        n.e2.accept(this);
    }

    // Exp e1,e2;
    public void visit(And n) {
        System.out.print("(");
        n.e1.accept(this);
        System.out.print(" && ");
        n.e2.accept(this);
        System.out.print(")");
    }

    // Exp e1,e2;
    public void visit(LessThan n) {
        System.out.print("(");
        n.e1.accept(this);
        System.out.print(" < ");
        n.e2.accept(this);
        System.out.print(")");
    }

    // Exp e1,e2;
    public void visit(Plus n) {
        System.out.print("(");
        n.e1.accept(this);
        System.out.print(" + ");
        n.e2.accept(this);
        System.out.print(")");
    }

    // Exp e1,e2;
    public void visit(Minus n) {
        System.out.print("(");
        n.e1.accept(this);
        System.out.print(" - ");
        n.e2.accept(this);
        System.out.print(")");
    }

    // Exp e1,e2;
    public void visit(Times n) {
        System.out.print("(");
        n.e1.accept(this);
        System.out.print(" * ");
        n.e2.accept(this);
        System.out.print(")");
    }

    // Exp e1,e2;
    public void visit(ArrayLookup n) {
        n.e1.accept(this);
        System.out.print("[");
        n.e2.accept(this);
        System.out.print("]");
    }

    // Exp e;
    public void visit(ArrayLength n) {
        n.e.accept(this);
        System.out.print(".length");
    }

    // Exp e;
    // Identifier i;
    // ExpList el;
    public void visit(Call n) {
        n.e.accept(this);
        System.out.print(".");
        n.i.accept(this);
        System.out.print("(");
        for ( int i = 0; i < n.el.size(); i++ ) {
            n.el.get(i).accept(this);
            if ( i+1 < n.el.size() ) { System.out.print(", "); }
        }
        System.out.print(")");
    }

    // int i;
    public void visit(IntegerLiteral n) {
        System.out.print(n.i);
    }

    public void visit(True n) {
        System.out.print("true");
    }

    public void visit(False n) {
        System.out.print("false");
    }

    public void visit(IdentifierExp n) {
        System.out.print(n.s);
    }

    public void visit(This n) {
        System.out.print("this");
    }

    public void visit(NewArray n) {
        System.out.print("new int[");
        n.e.accept(this);
        System.out.print("]");
    }

    public void visit(NewObject n) {
        System.out.print("new ");
        System.out.print(n.i.s);
        System.out.print("()");
    }

    public void visit(Not n) {
        System.out.print("!");
        n.e.accept(this);
    }

    public void visit(Identifier n) {
        System.out.print(n.s);
    }
}
