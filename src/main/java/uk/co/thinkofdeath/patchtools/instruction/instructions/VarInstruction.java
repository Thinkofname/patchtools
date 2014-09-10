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
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.patch.ValidateException;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class VarInstruction implements InstructionHandler {

    private final int opcode;

    public VarInstruction(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof VarInsnNode)
            || insn.getOpcode() != opcode) {
            return false;
        }
        int index = 0;
        boolean any = false;
        if (instruction.params[0].equals("*")) {
            any = true;
        } else {
            index = Integer.parseInt(instruction.params[0]);
        }

        int other = ((VarInsnNode) insn).var;
        return !(!any && other != index);
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        return new VarInsnNode(opcode, Integer.parseInt(instruction.params[0]));
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (insn instanceof VarInsnNode && insn.getOpcode() == opcode) {
            VarInsnNode varInsnNode = (VarInsnNode) insn;
            switch (varInsnNode.getOpcode()) {
                case Opcodes.ALOAD:
                    patch.append("load-object");
                    break;
                case Opcodes.ILOAD:
                    patch.append("load-int");
                    break;
                case Opcodes.LLOAD:
                    patch.append("load-long");
                    break;
                case Opcodes.FLOAD:
                    patch.append("load-float");
                    break;
                case Opcodes.DLOAD:
                    patch.append("load-double");
                    break;
                case Opcodes.ASTORE:
                    patch.append("store-object");
                    break;
                case Opcodes.ISTORE:
                    patch.append("store-int");
                    break;
                case Opcodes.LSTORE:
                    patch.append("store-long");
                    break;
                case Opcodes.FSTORE:
                    patch.append("store-float");
                    break;
                case Opcodes.DSTORE:
                    patch.append("store-double");
                    break;
                case Opcodes.RET:
                    patch.append("ret");
                    break;
            }
            patch.append(' ')
                .append(((VarInsnNode) insn).var);
            return true;
        }
        return false;
    }

    @Override
    public void validate(PatchInstruction instruction) throws ValidateException {
        if (instruction.params.length != 1) {
            throw new ValidateException("Incorrect number of arguments for var instruction");
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
