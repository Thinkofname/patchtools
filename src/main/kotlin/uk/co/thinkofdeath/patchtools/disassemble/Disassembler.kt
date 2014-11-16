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

package uk.co.thinkofdeath.patchtools.disassemble

import org.objectweb.asm.tree.*
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet
import org.objectweb.asm.Opcodes
import uk.co.thinkofdeath.patchtools.instruction.instructions.Utils
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.Instructions
import uk.co.thinkofdeath.patchtools.patch.modifierAccess
import uk.co.thinkofdeath.patchtools.patch.classModifiers
import java.util.Comparator
import org.objectweb.asm.Type

public class Disassembler(private val classSet: ClassSet) {

    private val imports = hashMapOf<String, String>()

    public fun disassemble(cls: String): String {
        val classWrapper = classSet.getClassWrapper(cls)!!

        val patch = StringBuilder("\n")

        val node = classWrapper.node

        appendModifiers(patch, node.access and classModifiers)
        patch.append("class ")
            .append(node.name.replace('/', '.'))
            .append(' ')
        imports[node.name.substring(node.name.lastIndexOf('/') + 1)] = node.name.replace('/', '.')

        if (node.superName != null) {
            patch.append("extends ")
                .append(tryImport(node.superName))
                .append(' ')
        }

        if (node.interfaces.size > 0) {
            patch.append(if ((node.access and Opcodes.ACC_INTERFACE) != 0) "extends " else "implements ")
            var i = 0
            for (inter in node.interfaces) {
                patch
                    .append(tryImport(inter))
                    .append(if (i == node.interfaces.size - 1) " " else ", ")

            }
        }
        patch.append("{\n")

        node.fields.forEach {

            patch.append("    ")
            appendModifiers(patch, it.access)

            val desc = Type.getType(it.desc)
            printType(patch, desc)
            patch.append(it.name)
            if (it.value != null) {
                patch.append(" = ")
                Utils.printConstant(patch, it.value)
            }
            patch.append(";\n")
        }

        patch.append('\n')

        node.methods.forEach {

            patch.append("    ")
            appendModifiers(patch, it.access)

            val desc = Type.getMethodType(it.desc)
            printType(patch, desc.getReturnType())
            patch.append(it.name)
                .append("(")

            val args = desc.getArgumentTypes()
            var i = 0
            for (arg in args) {
                printType(patch, arg)
                patch.append("arg$i")
                if (i != args.size - 1) {
                    patch.append(", ")
                }
            }
            patch.append(") ")


            patch.append("{\n")

            Instruction.TRY_CATCH.handler!!.print(
                Instruction.TRY_CATCH,
                patch,
                it,
                InsnNode(-55)
            )

            val m = it
            it.instructions.toArray()
                .filter { it !is LineNumberNode }
                .filter { it !is FrameNode }
                .filter { it !is LabelNode || isInUse(m, it) }
                .forEach {
                    patch.append("    ")
                        .append("    ")
                        .append('.')
                    if (!Instructions.print(patch, m, it)) {
                        println("Warning: unsupported instruction ${it.getOpcode()}")
                        patch.append("unsupported ")
                            .append(it.getOpcode())
                            .append(' ')
                            .append(it)
                    }
                    patch.append('\n')
                }

            patch.append("    ")
                .append("}\n\n")
        }

        patch.append("}\n")

        val full = StringBuilder()

        for (import in imports.entrySet().sortBy(Comparator {
            (a: Map.Entry<String, String>, b: Map.Entry<String, String>): Int ->
            a.value.compareTo(b.value)
        })) {
            full.append("import ")
                .append(import.value)
                .append(";\n")
        }
        full.append(patch)

        return full.toString()
    }

    private fun isInUse(m: MethodNode, label: LabelNode): Boolean {
        for (tryNode in m.tryCatchBlocks) {
            if (tryNode.start == label) return true
            if (tryNode.end == label) return true
            if (tryNode.handler == label) return true
        }
        for (insnNode in m.instructions.toArray()) {
            if (insnNode is JumpInsnNode) {
                if (insnNode.label == label) {
                    return true
                }
            } else if (insnNode is LookupSwitchInsnNode) {
                if (insnNode.dflt == label) {
                    return true
                }
                if (insnNode.labels.any { it.equals(label) }) {
                    return true
                }
            } else if (insnNode is TableSwitchInsnNode) {
                if (insnNode.dflt == label) {
                    return true
                }
                if (insnNode.labels.any { it.equals(label) }) {
                    return true
                }
            }
        }
        return false
    }

    private fun appendModifiers(buf: StringBuilder, access: Int) {
        for (e in modifierAccess) {
            if (access and e.value != 0) {
                buf.append(e.key)
                    .append(' ')
            }
        }
    }

    private fun tryImport(cls: String): String {
        val c = cls.replace('/', '.')
        val short = c.substring(c.lastIndexOf('.') + 1)
        if (short in imports) {
            if (imports[short] == c) {
                return short
            }
            return c
        } else {
            imports[short] = c
            return short
        }
    }

    fun printType(patch: StringBuilder, type: Type) {
        when (type.getSort()) {
            Type.VOID -> patch.append("void")
            Type.BYTE -> patch.append("byte")
            Type.CHAR -> patch.append("char")
            Type.DOUBLE -> patch.append("double")
            Type.FLOAT -> patch.append("float")
            Type.INT -> patch.append("int")
            Type.LONG -> patch.append("long")
            Type.SHORT -> patch.append("short")
            Type.BOOLEAN -> patch.append("boolean")
            Type.OBJECT -> {
                patch.append(tryImport(type.getInternalName()))
            }
            Type.ARRAY -> {
                patch.append(tryImport(type.getInternalName()))
                for (i in 1..type.getDimensions()) {
                    patch.append("[]")
                }
            }
            else -> throw IllegalArgumentException()
        }
        patch.append(' ')
    }
}
