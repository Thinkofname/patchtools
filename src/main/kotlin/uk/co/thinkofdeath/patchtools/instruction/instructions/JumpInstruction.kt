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
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.MethodNode
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler
import uk.co.thinkofdeath.patchtools.patch.Ident
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction
import uk.co.thinkofdeath.patchtools.patch.ValidateException
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

public class JumpInstruction(private val opcode: Int) : InstructionHandler {

    override fun check(classSet: ClassSet, scope: PatchScope?, instruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean {
        return !(insn !is JumpInsnNode || insn.getOpcode() != opcode)
            && Utils.checkOrSetLabel(scope, method, instruction.params[0], insn.label)
    }

    override fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode): AbstractInsnNode {
        return JumpInsnNode(opcode, Utils.getLabel(scope, method, instruction.params[0]))
    }

    override fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is JumpInsnNode || insn.getOpcode() != opcode) {
            return false
        }
        when (opcode) {
            Opcodes.IFEQ -> patch.append("if-zero")
            Opcodes.IFNE -> patch.append("if-not-zero")
            Opcodes.IFLT -> patch.append("if-less-zero")
            Opcodes.IFGE -> patch.append("if-greater-equal-zero")
            Opcodes.IFGT -> patch.append("if-greater-zero")
            Opcodes.IFLE -> patch.append("if-less-equal-zero")
            Opcodes.IF_ICMPEQ -> patch.append("if-equal-int")
            Opcodes.IF_ICMPNE -> patch.append("if-not-equal-int")
            Opcodes.IF_ICMPLT -> patch.append("if-less-int")
            Opcodes.IF_ICMPGE -> patch.append("if-greater-equal-int")
            Opcodes.IF_ICMPGT -> patch.append("if-greater-int")
            Opcodes.IF_ICMPLE -> patch.append("if-less-equal-int")
            Opcodes.IF_ACMPEQ -> patch.append("if-equal-object")
            Opcodes.IF_ACMPNE -> patch.append("if-not-equal-object")
            Opcodes.GOTO -> patch.append("goto")
            Opcodes.JSR -> patch.append("jsr")
            Opcodes.IFNULL -> patch.append("if-null")
            Opcodes.IFNONNULL -> patch.append("if-not-null")
            else -> throw UnsupportedOperationException("op:" + opcode)
        }
        patch.append(' ').append('~').append(Utils.printLabel(method, insn.label))
        return true
    }

    override fun validate(instruction: PatchInstruction) {
        if (instruction.params.size != 1) {
            throw ValidateException("Incorrect number of arguments for jump")
        }
        if (instruction.params[0] != "*" && !Ident(instruction.params[0]).isWeak()) {
            throw ValidateException("Non-weak label")
        }
    }
}
