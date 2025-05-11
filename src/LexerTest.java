import org.junit.Test;
import java.util.List;
import static org.junit.Assert.assertEquals;

public class LexerTest {
    @Test
    public void testTokenize() {
        Lexer lexer = new Lexer("testFile.txt");
        List<Token> actual = lexer.tokenize("testFile.txt");
        assertEquals(84, actual.size());
    }
    @Test
    public void testModulo() {
        Lexer lexer = new Lexer("testLexer.txt");
        List<Token> actual = lexer.tokenize("testLexer.txt");
        assertEquals(15, actual.size());
    }
}
