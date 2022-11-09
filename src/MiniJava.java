import AST.Program;
import AST.Statement;
import AST.Visitor.*;
import Parser.*;
import Scanner.*;
import java.io.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory;

public class MiniJava {
    private enum Flag {
        S, A, P, T;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("No file specified!");
            System.exit(1);
        }

        String path = "";
        Set<Flag> flags = EnumSet.noneOf(Flag.class);

        try {
            for (String arg : args) {
                if (arg.charAt(0) == '-') {
                    if (arg.length() > 2) {
                        System.err.println("Unknown flag '" +
                                arg.substring(1) + "' passed.");
                        System.exit(1);
                    }

                    switch (Character.toUpperCase(arg.charAt(1))) {
                        case 'S': flags.add(Flag.S);
                            break;
                        case 'A': flags.add(Flag.A);
                            break;
                        case 'P': flags.add(Flag.P);
                            break;
                        case 'T': flags.add(Flag.T);
                            break;
                        default:
                            System.err.println("Unknown flag '" +
                                    arg.substring(1) + "' passed.");
                            System.exit(1);
                    }
                } else {
                    path = arg;
                }
            }

            File f = new File(path);
            Reader in = new FileReader(f);
            ComplexSymbolFactory sf = new ComplexSymbolFactory();
            scanner s = new scanner(in, sf);
            boolean scannerError = false;

            if (flags.contains(Flag.S)) {
                Symbol t = s.next_token();

                while (t.sym != sym.EOF) {
                    if (t.sym == sym.error) {
                        scannerError = true;
                    }

                    System.out.print(s.symbolToString(t) + " ");
                    t = s.next_token();
                }

                System.out.print("\n");
            }

            if (scannerError) {
                System.err.println("Error occurred during scanning, exiting...");
                System.exit(1);
            }
            if (flags.contains(Flag.A) || flags.contains(Flag.P) || flags.contains(Flag.T)) {
                // If the user passes S and A/P, we reset the scanner to allow the tokens to be printed and pass the
                // tokens to the parser
                if (flags.contains(Flag.S)) {
                    f = new File(path);
                    in = new FileReader(f);
                    sf = new ComplexSymbolFactory();
                    s = new scanner(in, sf);
                }

                parser p = new parser(s, sf);
                Symbol root;
                root = p.parse();
                Program program = (Program) root.value;

                // Allows user to pass both parsing flags to see both trees
                if (flags.contains(Flag.A)) {
                    program.accept(new UglyPrintVisitor());
                    System.out.println("\n");
                }
                if (flags.contains(Flag.P)) {
                    program.accept(new PrettyPrintVisitor());
                    System.out.print("\n");
                }
                if (flags.contains(Flag.T)) {
                    //program.accept(new TypeVisitor());
                    program.accept(new TypeCheckVisitor());
                    System.out.println("\n");
                }
            }
            System.exit(0);
        } catch (Exception e) {
            System.err.println("Unexpected internal compiler error: " +
                    e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
