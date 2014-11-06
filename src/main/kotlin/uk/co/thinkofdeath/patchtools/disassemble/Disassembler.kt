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

public class Disassembler(private val classSet: ClassSet) {

    public fun disassemble(cls: String): String {
        val classWrapper = classSet.getClassWrapper(cls)!!

        val patch = StringBuilder("\n")

        val node = classWrapper.node

        patch.append(".class ")
            .append(node.name)
            .append('\n')

        if (node.superName != null) {
            patch.append("    .super ")
                .append(node.superName)
                .append('\n')
        }

        for (inter in node.interfaces) {
            patch
                .append("    .interface ")
                .append(inter)
                .append('\n')

        }

        node.fields.forEach {
            patch
                .append("    ")
                .append(".field ")
                .append(it.name)
                .append(' ')
                .append(it.desc)
            if ((it.access and Opcodes.ACC_STATIC) != 0) {
                patch.append(" static")
            }
            if ((it.access and Opcodes.ACC_PRIVATE) != 0) {
                patch.append(" private")
            }
            if (it.value != null) {
                patch.append(" ")
                Utils.printConstant(patch, it.value)
            }
            patch.append('\n')
        }

        patch.append('\n')

        node.methods.forEach {
            patch.append("    ")
                .append(".method ")
                .append(it.name)
                .append(' ')
                .append(it.desc)
            if ((it.access and Opcodes.ACC_STATIC) != 0) {
                patch.append(" static")
            }
            if ((it.access and Opcodes.ACC_PRIVATE) != 0) {
                patch.append(" private")
            }
            if ((it.access and Opcodes.ACC_PROTECTED) != 0) {
                patch.append(" protected")
            }
            patch.append('\n')

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
                    if (!Instruction.print(patch, m, it)) {
                        // TODO: throw new UnsupportedOperationException(i.toString());
                        patch.append("unsupported ")
                            .append(it.getOpcode())
                            .append(' ')
                            .append(it)
                    }
                    patch.append('\n')
                }

            patch.append("    ")
                .append(".end-method\n")
                .append('\n')
        }

        patch.append(".end-class\n")

        return patch.toString()
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
}
