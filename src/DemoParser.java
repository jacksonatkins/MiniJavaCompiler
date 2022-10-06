import Scanner.*;
import Parser.*;
import AST.*;
import AST.Visitor.*;
import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory;
import java.util.*;
import java.io.*;

public class DemoParser {
    public static void main(String[] args) {
        try {
            // create a scanner on the input file
            ComplexSymbolFactory sf = new ComplexSymbolFactory();
            Reader in = new BufferedReader(new InputStreamReader(System.in));
            scanner s = new scanner(in, sf);
            parser p = new parser(s, sf);
            Symbol root;
            // replace p.parse() with p.debug_parse() in the next line to see
            // a trace of parser shift/reduce actions during parsing
            root = p.parse();
            // We know the following unchecked cast is safe because of the
            // declarations in the CUP input file giving the type of the
            // root node, so we suppress warnings for the next assignment.
            @SuppressWarnings("unchecked")
            List<Statement> program = (List<Statement>)root.value;
            for (Statement statement: program) {
                statement.accept(new PrettyPrintVisitor());
                System.out.print("\n");
            }
        } catch (Exception e) {
            // yuck: some kind of error in the compiler implementation
            // that we're not expecting (a bug!)
            System.err.println("Unexpected internal compiler error: " + 
                               e.toString());
            // print out a stack dump
            e.printStackTrace();
        }
    }
}
