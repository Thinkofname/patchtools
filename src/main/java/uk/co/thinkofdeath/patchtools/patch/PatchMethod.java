package uk.co.thinkofdeath.patchtools.patch;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PatchMethod {

    private Ident ident;
    private String desc;
    private Mode mode;

    private List<PatchInstruction> instructions = new ArrayList<>();

    public PatchMethod(Command mCommand, BufferedReader reader) throws IOException {
        if (mCommand.args.length != 2) throw new IllegalArgumentException();
        ident = new Ident(mCommand.args[0]);
        mode = mCommand.mode;

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
            if (command.name.equals("end-method")) {
                return;
            }

            instructions.add(new PatchInstruction(command));
        }

    }
}
