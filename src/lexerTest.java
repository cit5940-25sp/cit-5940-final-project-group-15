import org.junit.Test;
import java.util.List;
import static org.junit.Assert.assertEquals;

public class lexerTest {

    @Test
    public void testTokenize() {
        Lexer lexer = new Lexer("testFile1.txt");
        List<Token> actual1 = lexer.tokenize("testFile1.txt");
        assertEquals(34, actual1.size());
    }

    @Test
    public void testTokenize2() {
        Lexer lexer = new Lexer("testFile2.txt");
        List<Token> actual2 = lexer.tokenize("testFile2.txt");
        assertEquals(36, actual2.size());
    }

    @Test
    public void testTokenize3() {
        Lexer lexer = new Lexer("testFile3.txt");
        List<Token> actual3 = lexer.tokenize("testFile3.txt");
        assertEquals(23, actual3.size());
    }
}
