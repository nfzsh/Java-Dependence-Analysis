package cn.lipg.instrument.jda;

import javassist.*;
import javassist.bytecode.MethodInfo;
import javassist.expr.*;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 指令检查器
 *
 * @author lipangeng, Email:lipg@outlook.com
 * Created on 2022/6/6 10:10
 * @version v1.0.0
 * @since v1.0.0
 */
public class InstrumentChecker extends ExprEditor {
    private String file;
    @Getter
    private final List<NotFountRecord> nfr = new LinkedList<>();

    @Override
    public boolean doit(CtClass clazz, MethodInfo minfo) throws CannotCompileException {
        return super.doit(clazz, minfo);
    }

    @Override
    public void edit(NewExpr e) throws CannotCompileException {
        try {
            CtConstructor constructor = e.getConstructor();
            constructor.getParameterTypes();
        } catch (NotFoundException ex) {
            nfr.add(NotFountRecord.builder()
                    .expr(e)
                    .exception(ex)
                    .originFileName(e.getEnclosingClass().getClassFile().getSourceFile())
                    .originLineNumber(e.getLineNumber())
                    .build());
            NotFountRecord.builder().expr(e).build();
            System.err.println("NewExpr Not Found" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void edit(FieldAccess f) throws CannotCompileException {
        try {
            f.getField();
            f.mayThrow();
        } catch (NotFoundException ex) {
            nfr.add(NotFountRecord.builder()
                    .expr(f)
                    .exception(ex)
                    .originFileName(f.getEnclosingClass().getClassFile().getSourceFile())
                    .originLineNumber(f.getLineNumber())
                    .build());
            System.err.println("Field Not Found" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void edit(NewArray a) throws CannotCompileException {
        try {
            a.getComponentType();
            a.mayThrow();
        } catch (NotFoundException ex) {
            nfr.add(NotFountRecord.builder()
                    .expr(a)
                    .exception(ex)
                    .originFileName(a.getEnclosingClass().getClassFile().getSourceFile())
                    .originLineNumber(a.getLineNumber())
                    .build());
            System.err.println("NewArray not found" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void edit(MethodCall m) throws CannotCompileException {
        try {
            CtMethod method = m.getMethod();
            method.getParameterTypes();
            method.getReturnType();
            method.getExceptionTypes();
            m.mayThrow();
        } catch (NotFoundException ex) {
            nfr.add(NotFountRecord.builder()
                    .expr(m)
                    .exception(ex)
                    .originFileName(m.getEnclosingClass().getClassFile().getSourceFile())
                    .originLineNumber(m.getLineNumber())
                    .build());
            System.err.println("MethodCall not found" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void edit(ConstructorCall c) throws CannotCompileException {
        try {
            CtConstructor constructor = c.getConstructor();
            constructor.getParameterTypes();
            constructor.getExceptionTypes();
            c.mayThrow();
        } catch (NotFoundException ex) {
            nfr.add(NotFountRecord.builder()
                    .expr(c)
                    .exception(ex)
                    .originFileName(c.getEnclosingClass().getClassFile().getSourceFile())
                    .originLineNumber(c.getLineNumber())
                    .build());
            System.err.println("ConstructorCall not found" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void edit(Instanceof i) throws CannotCompileException {
        try {
            i.getType();
            i.mayThrow();
        } catch (NotFoundException ex) {
            nfr.add(NotFountRecord.builder()
                    .expr(i)
                    .exception(ex)
                    .originFileName(i.getEnclosingClass().getClassFile().getSourceFile())
                    .originLineNumber(i.getLineNumber())
                    .build());
            System.err.println("Instanceof not found" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void edit(Cast c) throws CannotCompileException {
        try {
            c.getType();
            c.mayThrow();
        } catch (NotFoundException ex) {
            nfr.add(NotFountRecord.builder()
                    .expr(c)
                    .exception(ex)
                    .originFileName(c.getEnclosingClass().getClassFile().getSourceFile())
                    .originLineNumber(c.getLineNumber())
                    .build());
            System.err.println("Instanceof not found" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void clazz(CtClass clazz) throws CannotCompileException, NotFoundException {
        file = clazz.getURL().toString();
        try {
            for (CtField declaredField : clazz.getDeclaredFields()) {
                declaredField.getType();
            }
        } catch (NotFoundException ex) {
            nfr.add(NotFountRecord.builder()
                    .expr(null)
                    .exception(ex)
                    .originFileName(clazz.getPackageName() + clazz.getName())
                    .originLineNumber(-1)
                    .build());
        }
        clazz.instrument(this);
    }

    public void print(Pattern pattern) {
        List<NotFountRecord> nfr = this.nfr;
        if (pattern != null) {
            nfr = this.nfr.stream()
                    .filter(notFountRecord -> pattern.matcher(notFountRecord.getException().getMessage()).matches())
                    .collect(Collectors.toList());
        }
        if (nfr.size() == 0) {
            return;
        }
        System.out.printf("%s:%n", file);
        for (NotFountRecord notFountRecord : nfr) {
            System.out.printf("    [Line %s] -> %s %n", notFountRecord.getOriginLineNumber() == -1 ? "Field" :
                    notFountRecord.getOriginLineNumber(), notFountRecord.getException().getMessage());
        }
        System.out.println("###############" + "\n\n");
    }
}
