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

import com.google.common.base.Joiner
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction
import uk.co.thinkofdeath.patchtools.patch.ValidateException
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

public class PushStringInstruction : InstructionHandler {
    override fun check(classSet: ClassSet, scope: PatchScope?, patchInstruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is LdcInsnNode) {
            return false
        }
        var cst = Joiner.on(' ').join(patchInstruction.params)

        if (insn.cst is String) {
            if (cst == "*") {
                return true
            }

            if (!cst.startsWith("\"") || !cst.endsWith("\"")) {
                return false
            }
            cst = cst.substring(1, cst.length() - 1)
            if (insn.cst != cst) {
                return false
            }
        } else {
            return false
        }
        return true
    }

    override fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode): AbstractInsnNode {
        val cst = Joiner.on(' ').join(instruction.params)
        return LdcInsnNode(Utils.parseConstant(cst))
    }

    override fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is LdcInsnNode || insn.cst !is String) {
            return false
        }
        patch.append("push-string ")
        Utils.printConstant(patch, insn.cst)
        return true
    }

    override fun validate(instruction: PatchInstruction) {
        val cst = Joiner.on(' ').join(instruction.params)
        if (cst == "*") {
            return
        }

        if (!cst.startsWith("\"") || !cst.endsWith("\"")) {
            throw ValidateException("Invalid string")
        }
    }
}
