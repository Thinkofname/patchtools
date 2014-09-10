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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.patch.ValidateException;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class PushLongInstruction implements InstructionHandler {
    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method, AbstractInsnNode insn) {
        if (instruction.params.length != 1) {
            return false;
        }
        long val = 0;
        boolean any = false;
        if (instruction.params[0].equals("*")) {
            any = true;
        } else {
            val = Long.parseLong(instruction.params[0]);
        }

        if (insn instanceof LdcInsnNode) {
            LdcInsnNode ldcInsnNode = (LdcInsnNode) insn;
            if (ldcInsnNode.cst instanceof Long) {
                return !(!any && (long) ldcInsnNode.cst != val);
            }
        } else if (insn instanceof InsnNode) {
            if (insn.getOpcode() >= Opcodes.LCONST_0 && insn.getOpcode() <= Opcodes.LCONST_1) {
                long other = insn.getOpcode() - Opcodes.LCONST_0;
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
        long val = Long.parseLong(instruction.params[0]);
        if (val >= 0 && val <= 1) {
            return new InsnNode((int) (Opcodes.LCONST_0 + val));
        }
        return new LdcInsnNode(val);
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (insn instanceof LdcInsnNode) {
            LdcInsnNode ldcInsnNode = (LdcInsnNode) insn;
            if (ldcInsnNode.cst instanceof Long) {
                patch.append("push-long ")
                    .append(ldcInsnNode.cst);
                return true;
            }
        } else if (insn instanceof InsnNode) {
            if (insn.getOpcode() >= Opcodes.LCONST_0 && insn.getOpcode() <= Opcodes.LCONST_1) {
                patch.append("push-long ")
                    .append(insn.getOpcode() - Opcodes.LCONST_0);
                return true;
            }
        }
        return false;
    }

    @Override
    public void validate(PatchInstruction instruction) throws ValidateException {
        if (instruction.params.length != 1) {
            throw new ValidateException("Incorrect number of arguments for push-long");
        }

        try {
            if (!instruction.params[0].equals("*")) {
                Long.parseLong(instruction.params[0]);
            }
        } catch (NumberFormatException e) {
            throw new ValidateException("Invalid number " + e.getMessage());
        }
    }
}
