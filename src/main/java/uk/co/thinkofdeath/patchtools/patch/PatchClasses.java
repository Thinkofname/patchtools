package uk.co.thinkofdeath.patchtools.patch;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PatchClasses {

    private List<PatchClass> classes = new ArrayList<>();

    public PatchClasses(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("//") || line.length() == 0) continue;

            Command command = Command.from(line);
            switch (command.name) {
                case "class":
                    classes.add(new PatchClass(command, reader));
                    break;
                default:
                    throw new IllegalArgumentException(command.toString());
            }
        }
    }

    public List<PatchClass> getClasses() {
        return classes;
    }
}
