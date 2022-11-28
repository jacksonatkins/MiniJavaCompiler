package AST.Visitor;

import AST.*;
import CodeGeneration.Dependency;

import java.util.HashMap;
import java.util.Map;

public class DependencyVisitor implements Visitor {
    public Map<String, Dependency> dependencyGraph;

    public DependencyVisitor() {
        this.dependencyGraph = new HashMap<>();
    }

    public void visit(Display n) { }

    public void visit(Program n) {
        for (int i = 0; i < n.cl.size(); i++) {
            n.cl.get(i).accept(this);
        }
    }

    public void visit(MainClass n) { }

    public void visit(ClassDeclSimple n) {
        Dependency d = new Dependency(n.i.s);

        for (int i = 0; i < n.vl.size(); i++) {
            d.fields.add(n.vl.get(i).i.s);
        }

        for (int i = 0; i < n.ml.size(); i++) {
            d.methods.add(n.ml.get(i).i.s);
        }

        this.dependencyGraph.put(n.i.s, d);
    }

    public void visit(ClassDeclExtends n) {
        Dependency d = new Dependency(n.i.s);

        for (int i = 0; i < n.vl.size(); i++) {
            d.fields.add(n.vl.get(i).i.s);
        }

        for (int i = 0; i < n.ml.size(); i++) {
            d.methods.add(n.ml.get(i).i.s);
        }

        d.dependencies = n.j.s;
        this.dependencyGraph.put(n.i.s, d);
    }

    public void visit(VarDecl n) { }

    public void visit(MethodDecl n) { }

    public void visit(Formal n) { }

    public void visit(IntArrayType n) { }

    public void visit(BooleanType n) { }

    public void visit(IntegerType n) { }

    public void visit(IdentifierType n) { }

    public void visit(Block n) { }

    public void visit(If n) { }

    public void visit(While n) { }

    public void visit(Print n) { }

    public void visit(Assign n) { }

    public void visit(ArrayAssign n) { }

    public void visit(And n) { }

    public void visit(LessThan n) { }

    public void visit(Plus n) { }

    public void visit(Minus n) { }

    public void visit(Times n) { }

    public void visit(ArrayLookup n) { }

    public void visit(ArrayLength n) { }

    public void visit(Call n) { }

    public void visit(IntegerLiteral n) { }

    public void visit(True n) { }

    public void visit(False n) { }

    public void visit(IdentifierExp n) { }

    public void visit(This n) { }

    public void visit(NewArray n) { }

    public void visit(NewObject n) { }

    public void visit(Not n) { }

    public void visit(Identifier n) { }
}
