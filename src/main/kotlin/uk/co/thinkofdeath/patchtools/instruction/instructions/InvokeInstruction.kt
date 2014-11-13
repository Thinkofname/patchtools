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
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler
import uk.co.thinkofdeath.patchtools.matching.MatchClass
import uk.co.thinkofdeath.patchtools.matching.MatchGenerator
import uk.co.thinkofdeath.patchtools.matching.MatchMethod
import uk.co.thinkofdeath.patchtools.patch.Ident
import uk.co.thinkofdeath.patchtools.patch.PatchClass
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction
import uk.co.thinkofdeath.patchtools.patch.ValidateException
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

import java.util.ArrayList
import java.util.Arrays

public class InvokeInstruction(private val opcode: Int) : InstructionHandler {

    override fun check(classSet: ClassSet, scope: PatchScope?, instruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is MethodInsnNode || insn.getOpcode() != opcode) {
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

        val methodIdent = Ident(instruction.params[1])
        var methodName = methodIdent.name
        if (methodName != "*") {
            if (scope != null || !methodIdent.isWeak()) {
                if (methodIdent.isWeak()) {
                    val owner = classSet.getClassWrapper(insn.owner)!!
                    val ptMethod = scope!!.getMethod(owner, methodName, instruction.params[2])
                    if (ptMethod == null) {
                        // Assume true
                        scope.putMethod(classSet.getClassWrapper(insn.owner)!!.getMethod(insn.name, insn.desc)!!, methodName, instruction.params[2])
                        methodName = insn.name
                    } else {
                        methodName = ptMethod.name
                    }
                }
                if (methodName != insn.name) {
                    return false
                }
            }
        }

        val patchDesc = Type.getMethodType(instruction.params[2])
        val desc = Type.getMethodType(insn.desc)

        if (desc.getArgumentTypes().size != patchDesc.getArgumentTypes().size) {
            return false
        }

        for (i in 0..patchDesc.getArgumentTypes().size - 1) {
            val pt = patchDesc.getArgumentTypes()[i]
            val t = desc.getArgumentTypes()[i]

            if (!PatchClass.checkTypes(classSet, scope, pt, t)) {
                return false
            }
        }
        return PatchClass.checkTypes(classSet, scope, patchDesc.getReturnType(), desc.getReturnType())
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
            name = scope.getMethod(cls, name, instruction.params[2])!!.name
        }

        val mappedDesc = StringBuilder("(")
        val desc = Type.getMethodType(instruction.params[2])
        for (`type` in desc.getArgumentTypes()) {
            PatchClass.updatedTypeString(classSet, scope, mappedDesc, `type`)
        }
        mappedDesc.append(")")
        PatchClass.updatedTypeString(classSet, scope, mappedDesc, desc.getReturnType())
        return MethodInsnNode(opcode, owner, name, mappedDesc.toString(), opcode == Opcodes.INVOKEINTERFACE)
    }

    override fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        if (insn !is MethodInsnNode) {
            return false
        }
        patch.append("invoke-")
        when (insn.getOpcode()) {
            Opcodes.INVOKESTATIC -> patch.append("static")
            Opcodes.INVOKEVIRTUAL -> patch.append("virtual")
            Opcodes.INVOKESPECIAL -> patch.append("special")
            Opcodes.INVOKEINTERFACE -> patch.append("interface")
            else -> throw IllegalArgumentException("Invoke opcode: " + insn.getOpcode())
        }
        patch
            .append(' ')
            .append(insn.owner)
            .append(' ')
            .append(insn.name)
            .append(' ')
            .append(insn.desc)
        return true
    }


    override fun validate(instruction: PatchInstruction) {
        if (instruction.params.size != 3) {
            throw ValidateException("Incorrect number of arguments for invoke instruction")
        }
        // First & second param we assume is correct

        Utils.validateMethodType(instruction.params[2])
    }

    override fun getReferencedClasses(instruction: PatchInstruction): List<MatchClass> {
        if (instruction.params.size != 3) {
            throw RuntimeException("Incorrect number of arguments for invoke")
        }
        val classes = ArrayList<MatchClass>()
        val owner = Ident(instruction.params[0])

        if (owner.name != "*") {
            val omc = MatchClass(owner.name)
            classes.add(omc)
        }
        val desc = Type.getMethodType(instruction.params[2])

        for (`type` in desc.getArgumentTypes()) {
            val rt = MatchGenerator.getRootType(`type`)
            if (rt.getSort() == Type.OBJECT) {
                val argCls = MatchClass(Ident(rt.getInternalName()).name)
                if (argCls.name != "*") {
                    classes.add(argCls)
                }
            }
        }
        val `type` = desc.getReturnType()
        val rt = MatchGenerator.getRootType(`type`)
        if (rt.getSort() == Type.OBJECT) {
            val argCls = MatchClass(Ident(rt.getInternalName()).name)
            if (argCls.name != "*") {
                classes.add(argCls)
            }
        }
        return classes
    }

    override fun getReferencedMethods(instruction: PatchInstruction): List<MatchMethod> {
        if (instruction.params.size != 3) {
            throw RuntimeException("Incorrect number of arguments for invoke")
        }
        val owner = Ident(instruction.params[0])
        val method = Ident(instruction.params[1])


        if (owner.name != "*" && method.name != "*") {
            val omc = MatchClass(owner.name)
            val mmc = MatchMethod(omc, method.name, instruction.params[2])

            val desc = Type.getMethodType(instruction.params[2])

            for (`type` in desc.getArgumentTypes()) {
                mmc.addArgument(`type`)
            }
            val `type` = desc.getReturnType()
            mmc.setReturn(`type`)
            return Arrays.asList<MatchMethod>(mmc)
        }
        return ImmutableList.of<MatchMethod>()
    }
}
