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
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.IntInsnNode
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

public class ArrayInstruction : InstructionHandler {

    override fun check(classSet: ClassSet, scope: PatchScope?, instruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (instruction.params[0] == "*") {
            return (insn is TypeInsnNode && insn.getOpcode() == Opcodes.ANEWARRAY) || (insn is IntInsnNode && insn.getOpcode() == Opcodes.NEWARRAY)
        }

        val pType = Type.getType(instruction.params[0])

        if (pType.getSort() == Type.OBJECT || pType.getSort() == Type.ARRAY) {
            if (insn !is TypeInsnNode) {
                return false
            }
            val `type` = insn.desc
            return PatchClass.checkTypes(classSet, scope, pType, Type.getObjectType(`type`))
        } else {
            if (insn !is IntInsnNode) {
                return false
            }
            val `type`: Int
            when (pType.getSort()) {
                Type.BOOLEAN -> `type` = 4
                Type.CHAR -> `type` = 5
                Type.FLOAT -> `type` = 6
                Type.DOUBLE -> `type` = 7
                Type.BYTE -> `type` = 8
                Type.SHORT -> `type` = 9
                Type.INT -> `type` = 10
                Type.LONG -> `type` = 11
                else -> `type` = -1
            }
            return insn.operand == `type`
        }
    }

    override fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode): AbstractInsnNode {
        val pType = Type.getType(instruction.params[0])

        if (pType.getSort() == Type.OBJECT || pType.getSort() == Type.ARRAY) {
            val nDesc = StringBuilder()
            PatchClass.updatedTypeString(classSet, scope, nDesc, pType)
            val desc = nDesc.toString()
            return TypeInsnNode(Opcodes.ANEWARRAY, Type.getType(desc).getInternalName())
        } else {
            var `type` = -1
            when (pType.getSort()) {
                Type.BOOLEAN -> `type` = 4
                Type.CHAR -> `type` = 5
                Type.FLOAT -> `type` = 6
                Type.DOUBLE -> `type` = 7
                Type.BYTE -> `type` = 8
                Type.SHORT -> `type` = 9
                Type.INT -> `type` = 10
                Type.LONG -> `type` = 11
            }
            return IntInsnNode(Opcodes.NEWARRAY, `type`)
        }
    }

    override fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if ((insn is TypeInsnNode && insn.getOpcode() == Opcodes.ANEWARRAY)) {
            val `type` = insn.desc
            patch.append("new-array ").append(Type.getObjectType(`type`).getDescriptor())
            return true
        }
        if (insn is IntInsnNode && insn.getOpcode() == Opcodes.NEWARRAY) {
            val `type`: String
            when (insn.operand) {
                4 -> `type` = Type.BOOLEAN_TYPE.getDescriptor()
                5 -> `type` = Type.CHAR_TYPE.getDescriptor()
                6 -> `type` = Type.FLOAT_TYPE.getDescriptor()
                7 -> `type` = Type.DOUBLE_TYPE.getDescriptor()
                8 -> `type` = Type.BYTE_TYPE.getDescriptor()
                9 -> `type` = Type.SHORT_TYPE.getDescriptor()
                10 -> `type` = Type.INT_TYPE.getDescriptor()
                11 -> `type` = Type.LONG_TYPE.getDescriptor()
                else -> `type` = "error"
            }
            patch.append("new-array ").append(`type`)
            return true
        }
        return false
    }

    override fun validate(instruction: PatchInstruction) {
        if (instruction.params.size != 1) {
            throw ValidateException("Incorrect number of arguments for new-array")
        }

        Utils.validateType(instruction.params[0])

        val pType = Type.getType(instruction.params[0])

        if (pType.getSort() != Type.OBJECT && pType.getSort() != Type.ARRAY) {
            when (pType.getSort()) {
                Type.BOOLEAN, Type.CHAR, Type.FLOAT, Type.DOUBLE, Type.BYTE, Type.SHORT, Type.INT, Type.LONG -> {
                }
                else -> throw ValidateException("Invalid type for new-array " + pType.getDescriptor())
            }
        }
    }

    override fun getReferencedClasses(instruction: PatchInstruction): List<MatchClass> {
        val className = instruction.params[0]

        if (className == "*") {
            return listOf()
        }

        val type = MatchGenerator.getRootType(Type.getType(className))
        if (type.getSort() != Type.OBJECT) {
            return listOf()
        }
        return Arrays.asList<MatchClass>(MatchClass(Ident(type.getInternalName()).name))
    }
}
