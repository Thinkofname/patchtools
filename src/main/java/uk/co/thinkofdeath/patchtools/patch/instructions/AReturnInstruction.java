package uk.co.thinkofdeath.patchtools.patch.instructions;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.PatchVerifyException;
import uk.co.thinkofdeath.patchtools.patch.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class AReturnInstruction implements InstructionHandler {
    @Override
    public void check(ClassSet classSet, PatchScope scope, PatchInstruction instruction, AbstractInsnNode insn) {
        if (!(insn instanceof InsnNode)) {
            throw new PatchVerifyException();
        }
        if (insn.getOpcode() != Opcodes.ARETURN) {
            throw new PatchVerifyException();
        }
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction) {
        return new InsnNode(Opcodes.ARETURN);
    }
}
