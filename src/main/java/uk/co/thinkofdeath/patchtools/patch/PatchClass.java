package uk.co.thinkofdeath.patchtools.patch;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PatchClass {

    private Ident ident;
    private Mode mode;

    private List<PatchMethod> methods = new ArrayList<>();

    public PatchClass(Command clCommand, BufferedReader reader) throws IOException {
        if (clCommand.args.length != 1) throw new IllegalArgumentException();
        ident = new Ident(clCommand.args[0]);
        mode = clCommand.mode;
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("//") || line.length() == 0) continue;

            Command command = Command.from(line);
            if (mode == Mode.ADD && command.mode != Mode.ADD) {
                throw new IllegalArgumentException("In added classes everything must be +");
            } else if (mode == Mode.REMOVE && command.mode != Mode.REMOVE) {
                throw new IllegalArgumentException("In removed classes everything must be -");
            }
            switch (command.name) {
                case "method":
                    methods.add(new PatchMethod(command, reader));
                    break;
                case "end-class":
                    return;
                default:
                    throw new IllegalArgumentException(command.toString());
            }
        }
    }
}
