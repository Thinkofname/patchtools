package uk.co.thinkofdeath.patchtools.instruction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.instruction.instructions.InvokeInstruction;
import uk.co.thinkofdeath.patchtools.instruction.instructions.LabelInstruction;
import uk.co.thinkofdeath.patchtools.instruction.instructions.LdcInstruction;
import uk.co.thinkofdeath.patchtools.instruction.instructions.ReturnInstruction;

public enum Instruction {
    ANY(null),
    LDC(new LdcInstruction()),
    RETURN(new ReturnInstruction()),
    INVOKE_STATIC(new InvokeInstruction(Opcodes.INVOKESTATIC)),
    INVOKE_SPECIAL(new InvokeInstruction(Opcodes.INVOKESPECIAL)),
    INVOKE_VIRTUAL(new InvokeInstruction(Opcodes.INVOKEVIRTUAL)),
    INVOKE_INTERFACE(new InvokeInstruction(Opcodes.INVOKEINTERFACE)),
    LABEL(new LabelInstruction()),;

    private final InstructionHandler handler;

    Instruction(InstructionHandler handler) {
        this.handler = handler;
    }

    public InstructionHandler getHandler() {
        return handler;
    }

    public static boolean print(StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        for (Instruction i : values()) {
            if (i.getHandler() != null
                    && i.getHandler().print(i, patch, method, insn)) {
                return true;
            }
        }
        return false;
    }
}
