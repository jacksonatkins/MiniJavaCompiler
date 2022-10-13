import Scanner.*;
import Parser.sym;
import java.io.*;
import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory;

public class MiniJava {

    public static void main(String[] args) {

        try {
            ComplexSymbolFactory sf = new ComplexSymbolFactory();
            int flag = 0;
            if (args.length > 0) { // TODO: Maybe change compiler flag?
                String option = "";
                File f;
                if (args.length > 1) {
                    option = args[0];
                    f = new File(args[1]);
                } else {
                    f = new File(args[0]);
                }
                if (option.equals("") || option.equals("-S")) {
                    Reader in = new FileReader(f);
                    scanner s = new scanner(in, sf);
                    Symbol t = s.next_token();
                    while (t.sym != sym.EOF) {
                        // print token
                        if (t.sym == sym.error) {
                            flag = 1;
                        }
                        System.out.print(s.symbolToString(t) + " ");
                        t = s.next_token();
                    }
                    System.exit(flag);
                } else {
                    System.err.println("Unknown Flag Passed.");
                    System.exit(1);
                }
            } else {
              System.err.println("No File Passed.");
              System.exit(1);
            }
        } catch (Exception e) {
             System.err.println("Error found. Printing stack trace.");
             e.printStackTrace();
             System.exit(1);
        }
    }

}
