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
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.PatchClass;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.util.Map;

public class TryCatchInstruction implements InstructionHandler {
    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method, AbstractInsnNode insn) {
        return instruction.params.length == 4 && match(classSet, scope, instruction, method) != null;
    }

    public static TryCatchBlockNode match(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        for (TryCatchBlockNode tryNode : method.tryCatchBlocks) {
            if (!Utils.checkOrSetLabel(scope, method, instruction.params[0], tryNode.start)) {
                continue;
            }
            if (!Utils.checkOrSetLabel(scope, method, instruction.params[1], tryNode.end)) {
                continue;
            }
            if (!Utils.checkOrSetLabel(scope, method, instruction.params[2], tryNode.handler)) {
                continue;
            }
            String type = instruction.params[3].equals("null") ? null : instruction.params[3];

            if (type == null || tryNode.type == null) {
                if (type != null || tryNode.type != null) {
                    continue;
                }
            } else {
                if (!PatchClass.checkTypes(classSet, scope,
                    Type.getType("L" + type + ";"), Type.getType("L" + tryNode.type + ";"))) {
                    continue;
                }
            }
            return tryNode;
        }
        return null;
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        return null;
    }

    public static void create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method, Map<LabelNode, LabelNode> labels) {
        if (instruction.params.length != 4) {
            throw new RuntimeException("try catch, invalid number of arguments");
        }
        StringBuilder type = new StringBuilder();
        PatchClass.updatedTypeString(classSet, scope, type, Type.getType("L" + instruction.params[3] + ";"));
        TryCatchBlockNode tryNode = new TryCatchBlockNode(
            labels.get(Utils.getLabel(scope, method, instruction.params[0])),
            labels.get(Utils.getLabel(scope, method, instruction.params[1])),
            labels.get(Utils.getLabel(scope, method, instruction.params[2])),
            Type.getType(type.toString()).getInternalName()
        );
        method.tryCatchBlocks.add(tryNode);
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (insn != null) return false;
        for (TryCatchBlockNode tryNode : method.tryCatchBlocks) {
            patch.append("        .try-catch ")
                .append('~')
                .append(Utils.printLabel(method, tryNode.start))
                .append(' ')
                .append('~')
                .append(Utils.printLabel(method, tryNode.end))
                .append(' ')
                .append('~')
                .append(Utils.printLabel(method, tryNode.handler))
                .append(' ')
                .append(tryNode.type)
                .append('\n');
        }
        return true;
    }
}
