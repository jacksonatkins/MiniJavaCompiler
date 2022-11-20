package AST.Visitor;

import AST.*;
import java.util.*;

public class CodeGenerationVisitor implements Visitor{
    private List<String> code;

    public List<String> getCode() {
        return this.code;
    }

    public void visit(Display n) {

    }

    public void visit(Program n) {
        this.code = new ArrayList<>();
        n.m.accept(this);
        for (int i = 0; i < n.cl.size(); i++) {
            n.cl.get(i).accept(this);
        }
    }

    public void visit(MainClass n) {
        // Prologue
        this.code.add("    .text");
        this.code.add("    .globl asm_main");
        this.code.add("");
        this.code.add("asm_main:");

        n.s.accept(this);

        // Epilogue
        this.code.add("    movq    %rbp,%rsp");
        this.code.add("    popq    %rbp");
        this.code.add("    ret");
    }

    public void visit(ClassDeclSimple n) {

    }

    public void visit(ClassDeclExtends n) {

    }

    public void visit(VarDecl n) {

    }

    public void visit(MethodDecl n) {

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
        this.code.add("    pushq   %rbp");
        this.code.add("    movq    %rsp,%rbp");

        n.e.accept(this);
        // Printing the thing out
        this.code.add("    movq    %rax,%rdi");
        this.code.add("    call    put");

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
        // a + b
        n.e1.accept(this); // Integer constant -> placed in %rcx
        // move this to rax? do the addition
        this.code.add("    movq    %rax,%rdx");
        n.e2.accept(this); // Integer constant -> placed in %rcx
        this.code.add("    addq    %rdx,%rax");
    }

    public void visit(Minus n) {
        // a - b
        n.e1.accept(this); // a
        this.code.add("    movq    %rax,%rdx");
        n.e2.accept(this); // b
        this.code.add("    subq    %rdx,%rax");
    }

    public void visit(Times n) {
        // a - b
        n.e1.accept(this);
        this.code.add("    movq    %rax,%rdx");
        n.e2.accept(this);
        this.code.add("    imulq   %rdx,%rax");
    }

    public void visit(ArrayLookup n) {

    }

    public void visit(ArrayLength n) {

    }

    public void visit(Call n) {

    }

    public void visit(IntegerLiteral n) {
        this.code.add("    movq    $" + n.i +",%rax");
    }

    public void visit(True n) {
        this.code.add("    movq    $1,%rax");
    }

    public void visit(False n) {
        this.code.add("    movq    $0,%rax");
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
}
