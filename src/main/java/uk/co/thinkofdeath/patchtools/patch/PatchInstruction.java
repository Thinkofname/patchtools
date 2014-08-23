package uk.co.thinkofdeath.patchtools.patch;

public class PatchInstruction {

    Mode mode;
    Instruction instruction;
    String[] params;

    public PatchInstruction(Command command) {
        mode = command.mode;
        instruction = Instruction.valueOf(command.name.toUpperCase());
        params = command.args;
    }
}
