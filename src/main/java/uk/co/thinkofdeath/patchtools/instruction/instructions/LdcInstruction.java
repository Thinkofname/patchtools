package uk.co.thinkofdeath.patchtools.instruction.instructions;

import com.google.common.base.Joiner;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.PatchVerifyException;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class LdcInstruction implements InstructionHandler {
    @Override
    public void check(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, MethodNode method, AbstractInsnNode insn) {
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

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        String cst = Joiner.on(' ').join(instruction.params);
        return new LdcInsnNode(Utils.parseConstant(cst));
    }
}
