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
import org.objectweb.asm.tree.*
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction
import uk.co.thinkofdeath.patchtools.patch.ValidateException
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

public class PushIntInstruction : InstructionHandler {
    override fun check(classSet: ClassSet, scope: PatchScope?, instruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (instruction.params.size != 1) {
            return false
        }
        var `val` = 0
        var any = false
        if (instruction.params[0] == "*") {
            any = true
        } else {
            `val` = Integer.parseInt(instruction.params[0])
        }

        if (insn is LdcInsnNode) {
            if (insn.cst is Int) {
                return !(!any && insn.cst as Int != `val`)
            }
        } else if (insn is InsnNode) {
            if (insn.getOpcode() >= Opcodes.ICONST_M1 && insn.getOpcode() <= Opcodes.ICONST_5) {
                val other = insn.getOpcode() - Opcodes.ICONST_M1 - 1
                return !(!any && other != `val`)
            }
        } else if (insn is IntInsnNode) {
            if (insn.getOpcode() == Opcodes.BIPUSH || insn.getOpcode() == Opcodes.SIPUSH) {
                val other = insn.operand
                return !(!any && other != `val`)
            }
        }
        return false
    }

    override fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode): AbstractInsnNode {
        if (instruction.params.size != 1) {
            throw RuntimeException()
        }
        val `val` = Integer.parseInt(instruction.params[0])
        if (`val` >= -1 && `val` <= 5) {
            return InsnNode(Opcodes.ICONST_M1 + `val` + 1)
        }
        if (`val` >= java.lang.Byte.MIN_VALUE && `val` <= java.lang.Byte.MAX_VALUE) {
            return IntInsnNode(Opcodes.BIPUSH, `val`)
        }
        if (`val` >= java.lang.Short.MIN_VALUE && `val` <= java.lang.Short.MAX_VALUE) {
            return IntInsnNode(Opcodes.SIPUSH, `val`)
        }
        return LdcInsnNode(`val`)
    }

    override fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn is LdcInsnNode) {
            if (insn.cst is Int) {
                patch.append("push-int ").append(insn.cst as Int)
                return true
            }
        } else if (insn is InsnNode) {
            if (insn.getOpcode() >= Opcodes.ICONST_M1 && insn.getOpcode() <= Opcodes.ICONST_5) {
                patch.append("push-int ").append(insn.getOpcode() - Opcodes.ICONST_M1 - 1)
                return true
            }
        } else if (insn is IntInsnNode) {
            if (insn.getOpcode() == Opcodes.BIPUSH || insn.getOpcode() == Opcodes.SIPUSH) {
                patch.append("push-int ").append(insn.operand)
                return true
            }
        }
        return false
    }

    override fun validate(instruction: PatchInstruction) {
        if (instruction.params.size != 1) {
            throw ValidateException("Incorrect number of arguments for push-int")
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
