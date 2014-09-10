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

public class TypeInstruction implements InstructionHandler {

    private final int opcode;

    public TypeInstruction(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof TypeInsnNode) || insn.getOpcode() != opcode) {
            return false;
        }
        TypeInsnNode typeInsnNode = (TypeInsnNode) insn;
        String className = patchInstruction.params[0];

        if (className.equals("*")) {
            return true;
        }

        String type = typeInsnNode.desc;
        return PatchClass.checkTypes(classSet, scope, Type.getObjectType(className), Type.getObjectType(type));
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        StringBuilder nDesc = new StringBuilder();
        PatchClass.updatedTypeString(classSet, scope, nDesc, Type.getObjectType(instruction.params[0]));
        Type desc = Type.getType(nDesc.toString());
        return new TypeInsnNode(opcode, desc.getInternalName());
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof TypeInsnNode) || insn.getOpcode() != opcode) {
            return false;
        }
        switch (opcode) {
            case Opcodes.NEW:
                patch.append("new");
                break;
            case Opcodes.INSTANCEOF:
                patch.append("instance-of");
                break;
            case Opcodes.CHECKCAST:
                patch.append("check-cast");
                break;
        }
        String type = ((TypeInsnNode) insn).desc;
        patch
            .append(" ")
            .append(type);
        return true;
    }

    @Override
    public void validate(PatchInstruction instruction) throws ValidateException {
        if (instruction.params.length != 1) {
            throw new ValidateException("Incorrect number of arguments for type instruction");
        }

        Utils.validateObjectType(instruction.params[0]);
    }

    @Override
    public List<MatchClass> getReferencedClasses(PatchInstruction instruction) {
        String className = instruction.params[0];

        if (className.equals("*")) {
            return ImmutableList.of();
        }

        Type type = MatchGenerator.getRootType(Type.getObjectType(className));
        if (type.getSort() != Type.OBJECT) {
            return ImmutableList.of();
        }
        return Arrays.asList(new MatchClass(new Ident(type.getInternalName()).getName()));
    }
}
