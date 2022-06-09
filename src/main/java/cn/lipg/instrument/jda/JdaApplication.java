package cn.lipg.instrument.jda;

import javassist.CannotCompileException;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.NotFoundException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        String jdaFilter = System.getenv("JDA_FILTER");
        if (jdaFilter != null && jdaFilter.length() > 0) {
            pattern = Pattern.compile(jdaFilter);
        }
        boolean jdaDebug = Boolean.parseBoolean(System.getenv("JDA_DEBUG"));
        String outFile = Optional.ofNullable(System.getenv("JDA_OUT")).orElse("results.json");
        if (!outFile.endsWith(".json")) {
            outFile += ".json";
        }

        ClassPool classPool = new ClassPool();
        classPool.appendSystemPath();
        for (int i = 0; i < args.length; i++) {
            System.out.printf("found path %s %n", args[i]);
            addClassPath(classPool, args[i]);
        }
        System.out.printf("found classes %s%n", classes.size());
        List<InstrumentChecker> checkers = new LinkedList<>();
        for (String clazz : classes) {
            InstrumentChecker instrumentChecker = new InstrumentChecker();
            instrumentChecker.setDebug(jdaDebug);
            checkers.add(instrumentChecker);
            instrumentChecker.clazz(classPool.get(clazz));
        }
        System.out.println("Start Print");
        for (InstrumentChecker checker : checkers) {
            checker.print(pattern);
        }
        System.out.println("Print complete");

        System.out.println("Start export");
        JSONArray export = new JSONArray();
        for (InstrumentChecker checker : checkers) {
            List<NotFountRecord> nfr = checker.getNfr(pattern);
            if (nfr.size() > 0) {
                JSONObject clazz = new JSONObject();
                clazz.put("fileName", checker.getFile());
                JSONArray data = new JSONArray();
                for (NotFountRecord notFountRecord : nfr) {
                    JSONObject record = new JSONObject();
                    record.put("lineNumber", notFountRecord.getOriginLineNumber());
                    record.put("targetClass", notFountRecord.getException().getMessage());
                    data.put(record);
                }
                clazz.put("data", data);
                export.put(clazz);
            }
        }
        Files.write(Paths.get(outFile), export.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("Export complete");
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
