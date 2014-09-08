package uk.co.thinkofdeath.patchtools.logging;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import uk.co.thinkofdeath.patchtools.matching.MatchClass;
import uk.co.thinkofdeath.patchtools.matching.MatchGroup;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class StateLogger {

    LinkedHashMap<MatchGroup, LoggedGroup> groups = new LinkedHashMap<>();
    long failedTicks = 0;
    StringWriter writer = new StringWriter();
    private int currentLevel = 0;
    private final boolean isActive = System.getProperty("patchLogging") != null;

    public StateLogger() {
    }

    public void createGroup(MatchGroup group) {
        groups.put(group, new LoggedGroup(group));
    }

    public void failedTicks(long tick) {
        this.failedTicks = tick;
    }

    public void println(String str) {
        if (!isActive) return;
        for (int i = 0; i < currentLevel; i++) {
            writer.write("  ");
        }
        writer.write(str);
        writer.write('\n');
    }

    public void println(Supplier<String> str) {
        if (!isActive) return;
        println(str.get());
    }

    public void indent() {
        currentLevel++;
    }

    public void unindent() {
        currentLevel--;
    }

    public static Supplier<String> typeMismatch(Type required, Type got) {
        return () -> "The type " + got + " did not match the required type " + required;
    }

    public static Supplier<String> match(ClassNode node, MatchClass clazz) {
        return () -> "Adding " + node.name + " as a possible match for " + clazz.getName();
    }
}
