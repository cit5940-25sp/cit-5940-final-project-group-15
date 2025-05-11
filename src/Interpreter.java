import ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


public class Interpreter implements ASTVisitor<Object> {

    private final Environment globals = new Environment();  // global variable scope（保持不变）
    private Environment environment = globals;              // current variable scope（会切换）

    // functionTable 记录了程序中所有的函数定义，作为全局 function scope 管理
    private final Map<String, FunctionDecl> functionTable = new HashMap<>(); //
    private Object result;

    // constructor
    public Interpreter(Program program) {
        this.result = visitProgram(program); // 保存执行结果
    }

    public Object getResult() {
        return result;
    }

    @Override
    public Object visitAssignment(Assignment assignment) {
        Object value = assignment.value.accept(this);
        environment.assign(assignment.name, (Integer) value);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr be) {
        int left = (int) be.left.accept(this);
        int right = (int) be.right.accept(this);
        return switch (be.operator) {
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> left / right;
            case "%" -> left % right;
            case "=" -> left == right ? 1 : 0;
            case "~" -> left != right ? 1 : 0;
            case "<" -> left < right ? 1 : 0;
            case ">" -> left > right ? 1 : 0;
            case "<=" -> left <= right ? 1 : 0;
            case ">=" -> left >= right ? 1 : 0;
            default -> throw new RuntimeException("Unknown operator: " + be.operator);
        };
    }

    @Override
    public Object visitInputExpr(InputExpr ie) {
        System.out.print("user input> ");
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        return scanner.nextInt();
    }

    @Override
    public Object visitBlock(Block block) {
        for (Statement stmt : block.statements) {
            stmt.accept(this);  // 顺序执行语句
        }
        return null;
    }

    // 在 visitIfstmt() 中已经处理了 elifBranches，所以暂时不用实现
    @Override
    public Object visitElifBranch(ElifBranch elifb) {
        return null;
    }

    // 是 Expression 抽象类，通常不会直接访问这个节点，所以暂时不用实现
    @Override
    public Object visitExpression(Expression expression) {
        return null;
    }

    @Override
    public Object visitFuncCall(FuncCall fc) {

        // Handle built-in functions
        if (fc.name.equals("abs")) {
            if (fc.arguments.size() != 1) {
                throw new RuntimeException("abs() expects 1 argument.");
            }
            int arg = (int) fc.arguments.get(0).accept(this);
            return Math.abs(arg);
        }

        if (fc.name.equals("max")) {
            if (fc.arguments.size() != 2) {
                throw new RuntimeException("max() expects 2 arguments.");
            }
            int a = (int) fc.arguments.get(0).accept(this);
            int b = (int) fc.arguments.get(1).accept(this);
            return Math.max(a, b);
        }

        if (fc.name.equals("min")) {
            if (fc.arguments.size() != 2) {
                throw new RuntimeException("min() expects 2 arguments.");
            }
            int a = (int) fc.arguments.get(0).accept(this);
            int b = (int) fc.arguments.get(1).accept(this);
            return Math.min(a, b);
        }

        FunctionDecl func = functionTable.get(fc.name);
        if (func == null) {
            throw new RuntimeException("Undefined function: " + fc.name);
        }

        // handle wrong number of arguments
        if (fc.arguments.size() != func.params.size()) {
            throw new RuntimeException("Function '" + fc.name + "' expects " + func.params.size() +
                    " arguments, but got " + fc.arguments.size() + ".");
        }

        List<Object> argValues = new ArrayList<>();
        for (Expression arg : fc.arguments) {
            argValues.add(arg.accept(this));
        }

        Environment previous = environment;
        environment = new Environment(previous);

        for (int i = 0; i < func.params.size(); i++) {
            environment.define(func.params.get(i), (Integer) argValues.get(i));
        }

        try {
            return func.body.accept(this);
        } catch (ReturnException e) {
            return e.value;
        } finally {
            environment = previous;
        }
    }

    @Override
    public Object visitFunctionDecl(FunctionDecl fd) {
        Environment previous = environment;
        environment = new Environment(previous);  // 创建新作用域

        try {
            fd.body.accept(this);  // 执行函数体（block）
        } catch (ReturnException ret) {
            return ret.value;  // 支持 return X;
        } finally {
            environment = previous; // 恢复作用域
        }

        return null;
    }

    @Override
    public Object visitIfstmt(IfStmt is) { // 0 is false, != 0 is true
        int cond = (int) is.condition.accept(this);
        if (cond != 0) {
            return is.thenBranch.accept(this);
        }

        for (ElifBranch elif : is.elifBranches) {
            int elifCond = (int) elif.condition.accept(this);
            if (elifCond != 0) {
                return elif.body.accept(this);
            }
        }

        if (is.elseBranch != null) {
            return is.elseBranch.accept(this);
        }

        return null;
    }

    @Override
    public Object visitIntegerLiteral(IntegerLiteral il) {
        return il.value;
    }

    @Override
    public Object visitPrintStmt(PrintStmt ps) {
        Object value = ps.expression.accept(this);
        System.out.println(value); // 打印内容后会自动添加一个换行符\n
        return null;
    }

    @Override
    public Object visitProgram(Program program) {
        // 1. 把所有函数加入函数表
        for (FunctionDecl func : program.functions) {
            functionTable.put(func.name, func);
        }

        // 2. 找到 entry 函数
        FunctionDecl entry = functionTable.get("entry");
        if (entry == null) {
            throw new RuntimeException("No entry() function found.");
        }

        // 3. 调用 entry()
        return visitFunctionDecl(entry);
    }

    @Override
    public Object visitReturnStmt(ReturnStmt rs) {
        Object value = rs.expression.accept(this);  // 求值
        throw new ReturnException((int) value);     // 抛出返回值中断执行
    }

    @Override
    public Object visitRunWhileStmt(RunWhileStmt rws) {
        do {
            rws.body.accept(this);
        } while ((int) rws.condition.accept(this) != 0);
        return null;
    }

    // 真正处理的都是 Statement 的子类，statement 这个抽象父类不会被具体调用，所以不必实现
    @Override
    public Object visitStatement(Statement statement) {
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl vd) {
        Object value = vd.initializer.accept(this); // 递归执行右边表达式
        environment.define(vd.name, (Integer) value);
        return null;
    }

    @Override
    public Object visitVarRef(VarRef vr) {
        return environment.get(vr.name);  // 从作用域查变量
    }

    @Override
    public Object visitWhileStmt(WhileStmt ws) {
        while ((int) ws.condition.accept(this) != 0) {
            ws.body.accept(this);
        }
        return null;
    }
}
