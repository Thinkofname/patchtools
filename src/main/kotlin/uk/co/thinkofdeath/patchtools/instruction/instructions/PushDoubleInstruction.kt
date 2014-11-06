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

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction
import uk.co.thinkofdeath.patchtools.patch.ValidateException
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

public class PushDoubleInstruction : InstructionHandler {
    override fun check(classSet: ClassSet, scope: PatchScope?, instruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (instruction.params.size != 1) {
            return false
        }
        var `val`: Double = 0.0
        var any = false
        if (instruction.params[0] == "*") {
            any = true
        } else {
            `val` = java.lang.Double.parseDouble(instruction.params[0])
        }

        if (insn is LdcInsnNode) {
            if (insn.cst is Double) {
                return !(!any && insn.cst as Double != `val`)
            }
        } else if (insn is InsnNode) {
            if (insn.getOpcode() >= Opcodes.DCONST_0 && insn.getOpcode() <= Opcodes.DCONST_1) {
                val other = (insn.getOpcode() - Opcodes.DCONST_0).toDouble()
                return !(!any && other != `val`)
            }
        }
        return false
    }

    override fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode): AbstractInsnNode {
        if (instruction.params.size != 1) {
            throw RuntimeException()
        }
        val `val` = java.lang.Double.parseDouble(instruction.params[0])
        if (`val` >= 0 && `val` <= 1 && Math.floor(`val`) == `val`) {
            return InsnNode((Opcodes.DCONST_0.toDouble() + `val`).toInt())
        }
        return LdcInsnNode(`val`)
    }

    override fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn is LdcInsnNode) {
            if (insn.cst is Double) {
                patch.append("push-double ").append(insn.cst as Double)
                return true
            }
        } else if (insn is InsnNode) {
            if (insn.getOpcode() >= Opcodes.DCONST_0 && insn.getOpcode() <= Opcodes.DCONST_1) {
                patch.append("push-double ").append(insn.getOpcode() - Opcodes.DCONST_0)
                return true
            }
        }
        return false
    }

    override fun validate(instruction: PatchInstruction) {
        if (instruction.params.size != 1) {
            throw ValidateException("Incorrect number of arguments for push-double")
        }

        try {
            if (instruction.params[0] != "*") {
                java.lang.Double.parseDouble(instruction.params[0])
            }
        } catch (e: NumberFormatException) {
            throw ValidateException("Invalid number " + e.getMessage())
        }

    }
}
