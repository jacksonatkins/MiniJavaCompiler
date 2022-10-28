import AST.Program;
import AST.Visitor.PrettyPrintVisitor;
import AST.Visitor.UglyPrintVisitor;
import Scanner.*;
import Parser.*;
import java.io.*;
import java.util.*;

import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory;
import java.nio.file.Paths;
import java.nio.file.Files;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

public class TestParser {

    public static final String TEST_FILES_LOCATION = "test/resources/Parser/";
    public static final String TEST_FILES_INPUT_EXTENSION = ".java";
    public static final String TEST_FILES_EXPECTED_EXTENSION = ".expected";

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Before
    public void setStreams() {
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
    }

    @After
    public void restoreInitialStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private void runParserTestCase(String testCaseName) {
        try {
            FileInputStream input = new FileInputStream(TEST_FILES_LOCATION + testCaseName + TEST_FILES_INPUT_EXTENSION);
            //String[] expected = new String(Files.readAllBytes(Paths.get(TEST_FILES_LOCATION, testCaseName + TEST_FILES_EXPECTED_EXTENSION)),
            //        Charset.defaultCharset()).split(" ");

            String expected = Files.readString(Paths.get(TEST_FILES_LOCATION, testCaseName + TEST_FILES_EXPECTED_EXTENSION));
            ComplexSymbolFactory sf = new ComplexSymbolFactory();
            Reader in = new BufferedReader(new InputStreamReader(input));
            scanner s = new scanner(in, sf);
            parser p = new parser(s, sf);
            Symbol root;
            try {
                root = p.parse();
                Program program = (Program) root.value;
                program.accept(new UglyPrintVisitor());

                assertEquals(expected, out.toString());
            } catch (Exception e) {
                assertEquals(expected, err.toString());
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testLT() {
        runParserTestCase("LTPrecedence");
    }

    @Test
    public void testFunction() {
        runParserTestCase("FunctionPrecedence");
    }

    @Test
    public void testAssign() {
        runParserTestCase("AssignPrecedence");
    }

    @Test
    public void testAndAssign() {
        runParserTestCase("AssignAndPrecedence");
    }

    @Test
    public void testNotAssign() { runParserTestCase("NotPrecedence"); }

    @Test
    public void testArrayAssign() { runParserTestCase("AssignArrayPrecedence"); }

    @Test
    public void QuickSort() { runParserTestCase("QuickSort"); }

    @Test
    public void BinarySearch() { runParserTestCase("BinarySearch"); }

    @Test
    public void Factorial() { runParserTestCase("Factorial"); }

    @Test
    public void MainMethod() { runParserTestCase("MainMethod"); }

    @Test
    public void DeclareMain() { runParserTestCase("DeclareMain"); }

    @Test
    public void TwoSemicolons() { runParserTestCase("TwoSemicolons"); }

    @Test
    public void IfNoElse() { runParserTestCase("IfNoElse"); }
}
