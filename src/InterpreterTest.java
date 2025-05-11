import ast.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class InterpreterTest {

    // Test for:
    // visitProgram, visitFunctionDecl, visitBlock, visitVarDecl,visitBinaryExpr,
    // visitIntegerLiteral, visitPrintStmt, visitVarRef
    //
    //        function entry() {
    //            var x <- 1 + 2;
    //            print x;
    //        }
    //
    // Control flow:
    //Program
    //└── FunctionDecl("entry")
    //    └── Block
    //        ├── VarDecl("x")
    //        │   └── BinaryExpr("+")
    //        │       ├── IntegerLiteral(1)
    //        │       └── IntegerLiteral(2)
    //        └── PrintStmt
    //            └── VarRef("x")

    @Test
    public void testAdditionAndPrintProgram() {
        // var x <- 1 + 2;
        VarDecl decl = new VarDecl("x",
                new BinaryExpr(new IntegerLiteral(1), "+", new IntegerLiteral(2))
        );
        // print x;
        PrintStmt print = new PrintStmt(new VarRef("x"));

        Block body = new Block(List.of(decl, print));
        FunctionDecl entry = new FunctionDecl("entry", new ArrayList<>(), body);
        Program program = new Program(List.of(entry));

        // 捕捉 System.out 的输出
        String output = runWithCapturedOutput(() -> new Interpreter(program));
        assertEquals("3\n", output);
    }

    @Test
    public void testVisitReturnStmt() {
        ReturnStmt ret = new ReturnStmt(new IntegerLiteral(42));
        Block body = new Block(List.of(ret));
        FunctionDecl entry = new FunctionDecl("entry", new ArrayList<>(), body);
        Program program = new Program(List.of(entry));

        Interpreter interp = new Interpreter(program);
        Object result = interp.getResult();
        assertEquals(42, result);
    }

//    function entry() {
//        if (0) {
//            print 100;
//        } elif (0) {
//            print 200;
//        } else {
//            print 300;
//        }
//    }
    @Test
    public void testVisitIfstmt() {
        // 构建 else block: print 300;
        PrintStmt elsePrint = new PrintStmt(new IntegerLiteral(300));
        Block elseBlock = new Block(List.of(elsePrint));

        // 构建 elif: elif (0) { print 200; }
        PrintStmt elifPrint = new PrintStmt(new IntegerLiteral(200));
        Block elifBlock = new Block(List.of(elifPrint));
        ElifBranch elif = new ElifBranch(new IntegerLiteral(0), elifBlock);
        List<ElifBranch> elifs = List.of(elif);

        // 构建 if: if (0) { print 100; }
        PrintStmt ifPrint = new PrintStmt(new IntegerLiteral(100));
        Block ifBlock = new Block(List.of(ifPrint));
        IfStmt ifStmt = new IfStmt(new IntegerLiteral(0), ifBlock, elifs, elseBlock);

        // 构建函数体和程序
        FunctionDecl entry = new FunctionDecl("entry", new ArrayList<>(), new Block(List.of(ifStmt)));
        Program program = new Program(List.of(entry));

        // 捕捉输出
        String output = runWithCapturedOutput(() -> new Interpreter(program));
        assertEquals("300\n", output);
    }


//    function double(n) {
//        return n + n;
//    }
//
//    function entry() {
//        var result <- double(5);
//        print result;
//    }
    @Test
    public void testFunctionCall() {
        // function double(n) { return n + n; }
        String funcName = "double";
        List<String> params = List.of("n");
        Expression bodyExpr = new BinaryExpr(new VarRef("n"), "+", new VarRef("n"));
        ReturnStmt returnStmt = new ReturnStmt(bodyExpr);
        Block funcBody = new Block(List.of(returnStmt));
        FunctionDecl doubleFunc = new FunctionDecl(funcName, params, funcBody);

        // function entry() {
        //     var result <- double(5);
        //     print result;
        // }
        FuncCall callDouble = new FuncCall("double", List.of(new IntegerLiteral(5)));
        VarDecl resultDecl = new VarDecl("result", callDouble);
        PrintStmt printResult = new PrintStmt(new VarRef("result"));
        Block entryBody = new Block(List.of(resultDecl, printResult));
        FunctionDecl entry = new FunctionDecl("entry", new ArrayList<>(), entryBody);

        // Create Program and run interpreter
        Program program = new Program(List.of(doubleFunc, entry));
        String output = runWithCapturedOutput(() -> new Interpreter(program));

        assertEquals("10\n", output);
    }

    @Test
    public void testVisitWhileStmt() {
        // var x <- 0;
        VarDecl decl = new VarDecl("x", new IntegerLiteral(0));

        // while (x < 3) {
        //     x <- x + 1;
        // }
        WhileStmt loop = new WhileStmt(
                new BinaryExpr(new VarRef("x"), "<", new IntegerLiteral(3)),
                new Block(List.of(
                        new Assignment("x", new BinaryExpr(new VarRef("x"), "+", new IntegerLiteral(1)))
                ))
        );

        // print x;
        PrintStmt print = new PrintStmt(new VarRef("x"));

        Block body = new Block(List.of(decl, loop, print));
        FunctionDecl entry = new FunctionDecl("entry", List.of(), body);
        Program program = new Program(List.of(entry));

        String output = runWithCapturedOutput(() -> new Interpreter(program));
        assertEquals("3\n", output);
    }

    @Test
    public void testVisitRunWhileStmt() {
        // var x <- 0;
        VarDecl decl = new VarDecl("x", new IntegerLiteral(0));

        // run {
        //     x <- x + 1;
        // } while (x < 1);
        RunWhileStmt loop = new RunWhileStmt(
                new Block(List.of(
                        new Assignment("x", new BinaryExpr(new VarRef("x"), "+", new IntegerLiteral(1)))
                )),
                new BinaryExpr(new VarRef("x"), "<", new IntegerLiteral(1))
        );

        // print x;
        PrintStmt print = new PrintStmt(new VarRef("x"));

        Block body = new Block(List.of(decl, loop, print));
        FunctionDecl entry = new FunctionDecl("entry", List.of(), body);
        Program program = new Program(List.of(entry));

        String output = runWithCapturedOutput(() -> new Interpreter(program));
        assertEquals("1\n", output);
    }


    // InterpreterTest 的 helper 函数：捕获 System.out.println 输出内容
    private String runWithCapturedOutput(Runnable runnable) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        java.io.PrintStream oldOut = System.out;
        System.setOut(new java.io.PrintStream(out));
        runnable.run();
        System.setOut(oldOut);
        return out.toString();
    }


    @Test
    public void testSubtractMultiplicationDivisionModulo() {
        // var a <- 15 - 8;   // 7
        VarDecl declA = new VarDecl("a",
                new BinaryExpr(new IntegerLiteral(15), "-", new IntegerLiteral(8)));
        // var b <- 15 * 8;   // 120
        VarDecl declB = new VarDecl("b",
                new BinaryExpr(new IntegerLiteral(15), "*", new IntegerLiteral(8)));
        // var c <- 15 / 8;   // 1 (integer division)
        VarDecl declC = new VarDecl("c",
                new BinaryExpr(new IntegerLiteral(15), "/", new IntegerLiteral(8)));
        // var d <- 15 % 8;   // 7
        VarDecl declD = new VarDecl("d",
                new BinaryExpr(new IntegerLiteral(15), "%", new IntegerLiteral(8)));

        // print a; print b; print c; print d;
        PrintStmt printA = new PrintStmt(new VarRef("a"));
        PrintStmt printB = new PrintStmt(new VarRef("b"));
        PrintStmt printC = new PrintStmt(new VarRef("c"));
        PrintStmt printD = new PrintStmt(new VarRef("d"));

        Block body = new Block(List.of(
                declA, declB, declC, declD,
                printA, printB, printC, printD
        ));
        FunctionDecl entry = new FunctionDecl("entry", new ArrayList<>(), body);
        Program program = new Program(List.of(entry));

        String output = runWithCapturedOutput(() -> new Interpreter(program));
        assertEquals(
                "7\n"   +  // 15 - 8
                        "120\n" +  // 15 * 8
                        "1\n"   +  // 15 / 8
                        "7\n",     // 15 % 8
                output
        );
    }

    @Test
    public void testEnvironmentDefineAssignAndLookup() {
        // create a global environment and a nested child
        Environment global = new Environment();
        Environment child  = new Environment(global);

        // define in global and retrieve from both
        global.define("g", 10);
        assertEquals(Integer.valueOf(10), global.get("g"));
        assertEquals(Integer.valueOf(10), child.get("g"));  // falls through

        // define in child shadows global
        child.define("g", 20);
        assertEquals(Integer.valueOf(20), child.get("g"));
        assertEquals(Integer.valueOf(10), global.get("g"));

        // assign in child should update the nearest binding (child’s)
        child.assign("g", 30);
        assertEquals(Integer.valueOf(30), child.get("g"));
        assertEquals(Integer.valueOf(10), global.get("g"));

        // assign in child for a global‐only name should update global
        global.define("x", 5);
        child.assign("x", 15);
        assertEquals(Integer.valueOf(15), global.get("x"));
        assertEquals(Integer.valueOf(15), child.get("x"));
    }

    // === More test cases for visitFuncCall ===
    @Test
    public void TestBuiltInFuncAbs() {
        FuncCall absCall = new FuncCall("abs", List.of(new IntegerLiteral(-7)));
        Expression expr = absCall;
        FunctionDecl entry = new FunctionDecl("entry", new ArrayList<>(),
                new Block(List.of(new PrintStmt(expr))));
        Program program = new Program(List.of(entry));
        String output = runWithCapturedOutput(() -> new Interpreter(program));
        assertEquals("7\n", output);
    }

    @Test
    public void TestBuiltInFuncAbsWrongArgs() {
        FuncCall absCall = new FuncCall("abs", List.of()); // 0 argument
        FunctionDecl entry = new FunctionDecl("entry", new ArrayList<>(),
                new Block(List.of(new PrintStmt(absCall))));
        Program program = new Program(List.of(entry));

        try {
            new Interpreter(program);
            fail("Expected RuntimeException for abs() with 0 args");
        } catch (RuntimeException e) {
            assertEquals("abs() expects 1 argument.", e.getMessage());
        }
    }

    @Test
    public void TestBuiltInFuncMax() {
        FuncCall maxCall = new FuncCall("max", List.of(new IntegerLiteral(3), new IntegerLiteral(8)));
        FunctionDecl entry = new FunctionDecl("entry", new ArrayList<>(),
                new Block(List.of(new PrintStmt(maxCall))));
        Program program = new Program(List.of(entry));
        String output = runWithCapturedOutput(() -> new Interpreter(program));
        assertEquals("8\n", output);
    }

    @Test
    public void TestBuiltInFuncMaxWrongArgs() {
        FuncCall maxCall = new FuncCall("max", List.of(new IntegerLiteral(3)));
        FunctionDecl entry = new FunctionDecl("entry", new ArrayList<>(),
                new Block(List.of(new PrintStmt(maxCall))));
        Program program = new Program(List.of(entry));

        try {
            new Interpreter(program);
            fail("Expected RuntimeException for max() with 1 arg");
        } catch (RuntimeException e) {
            assertEquals("max() expects 2 arguments.", e.getMessage());
        }
    }

    @Test
    public void TestBuiltInFuncMin() {
        FuncCall minCall = new FuncCall("min", List.of(new IntegerLiteral(9), new IntegerLiteral(4)));
        FunctionDecl entry = new FunctionDecl("entry", new ArrayList<>(),
                new Block(List.of(new PrintStmt(minCall))));
        Program program = new Program(List.of(entry));
        String output = runWithCapturedOutput(() -> new Interpreter(program));
        assertEquals("4\n", output);
    }


}
