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
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.Ident;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.patch.ValidateException;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class JumpInstruction implements InstructionHandler {

    private final int opcode;

    public JumpInstruction(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method, AbstractInsnNode insn) {
        return !(!(insn instanceof JumpInsnNode) || insn.getOpcode() != opcode)
            && Utils.checkOrSetLabel(scope, method, instruction.params[0], ((JumpInsnNode) insn).label);
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        return new JumpInsnNode(opcode, Utils.getLabel(scope, method, instruction.params[0]));
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof JumpInsnNode) || insn.getOpcode() != opcode) {
            return false;
        }
        switch (opcode) {
            case Opcodes.IFEQ:
                patch.append("if-zero");
                break;
            case Opcodes.IFNE:
                patch.append("if-not-zero");
                break;
            case Opcodes.IFLT:
                patch.append("if-less-zero");
                break;
            case Opcodes.IFGE:
                patch.append("if-greater-equal-zero");
                break;
            case Opcodes.IFGT:
                patch.append("if-greater-zero");
                break;
            case Opcodes.IFLE:
                patch.append("if-less-equal-zero");
                break;
            case Opcodes.IF_ICMPEQ:
                patch.append("if-equal-int");
                break;
            case Opcodes.IF_ICMPNE:
                patch.append("if-not-equal-int");
                break;
            case Opcodes.IF_ICMPLT:
                patch.append("if-less-int");
                break;
            case Opcodes.IF_ICMPGE:
                patch.append("if-greater-equal-int");
                break;
            case Opcodes.IF_ICMPGT:
                patch.append("if-greater-int");
                break;
            case Opcodes.IF_ICMPLE:
                patch.append("if-less-equal-int");
                break;
            case Opcodes.IF_ACMPEQ:
                patch.append("if-equal-object");
                break;
            case Opcodes.IF_ACMPNE:
                patch.append("if-not-equal-object");
                break;
            case Opcodes.GOTO:
                patch.append("goto");
                break;
            case Opcodes.JSR:
                patch.append("jsr");
                break;
            case Opcodes.IFNULL:
                patch.append("if-null");
                break;
            case Opcodes.IFNONNULL:
                patch.append("if-not-null");
                break;
            default:
                throw new UnsupportedOperationException("op:" + opcode);
        }
        patch.append(' ')
            .append('~')
            .append(Utils.printLabel(method, ((JumpInsnNode) insn).label));
        return true;
    }

    @Override
    public void validate(PatchInstruction instruction) throws ValidateException {
        if (instruction.params.length != 1) {
            throw new ValidateException("Incorrect number of arguments for jump");
        }
        if (!instruction.params[0].equals("*") && !new Ident(instruction.params[0]).isWeak()) {
            throw new ValidateException("Non-weak label");
        }
    }
}
