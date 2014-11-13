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
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler
import uk.co.thinkofdeath.patchtools.matching.MatchClass
import uk.co.thinkofdeath.patchtools.matching.MatchGenerator
import uk.co.thinkofdeath.patchtools.patch.Ident
import uk.co.thinkofdeath.patchtools.patch.PatchClass
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction
import uk.co.thinkofdeath.patchtools.patch.ValidateException
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

import java.util.Arrays

public class MultiArrayInstruction : InstructionHandler {

    override fun check(classSet: ClassSet, scope: PatchScope?, instruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is MultiANewArrayInsnNode) {
            return false
        }

        val pType = Type.getType(instruction.params[0])
        val dims = if (instruction.params[1] == "*") -1 else Integer.parseInt(instruction.params[1])

        if (instruction.params[0] == "*") {
            return (dims == -1 || dims == insn.dims)
        }
        return PatchClass.checkTypes(classSet, scope, pType, Type.getType(insn.desc))
            && (dims == -1 || dims == insn.dims)
    }

    override fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode): AbstractInsnNode {
        val nDesc = StringBuilder()
        PatchClass.updatedTypeString(classSet, scope, nDesc, Type.getType(instruction.params[0]))
        val desc = nDesc.toString()
        val dims = Integer.parseInt(instruction.params[1])
        return MultiANewArrayInsnNode(Type.getType(desc).getInternalName(), dims)
    }

    override fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn is MultiANewArrayInsnNode) {
            patch.append("new-array-multi ").append(insn.desc).append(' ').append(insn.dims)
            return true
        }
        return false
    }

    throws(javaClass<ValidateException>())
    override fun validate(instruction: PatchInstruction) {
        if (instruction.params.size != 2) {
            throw ValidateException("Incorrect number of arguments for new-array-multi")
        }

        Utils.validateType(instruction.params[0])

        try {
            if (instruction.params[1] != "*") {
                Integer.parseInt(instruction.params[1])
            }
        } catch (e: NumberFormatException) {
            throw ValidateException("Invalid number " + e.getMessage())
        }


    }

    override fun getReferencedClasses(instruction: PatchInstruction): List<MatchClass> {
        val className = instruction.params[0]

        if (className == "*") {
            return listOf()
        }

        val `type` = MatchGenerator.getRootType(Type.getType(className))
        if (`type`.getSort() != Type.OBJECT) {
            return listOf()
        }
        return Arrays.asList<MatchClass>(MatchClass(Ident(`type`.getInternalName()).name))
    }
}
