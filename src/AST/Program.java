package AST;

import AST.Visitor.Visitor;
import java_cup.runtime.ComplexSymbolFactory.Location;

public class Program extends ASTNode {
  public MainClass m;
  public ClassDeclList cl;
  public int exitValue;
  public Program(MainClass am, ClassDeclList acl, Location pos) {
    super(pos);
    m=am; cl=acl; 
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public int getExitValue() {
    return this.exitValue;
  }
}
