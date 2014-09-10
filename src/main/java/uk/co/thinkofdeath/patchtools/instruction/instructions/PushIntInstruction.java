/*
 * Copyright 2014 Matthew Collins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.thinkofdeath.patchtools.instruction.instructions;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.patch.ValidateException;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class PushIntInstruction implements InstructionHandler {
    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method, AbstractInsnNode insn) {
        if (instruction.params.length != 1) {
            return false;
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
                return !(!any && (int) ldcInsnNode.cst != val);
            }
        } else if (insn instanceof InsnNode) {
            if (insn.getOpcode() >= Opcodes.ICONST_M1 && insn.getOpcode() <= Opcodes.ICONST_5) {
                int other = insn.getOpcode() - Opcodes.ICONST_M1 - 1;
                return !(!any && other != val);
            }
        } else if (insn instanceof IntInsnNode) {
            if (insn.getOpcode() == Opcodes.BIPUSH || insn.getOpcode() == Opcodes.SIPUSH) {
                int other = ((IntInsnNode) insn).operand;
                return !(!any && other != val);
            }
        }
        return false;
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        if (instruction.params.length != 1) {
            throw new RuntimeException();
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

    @Override
    public void validate(PatchInstruction instruction) throws ValidateException {
        if (instruction.params.length != 1) {
            throw new ValidateException("Incorrect number of arguments for push-int");
        }

        try {
            if (!instruction.params[0].equals("*")) {
                Integer.parseInt(instruction.params[0]);
            }
        } catch (NumberFormatException e) {
            throw new ValidateException("Invalid number " + e.getMessage());
        }
    }
}
