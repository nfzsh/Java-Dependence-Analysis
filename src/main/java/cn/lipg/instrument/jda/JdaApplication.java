package cn.lipg.instrument.jda;

import javassist.CannotCompileException;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class JdaApplication {
    private static final List<String> classes = new LinkedList<>();

    public static void main(String[] args) throws NotFoundException, IOException, CannotCompileException {
        Pattern pattern = null;
        ClassPool classPool = new ClassPool();
        classPool.appendSystemPath();
        for (int i = 0; i < args.length; i++) {
            if (i == 0 && args[0].startsWith("regex:")) {
                pattern = Pattern.compile(args[i].substring("regex:".length()));
                continue;
            }
            System.out.printf("found path %s %n", args[i]);
            addClassPath(classPool, args[i]);
        }
        System.out.printf("found classes %s%n", classes.size());
        List<InstrumentChecker> checkers = new LinkedList<>();
        for (String clazz : classes) {
            InstrumentChecker instrumentChecker = new InstrumentChecker();
            checkers.add(instrumentChecker);
            instrumentChecker.clazz(classPool.get(clazz));
        }
        System.out.println("Start Print");
        for (InstrumentChecker checker : checkers) {
            checker.print(pattern);
        }
        System.out.println("Print Complete");
    }

    public static void addClassPath(ClassPool classPool, String path) throws NotFoundException, IOException {
        ClassPath classPath = classPool.appendClassPath(path);
        switch (classPath.getClass().getTypeName()) {
            case "javassist.DirClassPath":
                findClasses(null, new File(path));
                break;
            case "javassist.JarClassPath":
                findJarClasses(path);
                break;
            case "javassist.JarDirClassPath":
                path = path.substring(0, path.length() - 2);
                File[] files = new File(path).listFiles((dir, name) -> name.endsWith(".jar") || name.endsWith(".zip"));
                for (File file : Optional.ofNullable(files).orElse(new File[0])) {
                    findJarClasses(file.getPath());
                }
                break;
            default:
                System.err.println("Not support " + classPath.getClass().getTypeName());
        }
    }

    private static void findClasses(String prefix, File path) {
        if (prefix == null) {
            prefix = path.getPath();
            if (!prefix.endsWith(File.separator)) {
                prefix += File.separator;
            }
        }
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            for (File file : Optional.ofNullable(files).orElse(new File[0])) {
                if (file.isDirectory()) {
                    findClasses(prefix, file);
                } else {
                    String filePath = file.getPath();
                    if (filePath.endsWith(".class")) {
                        classes.add(filePath.substring(prefix.length(), filePath.length() - ".class".length())
                                .replaceAll(File.separator, "."));
                    }
                }
            }
        }
    }

    private static void findJarClasses(String jar) throws IOException {
        JarFile jarFile = new JarFile(jar);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String name = jarEntry.getName();
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            if (jarEntry.isDirectory() || !name.endsWith(".class")) {
                continue;
            }
            classes.add(name.substring(0, name.length() - ".class".length()).replaceAll("/", "."));
        }
    }
}
