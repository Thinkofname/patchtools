package uk.co.thinkofdeath.patchtools.logging;

import uk.co.thinkofdeath.patchtools.matching.MatchClass;
import uk.co.thinkofdeath.patchtools.matching.MatchGroup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;

public class LoggableException extends RuntimeException {

    public LoggableException(StateLogger logger) {
        super(dump(logger));
    }

    private static String dump(StateLogger logger) {
        String name = new SimpleDateFormat("yyyy-mm-dd-hh-mm-ss-S").format(new Date()) + ".log";
        try (PrintWriter writer = new PrintWriter(new File(name))) {
            writer.println("Groups: " + logger.groups.size());
            for (MatchGroup group : logger.groups.keySet()) {
                writer.print("  Classes: ");
                writer.println(group.getClasses().size());
                for (MatchClass clazz : group.getClasses()) {
                    writer.print("    ");
                    writer.print(clazz.getName());
                    writer.print(" ");
                    writer.println(clazz.getMatches().size());
                    writer.print("    [ ");
                    writer.print(clazz.getMatches().stream()
                        .map(node -> node.name)
                        .collect(Collectors.joining(", ")));
                    writer.println(" ]");
                }
            }
            writer.println("Failed after " + logger.failedTicks + " tests");
            writer.println("Walk-through: ");
            writer.println(logger.writer.getBuffer().toString());
        } catch (FileNotFoundException e) {
            name = "failed to create log: " + e.getMessage();
        }
        return name;
    }

}
