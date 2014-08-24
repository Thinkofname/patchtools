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
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

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

    public static InstructionChecker checkInvoke(int opcode) {
        return (classSet, scope, patchInstruction, insn) -> {
            if (!(insn instanceof MethodInsnNode) || insn.getOpcode() != opcode) {
                throw new PatchVerifyException();
            }
            MethodInsnNode node = (MethodInsnNode) insn;

            if (patchInstruction.params.length != 3) {
                throw new PatchVerifyException("Incorrect number of arguments for invoke");
            }

            Ident cls = new Ident(patchInstruction.params[0]);
            String clsName = cls.getName();
            if (!clsName.equals("*")) {
                if (cls.isWeak()) {
                    ClassWrapper ptcls = scope.getClass(clsName);
                    if (ptcls == null) { // Assume true
                        scope.putClass(classSet.getClassWrapper(node.owner), clsName);
                        clsName = node.owner;
                    } else {
                        clsName = ptcls.getNode().name;
                    }
                }
                if (!clsName.equals(node.owner)) {
                    throw new PatchVerifyException();
                }
            }

            Ident method = new Ident(patchInstruction.params[1]);
            String methodName = method.getName();
            if (!methodName.equals("*")) {
                if (method.isWeak()) {
                    ClassWrapper owner = classSet.getClassWrapper(node.owner);
                    MethodWrapper ptMethod = scope.getMethod(owner, methodName);
                    if (ptMethod == null) { // Assume true
                        scope.putMethod(classSet.getClassWrapper(node.owner)
                                .getMethod(node.name, node.desc), methodName);
                        methodName = node.name;
                    } else {
                        methodName = ptMethod.getName();
                    }
                }
                if (!methodName.equals(node.name)) {
                    throw new PatchVerifyException();
                }
            }

            Type patchDesc = Type.getMethodType(patchInstruction.params[2]);
            Type desc = Type.getMethodType(node.desc);

            for (int i = 0; i < patchDesc.getArgumentTypes().length; i++) {
                Type pt = patchDesc.getArgumentTypes()[i];
                Type t = desc.getArgumentTypes()[i];

                PatchClass.checkTypes(classSet, scope, pt, t);
            }
            PatchClass.checkTypes(classSet, scope, patchDesc.getReturnType(), desc.getReturnType());
        };
    }
}
