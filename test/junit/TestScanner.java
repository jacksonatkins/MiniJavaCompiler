import Scanner.*;
import java.io.*;
import java.util.*;

import Parser.sym;
import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.charset.Charset;

import static org.junit.Assert.*;
import org.junit.Test;

public class TestScanner {

    public static final String TEST_FILES_LOCATION = "test/resources/Scanner/";
    public static final String TEST_FILES_INPUT_EXTENSION = ".java";
    public static final String TEST_FILES_EXPECTED_EXTENSION = ".expected";

    private void runScannerTestCase(String testCaseName) {
        try {
            FileInputStream input = new FileInputStream(TEST_FILES_LOCATION + testCaseName + TEST_FILES_INPUT_EXTENSION);
            String[] expected = new String(Files.readAllBytes(Paths.get(TEST_FILES_LOCATION, testCaseName + TEST_FILES_EXPECTED_EXTENSION)),
                    Charset.defaultCharset()).split(" ");

            ComplexSymbolFactory sf = new ComplexSymbolFactory();
            Reader in = new BufferedReader(new InputStreamReader(input));
            scanner s = new scanner(in, sf);
            Symbol t = s.next_token();
            int i = 0;
            while (t.sym != sym.EOF){
                // verify each token that we scan
                assertEquals(expected[i], s.symbolToString(t));
                t = s.next_token();
                i++;
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testIdentifier() {
        runScannerTestCase("Identifier");
    }

    @Test
    public void testThreeTokens () { runScannerTestCase("ThreeTokens"); }

    @Test
    public void testTwoKeyword () { runScannerTestCase("TwoKeyword"); }

    @Test
    public void testNewLine() { runScannerTestCase("NewLine"); }

    @Test
    public void testComments() { runScannerTestCase("Comments"); }

    @Test
    public void testError() { runScannerTestCase("Error"); }

    @Test
    public void testGeneral() { runScannerTestCase("General"); }

    @Test
    public void testBoolean() { runScannerTestCase("Boolean"); }
}
