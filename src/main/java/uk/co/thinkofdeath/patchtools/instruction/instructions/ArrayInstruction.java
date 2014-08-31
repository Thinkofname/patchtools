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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.PatchClass;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class ArrayInstruction implements InstructionHandler {

    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, MethodNode method, AbstractInsnNode insn) {
        if (patchInstruction.params.length != 1) {
            return false;
        }

        if (patchInstruction.params[0].equals("*")) {
            return (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.ANEWARRAY)
                || (insn instanceof IntInsnNode && insn.getOpcode() == Opcodes.NEWARRAY);
        }

        Type pType = Type.getType(patchInstruction.params[0]);

        if (pType.getSort() == Type.OBJECT || pType.getSort() == Type.ARRAY) {
            if (!(insn instanceof TypeInsnNode)) {
                return false;
            }
            String type = ((TypeInsnNode) insn).desc;
            if (!type.startsWith("[")) {
                type = "L" + type + ";";
            }
            return PatchClass.checkTypes(classSet, scope, pType, Type.getType(type));
        } else {
            if (!(insn instanceof IntInsnNode)) {
                return false;
            }
            int type;
            switch (pType.getSort()) {
                case Type.BOOLEAN:
                    type = 4;
                    break;
                case Type.CHAR:
                    type = 5;
                    break;
                case Type.FLOAT:
                    type = 6;
                    break;
                case Type.DOUBLE:
                    type = 7;
                    break;
                case Type.BYTE:
                    type = 8;
                    break;
                case Type.SHORT:
                    type = 9;
                    break;
                case Type.INT:
                    type = 10;
                    break;
                case Type.LONG:
                    type = 11;
                    break;
                default:
                    type = -1;
                    break;
            }
            return ((IntInsnNode) insn).operand == type;
        }
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        if (instruction.params.length != 1) {
            throw new RuntimeException();
        }
        Type pType = Type.getType(instruction.params[0]);

        if (pType.getSort() == Type.OBJECT || pType.getSort() == Type.ARRAY) {

            String desc = pType.getDescriptor();
            if (desc.startsWith("L")) {
                desc = desc.substring(1, desc.length() - 1);
            }
            return new TypeInsnNode(Opcodes.ANEWARRAY, desc);
        } else {
            int type;
            switch (pType.getSort()) {
                case Type.BOOLEAN:
                    type = 4;
                    break;
                case Type.CHAR:
                    type = 5;
                    break;
                case Type.FLOAT:
                    type = 6;
                    break;
                case Type.DOUBLE:
                    type = 7;
                    break;
                case Type.BYTE:
                    type = 8;
                    break;
                case Type.SHORT:
                    type = 9;
                    break;
                case Type.INT:
                    type = 10;
                    break;
                case Type.LONG:
                    type = 11;
                    break;
                default:
                    type = -1;
                    break;
            }
            return new IntInsnNode(Opcodes.NEWARRAY, type);
        }
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if ((insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.ANEWARRAY)) {
            String type = ((TypeInsnNode) insn).desc;
            if (!type.startsWith("[")) {
                type = "L" + type + ";";
            }
            patch.append("new-array ")
                .append(type);
            return true;
        }
        if (insn instanceof IntInsnNode && insn.getOpcode() == Opcodes.NEWARRAY) {
            String type;
            switch (((IntInsnNode) insn).operand) {
                case 4:
                    type = Type.BOOLEAN_TYPE.getDescriptor();
                    break;
                case 5:
                    type = Type.CHAR_TYPE.getDescriptor();
                    break;
                case 6:
                    type = Type.FLOAT_TYPE.getDescriptor();
                    break;
                case 7:
                    type = Type.DOUBLE_TYPE.getDescriptor();
                    break;
                case 8:
                    type = Type.BYTE_TYPE.getDescriptor();
                    break;
                case 9:
                    type = Type.SHORT_TYPE.getDescriptor();
                    break;
                case 10:
                    type = Type.INT_TYPE.getDescriptor();
                    break;
                case 11:
                    type = Type.LONG_TYPE.getDescriptor();
                    break;
                default:
                    type = "error";
                    break;
            }
            patch.append("new-array ")
                .append(type);
            return true;
        }
        return false;
    }
}
