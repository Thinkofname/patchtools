package uk.co.thinkofdeath.patchtools.patch;

public class PatchInstruction {

    private Mode mode;
    private Instruction instruction;
    private String[] params;

    public PatchInstruction(Command command) {
        if (command.args.length < 1) throw new IllegalArgumentException();
        mode = command.mode;
        instruction = Instruction.valueOf(command.args[0].toUpperCase());
        params = new String[command.args.length - 1];
        System.arraycopy(command.args, 1, params, 0, params.length);
    }
}
