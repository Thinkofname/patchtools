package uk.co.thinkofdeath.patchtools.patch;

import org.objectweb.asm.Opcodes;
import uk.co.thinkofdeath.patchtools.patch.instructions.AReturnInstruction;
import uk.co.thinkofdeath.patchtools.patch.instructions.InvokeInstruction;
import uk.co.thinkofdeath.patchtools.patch.instructions.LdcInstruction;

public enum Instruction {
    ANY(null),
    LDC(new LdcInstruction()),
    ARETURN(new AReturnInstruction()),
    INVOKE_STATIC(new InvokeInstruction(Opcodes.INVOKESTATIC)),
    INVOKE_SPECIAL(new InvokeInstruction(Opcodes.INVOKESPECIAL)),
    INVOKE_VIRTUAL(new InvokeInstruction(Opcodes.INVOKEVIRTUAL)),
    INVOKE_INTERFACE(new InvokeInstruction(Opcodes.INVOKEINTERFACE)),;

    private final InstructionHandler handler;

    Instruction(InstructionHandler handler) {
        this.handler = handler;
    }

    public InstructionHandler getHandler() {
        return handler;
    }
}
