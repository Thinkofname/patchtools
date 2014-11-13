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
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodNode
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler
import uk.co.thinkofdeath.patchtools.matching.MatchClass
import uk.co.thinkofdeath.patchtools.matching.MatchField
import uk.co.thinkofdeath.patchtools.matching.MatchGenerator
import uk.co.thinkofdeath.patchtools.patch.Ident
import uk.co.thinkofdeath.patchtools.patch.PatchClass
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction
import uk.co.thinkofdeath.patchtools.patch.ValidateException
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

import java.util.ArrayList
import java.util.Arrays

public class FieldInstruction(private val opcode: Int) : InstructionHandler {

    override fun check(classSet: ClassSet, scope: PatchScope?, instruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is FieldInsnNode || insn.getOpcode() != opcode) {
            return false
        }

        val cls = Ident(instruction.params[0])
        var clsName = cls.name
        if (clsName != "*") {
            if (scope != null || !cls.isWeak()) {
                if (cls.isWeak()) {
                    val ptcls = scope!!.getClass(clsName)
                    if (ptcls == null) {
                        // Assume true
                        scope.putClass(classSet.getClassWrapper(insn.owner)!!, clsName)
                        clsName = insn.owner
                    } else {
                        clsName = ptcls.node.name
                    }
                }
                if (clsName != insn.owner) {
                    return false
                }
            }
        }

        val fieldIdent = Ident(instruction.params[1])
        var fieldName = fieldIdent.name
        if (fieldName != "*") {
            if (scope != null || !fieldIdent.isWeak()) {
                if (fieldIdent.isWeak()) {
                    val owner = classSet.getClassWrapper(insn.owner)!!
                    val ptField = scope!!.getField(owner, fieldName, instruction.params[2])
                    if (ptField == null) {
                        // Assume true
                        scope.putField(classSet.getClassWrapper(insn.owner)!!.getField(insn.name, insn.desc)!!, fieldName, instruction.params[2])
                        fieldName = insn.name
                    } else {
                        fieldName = ptField.name
                    }
                }
                if (fieldName != insn.name) {
                    return false
                }
            }
        }

        val patchDesc = Type.getType(instruction.params[2])
        val desc = Type.getType(insn.desc)

        return PatchClass.checkTypes(classSet, scope, patchDesc, desc)
    }

    override fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode): AbstractInsnNode {
        val ownerId = Ident(instruction.params[0])
        var owner = ownerId.name
        if (ownerId.isWeak()) {
            owner = scope.getClass(owner)!!.node.name
        }
        val nameId = Ident(instruction.params[1])
        var name = nameId.name
        if (nameId.isWeak()) {
            val cls = classSet.getClassWrapper(owner)!!
            val wrapper = scope.getField(cls, name, instruction.params[2])
            if (wrapper != null) {
                name = wrapper.name
            }
        }

        val mappedDesc = StringBuilder()
        val desc = Type.getType(instruction.params[2])
        PatchClass.updatedTypeString(classSet, scope, mappedDesc, desc)
        return FieldInsnNode(opcode, owner, name, mappedDesc.toString())
    }

    override fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is FieldInsnNode) {
            return false
        }
        when (insn.getOpcode()) {
            Opcodes.GETFIELD -> patch.append("get-field")
            Opcodes.GETSTATIC -> patch.append("get-static")
            Opcodes.PUTFIELD -> patch.append("put-field")
            Opcodes.PUTSTATIC -> patch.append("put-static")
            else -> throw IllegalArgumentException("Invoke opcode: " + insn.getOpcode())
        }
        patch.append(' ').append(insn.owner).append(' ').append(insn.name).append(' ').append(insn.desc)
        return true
    }

    override fun validate(instruction: PatchInstruction) {
        if (instruction.params.size != 3) {
            throw ValidateException("Incorrect number of arguments for field instruction")
        }
        // First & second param we assume is correct

        Utils.validateType(instruction.params[2])
    }

    override fun getReferencedClasses(instruction: PatchInstruction): List<MatchClass> {
        val classes = ArrayList<MatchClass>()
        val owner = Ident(instruction.params[0])
        if (owner.name != "*") {
            val omc = MatchClass(owner.name)
            classes.add(omc)
        }

        if (instruction.params[2] != "*") {
            val desc = Type.getType(instruction.params[2])

            val rt = MatchGenerator.getRootType(desc)
            if (rt.getSort() == Type.OBJECT) {
                val argCls = MatchClass(Ident(rt.getInternalName()).name)
                if (argCls.name != "*") {
                    classes.add(argCls)
                }
            }
        }
        return classes
    }

    override fun getReferencedFields(instruction: PatchInstruction): List<MatchField> {
        val owner = Ident(instruction.params[0])
        val field = Ident(instruction.params[1])
        if (owner.name != "*" && field.name != "*") {
            val omc = MatchClass(owner.name)
            val mmc = MatchField(omc, field.name, instruction.params[2])

            val desc = Type.getType(instruction.params[2])
            mmc.`type` = desc
            return Arrays.asList<MatchField>(mmc)
        }
        return listOf()
    }
}
