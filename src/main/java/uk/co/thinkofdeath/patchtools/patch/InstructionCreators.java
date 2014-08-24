package uk.co.thinkofdeath.patchtools.patch;

import com.google.common.base.Joiner;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.PatchVerifyException;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;

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

    public static InstructionCreator createInvoke(int opcode) {
        return (classSet, scope, patchInstruction) -> {
            if (patchInstruction.params.length != 3) {
                throw new PatchVerifyException("Incorrect number of arguments for invoke");
            }

            Ident ownerId = new Ident(patchInstruction.params[0]);
            String owner = ownerId.getName();
            if (ownerId.isWeak()) {
                owner = scope.getClass(owner).getNode().name;
            }
            Ident nameId = new Ident(patchInstruction.params[1]);
            String name = nameId.getName();
            if (nameId.isWeak()) {
                ClassWrapper cls = classSet.getClassWrapper(owner);
                name = scope.getMethod(cls, name, patchInstruction.params[2]).getName();
            }

            StringBuilder mappedDesc = new StringBuilder("(");
            Type desc = Type.getMethodType(patchInstruction.params[2]);
            for (Type type : desc.getArgumentTypes()) {
                PatchClass.updatedTypeString(classSet, scope, mappedDesc, type);
            }
            mappedDesc.append(")");
            PatchClass.updatedTypeString(classSet, scope, mappedDesc, desc.getReturnType());
            return new MethodInsnNode(
                    opcode,
                    owner,
                    name,
                    mappedDesc.toString(),
                    false
            );
        };
    }
}
