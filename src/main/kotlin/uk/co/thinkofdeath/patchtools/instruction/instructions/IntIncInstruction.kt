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
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.tree.MethodNode
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction
import uk.co.thinkofdeath.patchtools.patch.ValidateException
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

public class IntIncInstruction : InstructionHandler {
    override fun check(classSet: ClassSet, scope: PatchScope?, instruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is IincInsnNode) {
            return false
        }
        var `var` = 0
        var any = false
        if (instruction.params[0] == "*") {
            any = true
        } else {
            `var` = Integer.parseInt(instruction.params[0])
        }
        var other = insn.`var`
        if (any || `var` == other) {
            var `val` = 0
            any = false
            if (instruction.params[1] == "*") {
                any = true
            } else {
                `val` = Integer.parseInt(instruction.params[1])
            }
            other = insn.incr
            return !(!any && other != `val`)
        }
        return false
    }

    override fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode): AbstractInsnNode {
        val `var` = Integer.parseInt(instruction.params[0])
        val `val` = Integer.parseInt(instruction.params[1])
        return IincInsnNode(`var`, `val`)
    }

    override fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn is IincInsnNode) {
            patch.append("inc-int ").append(insn.`var`).append(' ').append(insn.incr)
            return true
        }
        return false
    }

    override fun validate(instruction: PatchInstruction) {
        if (instruction.params.size != 2) {
            throw ValidateException("Incorrect number of arguments for int-inc")
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

    }
}
