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

package uk.co.thinkofdeath.patchtools.instruction.instructions

import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TryCatchBlockNode
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler
import uk.co.thinkofdeath.patchtools.patch.Ident
import uk.co.thinkofdeath.patchtools.patch.PatchClass
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction
import uk.co.thinkofdeath.patchtools.patch.ValidateException
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

public class TryCatchInstruction : InstructionHandler {
    override fun check(classSet: ClassSet, scope: PatchScope?, instruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean {
        return match(classSet, scope, instruction, method) != null
    }

    override fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode): AbstractInsnNode {
        throw RuntimeException()
    }

    override fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn.getOpcode() != -55) return false
        for (tryNode in method.tryCatchBlocks) {
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
                .append(tryNode.`type`).append('\n')
        }
        return true
    }

    override fun validate(instruction: PatchInstruction) {
        if (instruction.params.size != 4) {
            throw ValidateException("Incorrect number of arguments for try-catch")
        }
        if (instruction.params[0] != "*" && !Ident(instruction.params[0]).isWeak()) {
            throw ValidateException("Non-weak label")
        }
        if (instruction.params[1] != "*" && !Ident(instruction.params[0]).isWeak()) {
            throw ValidateException("Non-weak label")
        }
        if (instruction.params[2] != "*" && !Ident(instruction.params[0]).isWeak()) {
            throw ValidateException("Non-weak label")
        }
        Utils.validateObjectType(instruction.params[3])
    }

    class object {

        public fun match(classSet: ClassSet, scope: PatchScope?, instruction: PatchInstruction, method: MethodNode): TryCatchBlockNode? {
            for (tryNode in method.tryCatchBlocks) {
                if (!Utils.checkOrSetLabel(scope, method, instruction.params[0], tryNode.start)) {
                    continue
                }
                if (!Utils.checkOrSetLabel(scope, method, instruction.params[1], tryNode.end)) {
                    continue
                }
                if (!Utils.checkOrSetLabel(scope, method, instruction.params[2], tryNode.handler)) {
                    continue
                }
                val `type` = if (instruction.params[3] == "null") null else instruction.params[3]

                if (`type` == null || tryNode.`type` == null) {
                    if (`type` != null || tryNode.`type` != null) {
                        continue
                    }
                } else {
                    if (!PatchClass.checkTypes(classSet, scope, Type.getObjectType(`type`), Type.getObjectType(tryNode.`type`))) {
                        continue
                    }
                }
                return tryNode
            }
            return null
        }

        public fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode, labels: Map<LabelNode, LabelNode>) {
            val `type` = StringBuilder()
            PatchClass.updatedTypeString(classSet, scope, `type`, Type.getType("L" + instruction.params[3] + ";"))
            val tryNode = TryCatchBlockNode(labels.get(Utils.getLabel(scope, method, instruction.params[0])), labels.get(Utils.getLabel(scope, method, instruction.params[1])), labels.get(Utils.getLabel(scope, method, instruction.params[2])), Type.getType(`type`.toString()).getInternalName())
            method.tryCatchBlocks.add(tryNode)
        }
    }
}
