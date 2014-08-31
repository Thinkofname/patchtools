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
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class PushFloatInstruction implements InstructionHandler {
    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method, AbstractInsnNode insn) {
        if (instruction.params.length != 1) {
            return false;
        }
        float val = 0;
        boolean any = false;
        if (instruction.params[0].equals("*")) {
            any = true;
        } else {
            val = Float.parseFloat(instruction.params[0]);
        }

        if (insn instanceof LdcInsnNode) {
            LdcInsnNode ldcInsnNode = (LdcInsnNode) insn;
            if (ldcInsnNode.cst instanceof Float) {
                return !(!any && (float) ldcInsnNode.cst != val);
            }
        } else if (insn instanceof InsnNode) {
            if (insn.getOpcode() >= Opcodes.FCONST_0 && insn.getOpcode() <= Opcodes.FCONST_2) {
                float other = insn.getOpcode() - Opcodes.FCONST_0;
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
        float val = Float.parseFloat(instruction.params[0]);
        if (val >= 0 && val <= 2 && Math.floor(val) == val) {
            return new InsnNode((int) (Opcodes.FCONST_0 + val));
        }
        return new LdcInsnNode(val);
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (insn instanceof LdcInsnNode) {
            LdcInsnNode ldcInsnNode = (LdcInsnNode) insn;
            if (ldcInsnNode.cst instanceof Float) {
                patch.append("push-float ")
                    .append(ldcInsnNode.cst);
                return true;
            }
        } else if (insn instanceof InsnNode) {
            if (insn.getOpcode() >= Opcodes.FCONST_0 && insn.getOpcode() <= Opcodes.FCONST_2) {
                patch.append("push-float ")
                    .append(insn.getOpcode() - Opcodes.FCONST_0);
                return true;
            }
        }
        return false;
    }
}
