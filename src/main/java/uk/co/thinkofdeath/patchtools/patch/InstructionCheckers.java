package uk.co.thinkofdeath.patchtools.patch;

import com.google.common.base.Joiner;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import uk.co.thinkofdeath.patchtools.ClassSet;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.PatchVerifyException;

public class InstructionCheckers {
    static void checkLdc(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, AbstractInsnNode insn) {
        if (!(insn instanceof LdcInsnNode)) {
            throw new PatchVerifyException();
        }
        LdcInsnNode ldcInsnNode = (LdcInsnNode) insn;
        String cst = Joiner.on(' ').join(patchInstruction.params);

        if (cst.equals("*")) {
            return;
        }

        if (ldcInsnNode.cst instanceof String) {
            if (!cst.startsWith("\"") || !cst.endsWith("\"")) {
                throw new PatchVerifyException();
            }
            cst = cst.substring(1, cst.length() - 1);
            if (!ldcInsnNode.cst.equals(cst)) {
                throw new PatchVerifyException(ldcInsnNode.cst + " != " + cst);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static void checkAReturn(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, AbstractInsnNode insn) {
        if (!(insn instanceof InsnNode)) {
            throw new PatchVerifyException();
        }
        if (insn.getOpcode() != Opcodes.ARETURN) {
            throw new PatchVerifyException();
        }
    }
}
