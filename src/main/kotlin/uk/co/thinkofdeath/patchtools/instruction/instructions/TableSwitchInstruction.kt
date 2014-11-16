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
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler
import uk.co.thinkofdeath.patchtools.patch.Ident
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction
import uk.co.thinkofdeath.patchtools.patch.ValidateException
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

public class TableSwitchInstruction : InstructionHandler {
    override fun check(classSet: ClassSet, scope: PatchScope?, instruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is TableSwitchInsnNode) {
            return false
        }

        if (!Utils.equalOrWild(instruction.params[0], insn.min) || !Utils.equalOrWild(instruction.params[1], insn.max) || !Utils.checkOrSetLabel(scope, method, instruction.params[2], insn.dflt)) {
            return false
        }

        if (insn.labels.size() < instruction.meta.size()) return false

        for (i in instruction.meta.indices) {
            if (!Utils.checkOrSetLabel(scope, method, instruction.meta.get(i), insn.labels.get(i))) {
                return false
            }
        }
        return true
    }

    override fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode): AbstractInsnNode {
        val insnNode = TableSwitchInsnNode(Integer.parseInt(instruction.params[0]), Integer.parseInt(instruction.params[1]), Utils.getLabel(scope, method, instruction.params[2]))
        instruction.meta
            .map { Utils.getLabel(scope, method, it) }
            .forEach { insnNode.labels.add(it) }
        return insnNode
    }

    override fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is TableSwitchInsnNode) {
            return false
        }

        patch
            .append("switch-table ")
            .append(insn.min)
            .append(' ')
            .append(insn.max)
            .append(' ')
            .append('~')
            .append(Utils.printLabel(method, insn.dflt))
            .append('\n')
        for (label in insn.labels) {
            patch
                .append("    ")
                .append("    ")
                .append("    ")
                .append("# ~")
                .append(Utils.printLabel(method, label))
                .append('\n')
        }
        return true
    }

    override fun validate(instruction: PatchInstruction) {
        if (instruction.params.size != 3) {
            throw ValidateException("Incorrect number of arguments for switch-table")
        }
        try {
            if (instruction.params[0] != "*") {
                Integer.parseInt(instruction.params[0])
            }
            if (instruction.params[1] != "*") {
                Integer.parseInt(instruction.params[1])
            }
        } catch (e: NumberFormatException) {
            throw ValidateException("Invalid number " + e.getMessage())
        }


        if (instruction.params[2] != "*" && !Ident(instruction.params[2]).isWeak()) {
            throw ValidateException("Non-weak label ")
        }

        if (instruction.meta
            .filter { it != "*" }
            .any { !Ident(it).isWeak() }) {
            throw ValidateException("Non-weak label")
        }
    }
}
