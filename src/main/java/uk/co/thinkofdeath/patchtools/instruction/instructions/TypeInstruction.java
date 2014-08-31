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
import org.objectweb.asm.tree.TypeInsnNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.Ident;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;

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
        if (patchInstruction.params.length != 1) {
            return false;
        }
        TypeInsnNode typeInsnNode = (TypeInsnNode) insn;
        String className = patchInstruction.params[0];

        if (className.equals("*")) {
            return true;
        }

        String type = typeInsnNode.desc;

        Ident cls = new Ident(className);
        String clsName = cls.getName();
        if (!clsName.equals("*")) {
            if (scope != null || !cls.isWeak()) {
                if (cls.isWeak()) {
                    ClassWrapper ptcls = scope.getClass(clsName);
                    if (ptcls == null) { // Assume true
                        scope.putClass(classSet.getClassWrapper(type), clsName);
                        clsName = type;
                    } else {
                        clsName = ptcls.getNode().name;
                    }
                }
                if (!clsName.equals(type)) {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        return new TypeInsnNode(opcode, instruction.params[0]);
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
        patch
            .append(" ")
            .append(((TypeInsnNode) insn).desc);
        return true;
    }
}
