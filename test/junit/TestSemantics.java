import AST.Program;
import AST.Visitor.TestVisitor;
import AST.Visitor.UglyPrintVisitor;
import Parser.*;
import Scanner.*;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Symbol;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestSemantics {

    private static final String RESOURCE_DIR = "../test/resources/Semantics/";

    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final PrintStream stderr = System.err;

    @Before
    public void prepareAlternateIO() {
        System.setErr(new PrintStream(err));
    }

    @After
    public void restoreStandardIO() {
        System.setErr(stderr);
    }

    private void test(String testName) {
        try {
            ComplexSymbolFactory sf = new ComplexSymbolFactory();
            File f = new File(RESOURCE_DIR + testName + ".java");
            Reader in = new FileReader(f);
            scanner s = new scanner(in, sf);
            parser p = new parser(s, sf);
            Symbol root = p.parse();
            Program program = (Program) root.value;
            String expected = Files.readString(Paths.get(RESOURCE_DIR + testName + ".expected"));

            program.accept(new TestVisitor());
            assertEquals(expected, err.toString());
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDeclarations() { test("Declarations"); }

    @Test
    public void testOperatorTypes() { test("OperatorTypes"); }

    @Test
    public void testConditionals() { test("Conditionals"); }

    @Test
    public void testInheritanceCycle() { test("InheritanceCycle"); }

    @Test
    public void testClasses() { test("Classes"); }

    @Test
    public void testMethodCalls() { test("MethodCalls"); }

    @Test
    public void testSubclassTypes() { test("SubclassTypes"); }

    @Test
    public void testOverrides() { test("Overrides"); }

    @Test
    public void testAccessControl() { test("AccessControl"); }

}
