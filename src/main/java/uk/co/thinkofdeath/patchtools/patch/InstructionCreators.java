package uk.co.thinkofdeath.patchtools.patch;

import com.google.common.base.Joiner;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import uk.co.thinkofdeath.patchtools.ClassSet;
import uk.co.thinkofdeath.patchtools.PatchScope;

public class InstructionCreators {

    static AbstractInsnNode createLdc(ClassSet classSet, PatchScope scope, PatchInstruction instruction) {
        String cst = Joiner.on(' ').join(instruction.params);
        if (cst.startsWith("\"") && cst.endsWith("\"")) {
            return new LdcInsnNode(cst.substring(1, cst.length() - 1));
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static AbstractInsnNode createAReturn(ClassSet classSet, PatchScope scope, PatchInstruction instruction) {
        return new InsnNode(Opcodes.ARETURN);
    }
}
