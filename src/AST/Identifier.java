package AST;

import AST.Visitor.Visitor;
import java_cup.runtime.ComplexSymbolFactory.Location;

public class Identifier extends ASTNode {
  public String s;

  public Identifier(String as, Location pos) { 
    super(pos);
    s=as;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public String toString(){
    return s;
  }
}
