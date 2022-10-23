import AST.Program;
import AST.Statement;
import AST.Visitor.PrettyPrintVisitor;
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
        S, A, P;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("No file specified!");
            System.exit(1);
        }

        String path = new String();
        Set flags = EnumSet.noneOf(Flag.class);

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

            parser p = new parser(s, sf);
            Symbol root;
            root = p.parse();
            Program program = (Program) root.value;

            if (flags.contains(Flag.A)) {
                System.out.println("Not yet implemented! :(");
            } else if (flags.contains(Flag.P)) {
                program.accept(new PrettyPrintVisitor());
                System.out.print("\n");
            }
        } catch (Exception e) {
            System.err.println("Unexpected internal compiler error: " +
                    e.toString());
            e.printStackTrace();
            System.exit(1);
        }

        /*try {
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
        }*/
    }
}
