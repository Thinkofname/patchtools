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

import com.google.common.collect.ImmutableList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.matching.MatchClass;
import uk.co.thinkofdeath.patchtools.matching.MatchGenerator;
import uk.co.thinkofdeath.patchtools.patch.Ident;
import uk.co.thinkofdeath.patchtools.patch.PatchClass;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.patch.ValidateException;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.util.Arrays;
import java.util.List;

public class ArrayInstruction implements InstructionHandler {

    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, MethodNode method, AbstractInsnNode insn) {
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
            return PatchClass.checkTypes(classSet, scope, pType, Type.getObjectType(type));
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
        Type pType = Type.getType(instruction.params[0]);

        if (pType.getSort() == Type.OBJECT || pType.getSort() == Type.ARRAY) {
            StringBuilder nDesc = new StringBuilder();
            PatchClass.updatedTypeString(classSet, scope, nDesc, pType);
            String desc = nDesc.toString();
            return new TypeInsnNode(Opcodes.ANEWARRAY, Type.getType(desc).getInternalName());
        } else {
            int type = -1;
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
            }
            return new IntInsnNode(Opcodes.NEWARRAY, type);
        }
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if ((insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.ANEWARRAY)) {
            String type = ((TypeInsnNode) insn).desc;
            patch.append("new-array ")
                .append(Type.getObjectType(type).getDescriptor());
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

    @Override
    public void validate(PatchInstruction instruction) throws ValidateException {
        if (instruction.params.length != 1) {
            throw new ValidateException("Incorrect number of arguments for new-array");
        }

        Utils.validateType(instruction.params[0]);

        Type pType = Type.getType(instruction.params[0]);

        if (pType.getSort() != Type.OBJECT && pType.getSort() != Type.ARRAY) {
            switch (pType.getSort()) {
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.FLOAT:
                case Type.DOUBLE:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                case Type.LONG:
                    break;
                default:
                    throw new ValidateException("Invalid type for new-array " + pType.getDescriptor());
            }
        }
    }

    @Override
    public List<MatchClass> getReferencedClasses(PatchInstruction instruction) {
        String className = instruction.params[0];

        if (className.equals("*")) {
            return ImmutableList.of();
        }

        Type type = MatchGenerator.getRootType(Type.getType(className));
        if (type.getSort() != Type.OBJECT) {
            return ImmutableList.of();
        }
        return Arrays.asList(new MatchClass(new Ident(type.getInternalName()).getName()));
    }
}
