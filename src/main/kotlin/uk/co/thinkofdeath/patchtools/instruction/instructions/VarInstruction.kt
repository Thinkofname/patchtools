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
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction
import uk.co.thinkofdeath.patchtools.patch.ValidateException
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

public class VarInstruction(private val opcode: Int) : InstructionHandler {

    override fun check(classSet: ClassSet, scope: PatchScope?, instruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is VarInsnNode || insn.getOpcode() != opcode) {
            return false
        }
        var index = 0
        var any = false
        if (instruction.params[0] == "*") {
            any = true
        } else {
            index = Integer.parseInt(instruction.params[0])
        }

        val other = insn.`var`
        return !(!any && other != index)
    }

    override fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode): AbstractInsnNode {
        return VarInsnNode(opcode, Integer.parseInt(instruction.params[0]))
    }

    override fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn is VarInsnNode && insn.getOpcode() == opcode) {
            when (insn.getOpcode()) {
                Opcodes.ALOAD -> patch.append("load-object")
                Opcodes.ILOAD -> patch.append("load-int")
                Opcodes.LLOAD -> patch.append("load-long")
                Opcodes.FLOAD -> patch.append("load-float")
                Opcodes.DLOAD -> patch.append("load-double")
                Opcodes.ASTORE -> patch.append("store-object")
                Opcodes.ISTORE -> patch.append("store-int")
                Opcodes.LSTORE -> patch.append("store-long")
                Opcodes.FSTORE -> patch.append("store-float")
                Opcodes.DSTORE -> patch.append("store-double")
                Opcodes.RET -> patch.append("ret")
            }
            patch.append(' ').append(insn.`var`)
            return true
        }
        return false
    }

    override fun validate(instruction: PatchInstruction) {
        if (instruction.params.size != 1) {
            throw ValidateException("Incorrect number of arguments for var instruction")
        }
        try {
            if (instruction.params[0] != "*") {
                Integer.parseInt(instruction.params[0])
            }
        } catch (e: NumberFormatException) {
            throw ValidateException("Invalid number " + e.getMessage())
        }

    }
}
