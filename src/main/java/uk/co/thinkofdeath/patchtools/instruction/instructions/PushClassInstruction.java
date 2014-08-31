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

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.Ident;
import uk.co.thinkofdeath.patchtools.patch.PatchClass;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;

public class PushClassInstruction implements InstructionHandler {
    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof LdcInsnNode)) {
            return false;
        }
        if (patchInstruction.params.length != 1) {
            return false;
        }
        LdcInsnNode ldcInsnNode = (LdcInsnNode) insn;
        String className = patchInstruction.params[0];

        if (ldcInsnNode.cst instanceof Type) {
            if (className.equals("*")) {
                return true;
            }

            Type type = (Type) ldcInsnNode.cst;

            Ident cls = new Ident(className);
            String clsName = cls.getName();
            if (!clsName.equals("*")) {
                if (scope != null || !cls.isWeak()) {
                    if (cls.isWeak()) {
                        ClassWrapper ptcls = scope.getClass(clsName);
                        if (ptcls == null) { // Assume true
                            scope.putClass(classSet.getClassWrapper(type.getInternalName()), clsName);
                            clsName = type.getInternalName();
                        } else {
                            clsName = ptcls.getNode().name;
                        }
                    }
                    if (!clsName.equals(type.getInternalName())) {
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
        return false;
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        StringBuilder nDesc = new StringBuilder();
        PatchClass.updatedTypeString(classSet, scope, nDesc, Type.getType("L" + instruction.params[0] + ";"));
        String desc = nDesc.toString();
        return new LdcInsnNode(desc);
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof LdcInsnNode) || !(((LdcInsnNode) insn).cst instanceof Type)) {
            return false;
        }
        patch.append("push-class ")
            .append(((Type) ((LdcInsnNode) insn).cst).getInternalName());
        return true;
    }
}
