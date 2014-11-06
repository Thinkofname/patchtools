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

import com.google.common.collect.ImmutableList
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
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

public class TypeInstruction(private val opcode: Int) : InstructionHandler {

    override fun check(classSet: ClassSet, scope: PatchScope?, patchInstruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is TypeInsnNode || insn.getOpcode() != opcode) {
            return false
        }
        val className = patchInstruction.params[0]

        if (className == "*") {
            return true
        }

        val `type` = insn.desc
        return PatchClass.checkTypes(classSet, scope, Type.getObjectType(className), Type.getObjectType(`type`))
    }

    override fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode): AbstractInsnNode {
        val nDesc = StringBuilder()
        PatchClass.updatedTypeString(classSet, scope, nDesc, Type.getObjectType(instruction.params[0]))
        val desc = Type.getType(nDesc.toString())
        return TypeInsnNode(opcode, desc.getInternalName())
    }

    override fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is TypeInsnNode || insn.getOpcode() != opcode) {
            return false
        }
        when (opcode) {
            Opcodes.NEW -> patch.append("new")
            Opcodes.INSTANCEOF -> patch.append("instance-of")
            Opcodes.CHECKCAST -> patch.append("check-cast")
        }
        val `type` = insn.desc
        patch.append(" ").append(`type`)
        return true
    }

    throws(javaClass<ValidateException>())
    override fun validate(instruction: PatchInstruction) {
        if (instruction.params.size != 1) {
            throw ValidateException("Incorrect number of arguments for type instruction")
        }

        Utils.validateObjectType(instruction.params[0])
    }

    override fun getReferencedClasses(instruction: PatchInstruction): List<MatchClass> {
        val className = instruction.params[0]

        if (className == "*") {
            return ImmutableList.of<MatchClass>()
        }

        val `type` = MatchGenerator.getRootType(Type.getObjectType(className))
        if (`type`.getSort() != Type.OBJECT) {
            return ImmutableList.of<MatchClass>()
        }
        return Arrays.asList<MatchClass>(MatchClass(Ident(`type`.getInternalName()).name))
    }
}
