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

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.MethodNode
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler
import uk.co.thinkofdeath.patchtools.patch.Ident
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction
import uk.co.thinkofdeath.patchtools.patch.ValidateException
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

public class LookupSwitchInstruction : InstructionHandler {
    override fun check(classSet: ClassSet, scope: PatchScope?, instruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is LookupSwitchInsnNode) {
            return false
        }

        if (!Utils.checkOrSetLabel(scope, method, instruction.params[0], insn.dflt)) {
            return false
        }

        if (insn.labels.size() < instruction.meta.size()) return false

        for (i in instruction.meta.indices) {
            val parts = instruction.meta.get(i).split(":")
            val key = parts[0].trim()
            val label = parts[1].trim()
            if (!Utils.equalOrWild(key, insn.keys.get(i)) || !Utils.checkOrSetLabel(scope, method, label, insn.labels.get(i))) {
                return false
            }
        }
        return true
    }

    override fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode): AbstractInsnNode {
        if (instruction.params.size != 1) {
            throw RuntimeException("Incorrect number of arguments for switch-table")
        }
        val insnNode = LookupSwitchInsnNode(Utils.getLabel(scope, method, instruction.params[0]), null, null)
        instruction.meta
            .map { it.split(":")[1].trim() }
            .map { Utils.getLabel(scope, method, it) }
            .forEach { insnNode.labels.add(it) }
        instruction.meta
            .map { it.split(":")[0].trim() }
            .map { it.toInt() }
            .forEach { insnNode.keys.add(it) }
        return insnNode
    }

    override fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is LookupSwitchInsnNode) {
            return false
        }

        patch.append("switch-lookup ").append('~').append(Utils.printLabel(method, insn.dflt)).append('\n')
        for (i in insn.labels.indices) {
            val label = insn.labels.get(i)
            val key = insn.keys.get(i)
            patch
                .append("    ")
                .append("    ")
                .append("    ")
                .append(Integer.toString(key))
                .append(':')
                .append('~')
                .append(Utils.printLabel(method, label))
                .append('\n')
        }
        patch.append("    ").append("    ").append(".end-switch-lookup")
        return true
    }

    override fun validate(instruction: PatchInstruction) {
        if (instruction.params.size != 1) {
            throw ValidateException("Incorrect number of arguments for switch-lookup")
        }

        if (instruction.params[0] != "*" && !Ident(instruction.params[0]).isWeak()) {
            throw ValidateException("Non-weak label")
        }

        if (instruction.meta
            .map { it.split(":")[1].trim() }
            .filter { it != "*" }
            .any { !Ident(it).isWeak() }
        ) {
            throw ValidateException("Non-weak label")
        }
        try {
            instruction.meta
                .map { it.split(":")[0].trim() }
                .filter { it != "*" }
                .forEach { it.toInt() }
        } catch (e: NumberFormatException) {
            throw ValidateException("Invalid number " + e.getMessage())
        }

    }
}
