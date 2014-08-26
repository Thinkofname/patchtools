package uk.co.thinkofdeath.patchtools.patch;

import java.util.Arrays;

public class PatchInstruction {

    public Mode mode;
    public Instruction instruction;
    public String[] params;

    public PatchInstruction(Command command) {
        mode = command.mode;
        instruction = Instruction.valueOf(command.name.toUpperCase().replace('-', '_'));
        params = command.args;
    }

    @Override
    public String toString() {
        return "PatchInstruction{" +
                "mode=" + mode +
                ", instruction=" + instruction +
                ", params=" + Arrays.toString(params) +
                '}';
    }
}
