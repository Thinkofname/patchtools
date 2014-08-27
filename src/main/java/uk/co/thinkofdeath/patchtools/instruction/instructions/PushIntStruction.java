package uk.co.thinkofdeath.patchtools.instruction.instructions;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.PatchVerifyException;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class PushIntStruction implements InstructionHandler {
    @Override
    public void check(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method, AbstractInsnNode insn) {
        if (instruction.params.length != 1) {
            throw new PatchVerifyException();
        }
        int val = 0;
        boolean any = false;
        if (instruction.params[0].equals("*")) {
            any = true;
        } else {
            val = Integer.parseInt(instruction.params[0]);
        }

        if (insn instanceof LdcInsnNode) {
            LdcInsnNode ldcInsnNode = (LdcInsnNode) insn;
            if (ldcInsnNode.cst instanceof Integer) {
                if (!any && (int) ldcInsnNode.cst != val) {
                    throw new PatchVerifyException();
                }
                return;
            }
        } else if (insn instanceof InsnNode) {
            if (insn.getOpcode() >= Opcodes.ICONST_M1 && insn.getOpcode() <= Opcodes.ICONST_5) {
                int other = insn.getOpcode() - Opcodes.ICONST_M1 - 1;
                if (!any && other != val) {
                    throw new PatchVerifyException(other + " vs " + val);
                }
                return;
            }
        } else if (insn instanceof IntInsnNode) {
            if (insn.getOpcode() == Opcodes.BIPUSH || insn.getOpcode() == Opcodes.SIPUSH) {
                int other = ((IntInsnNode) insn).operand;
                if (!any && other != val) {
                    throw new PatchVerifyException();
                }
                return;
            }
        }
        throw new PatchVerifyException(insn.getClass().getSimpleName() + " " + insn.getOpcode());
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        if (instruction.params.length != 1) {
            throw new PatchVerifyException();
        }
        int val = Integer.parseInt(instruction.params[0]);
        if (val >= -1 && val <= 5) {
            return new InsnNode(Opcodes.ICONST_M1 + val + 1);
        }
        if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
            return new IntInsnNode(Opcodes.BIPUSH, val);
        }
        if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
            return new IntInsnNode(Opcodes.SIPUSH, val);
        }
        return new LdcInsnNode(val);
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (insn instanceof LdcInsnNode) {
            LdcInsnNode ldcInsnNode = (LdcInsnNode) insn;
            if (ldcInsnNode.cst instanceof Integer) {
                patch.append("push-int ")
                        .append(ldcInsnNode.cst);
                return true;
            }
        } else if (insn instanceof InsnNode) {
            if (insn.getOpcode() >= Opcodes.ICONST_M1 && insn.getOpcode() <= Opcodes.ICONST_5) {
                patch.append("push-int ")
                        .append(insn.getOpcode() - Opcodes.ICONST_M1 - 1);
                return true;
            }
        } else if (insn instanceof IntInsnNode) {
            if (insn.getOpcode() == Opcodes.BIPUSH || insn.getOpcode() == Opcodes.SIPUSH) {
                patch.append("push-int ")
                        .append(((IntInsnNode) insn).operand);
                return true;
            }
        }
        return false;
    }
}
