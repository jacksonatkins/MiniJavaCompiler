import Scanner.*;
import Parser.sym;
import java.io.*;
import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory;

public class MiniJava {

    public static void main(String[] args) {
        /*
         Types: int, boolean, int[], reference types
         If, while , assignment
         */
        try {
            ComplexSymbolFactory sf = new ComplexSymbolFactory();
            if (args.length > 0) {
                File f = new File(args[0]); // Do I need to add a catch or sum here?
                Reader in = new FileReader(f);
                scanner s = new scanner(in, sf);
                Symbol t = s.next_token();
                while (t.sym != sym.EOF) {
                    // print token
                    if (t.sym == sym.error) {
                        System.err.print(s.symbolToString(t) + " ");
                    } else {
                        System.out.print(s.symbolToString(t) + " ");
                    }
                    t = s.next_token();
                }
                System.exit(0);
            } else {
              System.err.println("No File Passed");
              System.exit(1);
            }
        } catch (Exception e) { // Maybe change to something more specific later?
             System.err.println("Oops");
             e.printStackTrace();
             System.exit(1); // Exit code 1 for error?
        }
    }

}
