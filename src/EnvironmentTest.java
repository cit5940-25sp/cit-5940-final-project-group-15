import ast.Environment;
import org.junit.Test;
import static org.junit.Assert.*;

public class EnvironmentTest {

    // === define ===
    @Test
    public void testDefine1_defineInCurrentScope() {
        Environment env = new Environment();
        env.define("x", 42);
        assertEquals(Integer.valueOf(42), env.get("x"));
    }

    @Test
    public void testDefine2_shadowParentValue() {
        Environment parent = new Environment();
        parent.define("x", 10);
        Environment child = new Environment(parent);
        child.define("x", 99);
        assertEquals(Integer.valueOf(99), child.get("x"));
        assertEquals(Integer.valueOf(10), parent.get("x"));
    }

    // === assign ===
    @Test
    public void testAssign1_updateLocalValue() {
        Environment env = new Environment();
        env.define("x", 5);
        env.assign("x", 20);
        assertEquals(Integer.valueOf(20), env.get("x"));
    }

    @Test
    public void testAssign2_updateParentValue() {
        Environment parent = new Environment();
        parent.define("x", 5);
        Environment child = new Environment(parent);
        child.assign("x", 30);
        assertEquals(Integer.valueOf(30), child.get("x"));
        assertEquals(Integer.valueOf(30), parent.get("x"));
    }

    @Test
    public void testAssign3_undefinedVariableThrows() {
        Environment env = new Environment();
        try {
            env.assign("y", 100);
            fail("Expected RuntimeException was not thrown");
        } catch (RuntimeException e) {
            assertEquals("Undefined variable 'y'", e.getMessage());
        }
    }

    // === get ===
    @Test
    public void testGet1_getFromCurrentScope() {
        Environment env = new Environment();
        env.define("z", 11);
        assertEquals(Integer.valueOf(11), env.get("z"));
    }

    @Test
    public void testGet2_getFromParentScope() {
        Environment parent = new Environment();
        parent.define("z", 11);
        Environment child = new Environment(parent);
        assertEquals(Integer.valueOf(11), child.get("z"));
    }

    @Test
    public void testGet3_undefinedVariableThrows() {
        Environment env = new Environment();
        try {
            env.get("z");
            fail("Expected RuntimeException was not thrown");
        } catch (RuntimeException e) {
            assertEquals("Undefined variable 'z'", e.getMessage());
        }
    }
}
