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

package uk.co.thinkofdeath.patchtools.patch

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.instructions.TryCatchInstruction
import uk.co.thinkofdeath.patchtools.instruction.instructions.Utils
import uk.co.thinkofdeath.patchtools.logging.StateLogger
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

import java.util.*

public class PatchMethod(public val owner: PatchClass, mCommand: Command, reader: LineReader) {
    public val ident: Ident
    public val descRaw: String
    public val desc: Type
        get() = Type.getMethodType(descRaw)
    public val mode: Mode
    public val isStatic: Boolean
    public val isPrivate: Boolean
    public val isProtected: Boolean

    public val instructions: MutableList<PatchInstruction> = ArrayList()

        ;{
        if (mCommand.args.size < 2) {
            throw ValidateException("Incorrect number of arguments for method").setLineNumber(reader.lineNumber)
        }
        ident = Ident(mCommand.args[0])
        mode = mCommand.mode
        try {
            $descRaw = mCommand.args[1]
            Utils.validateMethodType(descRaw)
        } catch (e: ValidateException) {
            throw e.setLineNumber(reader.lineNumber)
        }

        var isS = false
        var isP = false
        var isPo = false
        if (mCommand.args.size >= 3) {
            for (i in 2..mCommand.args.size - 1) {
                if (mCommand.args[i].equalsIgnoreCase("static")) {
                    isS = true
                } else if (mCommand.args[i].equalsIgnoreCase("private")) {
                    isP = true
                } else if (mCommand.args[i].equalsIgnoreCase("protected")) {
                    isPo = true
                } else {
                    throw ValidateException("Unexpected " + mCommand.args[i]).setLineNumber(reader.lineNumber)
                }
            }
        }
        isStatic = isS
        isPrivate = isP
        isProtected = isPo

        reader.whileHasLine {
            (it: String): Boolean ->
            val l = it.trim()
            if (l.startsWith("//") || l.length() == 0) return@whileHasLine false

            val command = Command.from(l)
            if (mode == Mode.ADD && command.mode != Mode.ADD) {
                throw ValidateException("In added methods everything must be +").setLineNumber(reader.lineNumber)
            } else if (mode == Mode.REMOVE && command.mode != Mode.REMOVE) {
                throw ValidateException("In removed methods everything must be -").setLineNumber(reader.lineNumber)
            }
            if (command.name.equalsIgnoreCase("end-method")) {
                return@whileHasLine true
            }
            val lineNo = reader.lineNumber
            try {
                val insn = PatchInstruction(command, reader)
                if (insn.instruction.handler != null) {
                    insn.instruction.handler!!.validate(insn)
                }
                instructions.add(insn)
            } catch (e: ValidateException) {
                throw e.setLineNumber(lineNo)
            }

            return@whileHasLine false
        }
    }

    public fun apply(classSet: ClassSet, scope: PatchScope, methodNode: MethodNode) {
        methodNode.access = methodNode.access and (Opcodes.ACC_STATIC or Opcodes.ACC_PRIVATE or Opcodes.ACC_PUBLIC).inv()
        if (isStatic) {
            methodNode.access = methodNode.access or Opcodes.ACC_STATIC
        }
        if (isPrivate) {
            methodNode.access = methodNode.access or Opcodes.ACC_PRIVATE
        } else if (isProtected) {
            methodNode.access = methodNode.access or Opcodes.ACC_PROTECTED
        } else {
            methodNode.access = methodNode.access or Opcodes.ACC_PUBLIC
        }
        val outInstructions = InsnList()
        val cloneMap = LabelCloneMap()
        for (insnNode in methodNode.instructions.toArray()) {
            outInstructions.add(insnNode.clone(cloneMap))
        }

        val tries = ArrayList<TryCatchBlockNode>()
        for (tryCatchBlockNode in methodNode.tryCatchBlocks) {
            val newTry = TryCatchBlockNode(cloneMap.get(tryCatchBlockNode.start), cloneMap.get(tryCatchBlockNode.end), cloneMap.get(tryCatchBlockNode.handler), tryCatchBlockNode.`type`)
            tries.add(newTry)
        }
        methodNode.tryCatchBlocks = tries

        val insnMap = scope.getInstructMap(methodNode)
        var position = 0
        var offset = 0

        for (patchInstruction in instructions) {
            if (patchInstruction.mode == Mode.ADD) {
                if (patchInstruction.instruction == Instruction.TRY_CATCH) {
                    TryCatchInstruction.create(classSet, scope, patchInstruction, methodNode, cloneMap)
                    continue
                }
                val newIn = patchInstruction.instruction.handler!!.create(classSet, scope, patchInstruction, methodNode)
                if (position - 1 >= 0) {
                    outInstructions.insert(outInstructions.get(position - 1), newIn.clone(cloneMap))
                } else {
                    outInstructions.insert(newIn.clone(cloneMap))
                }
                position++
                offset++
                continue
            }

            if (patchInstruction.instruction == Instruction.TRY_CATCH) {
                if (patchInstruction.mode == Mode.REMOVE) {
                    val match = TryCatchInstruction.match(classSet, scope, patchInstruction, methodNode)
                    methodNode.tryCatchBlocks.remove(match)
                }
                continue
            }

            if (patchInstruction.instruction == Instruction.ANY) {
                continue
            }

            val pos = insnMap?.get(patchInstruction)!!
            if (patchInstruction.mode == Mode.REMOVE) {
                outInstructions.remove(outInstructions.get(pos))
                offset--
            }
            position = pos + offset + 1
        }

        methodNode.instructions = outInstructions
    }

    public fun check(logger: StateLogger, classSet: ClassSet, scope: PatchScope?, methodNode: MethodNode): Boolean {
        var ok = false
        var inInstructions = false
        try {
            if (!ident.isWeak() && methodNode.name != ident.name) {
                logger.println("Name mis-match " + ident + " != " + methodNode.name)
                return false
            }

            val patchDesc = desc
            val desc = Type.getMethodType(methodNode.desc)

            if (patchDesc.getArgumentTypes().size != desc.getArgumentTypes().size) {
                logger.println("Argument size mis-match " + patchDesc.getArgumentTypes().size + " != " + desc.getArgumentTypes().size)
                return false
            }

            for (i in 0..patchDesc.getArgumentTypes().size - 1) {
                val pt = patchDesc.getArgumentTypes()[i]
                val t = desc.getArgumentTypes()[i]

                if (!PatchClass.checkTypes(classSet, scope, pt, t)) {
                    logger.println(StateLogger.typeMismatch(pt, t))
                    return false
                }
            }

            if (!PatchClass.checkTypes(classSet, scope, patchDesc.getReturnType(), desc.getReturnType())) {
                logger.println(StateLogger.typeMismatch(patchDesc.getReturnType(), desc.getReturnType()))
                return false
            }

            var position = 0
            val insns = methodNode.instructions

            if (((methodNode.access and Opcodes.ACC_STATIC) == 0) == isStatic) {
                logger.println(if (isStatic) "Required static" else "Required non-static")
                return false
            }
            if (((methodNode.access and Opcodes.ACC_PRIVATE) == 0) == isPrivate) {
                logger.println(if (isPrivate) "Required private" else "Required non-private")
                return false
            }
            if (((methodNode.access and Opcodes.ACC_PROTECTED) == 0) == isProtected) {
                logger.println(if (isProtected) "Required protected" else "Required non-protected")
                return false
            }

            var wildcard = false
            var wildcardPosition = -1
            var wildcardPatchPosition = -1

            val insnMap = hashMapOf<PatchInstruction, Int>()

            inInstructions = true
            logger.indent()

            var i = 0

            @checkLoop
            while (i < instructions.size()) {
                try {
                    val patchInstruction = instructions.get(i)
                    if (patchInstruction.mode == Mode.ADD) continue

                    if (patchInstruction.instruction == Instruction.ANY) {
                        logger.println("$i: Wild-card")
                        wildcard = true
                        wildcardPosition = -1
                        wildcardPatchPosition = -1
                        if (i == instructions.size() - 1) {
                            position = insns.size()
                        }
                        continue
                    }
                    while (true) {

                        if (position >= insns.size()) {
                            if (!wildcard) {
                                logger.println("Not enough instructions")
                                return false
                            }
                            break
                        }
                        val insn = insns.get(position)

                        val allowLabel = insn is LabelNode
                            && (patchInstruction.instruction == Instruction.LABEL
                            || patchInstruction.instruction == Instruction.TRY_CATCH)

                        if (insn !is LineNumberNode
                            && insn !is FrameNode
                            && (insn !is LabelNode || allowLabel)) {
                            if (patchInstruction.instruction.handler!!.check(classSet, scope, patchInstruction, methodNode, insn)) {
                                logger.println(i.toString() + ": " + patchInstruction + " succeeded on " + insn)
                                if (patchInstruction.instruction == Instruction.TRY_CATCH) continue@checkLoop
                                if (wildcard) {
                                    wildcardPosition = position
                                    wildcardPatchPosition = i
                                    logger.println("(Saving wildcard state)")
                                }
                                insnMap.put(patchInstruction, position)
                                wildcard = false
                                position++
                                continue@checkLoop
                            } else {
                                logger.println(i.toString() + ": " + patchInstruction + " failed on " + insn)
                                if (!wildcard) {
                                    if (wildcardPosition != -1) {
                                        logger.println("Failed")
                                        wildcard = true
                                        position = ++wildcardPosition
                                        i = --wildcardPatchPosition
                                        logger.println("Rolling back to the last wildcard")
                                        continue@checkLoop
                                    } else {
                                        logger.println("Failed")
                                        return false
                                    }
                                }
                                logger.println("Continuing because of wild-card")
                            }
                        }
                        position++
                    }
                    return false
                } finally {
                    i++
                }
            }

            for (pos in position..insns.size() - 1) {
                val insn = insns.get(pos)
                if (insn is LineNumberNode || insn is LabelNode) {
                    continue
                }
                logger.println("Too many instructions")
                return false
            }
            inInstructions = false
            logger.unindent()

            scope?.putInstructMap(methodNode, insnMap)
            ok = true
            logger.println("ok")
            return true
        } finally {
            if (!ok) {
                if (scope != null) {
                    scope.clearLabels(methodNode)
                    scope.clearInstructions(methodNode)
                }
            }
            if (inInstructions) {
                logger.unindent()
            }
        }
    }

    private inner class LabelCloneMap : MutableMap<LabelNode, LabelNode> {

        private val internal = HashMap<LabelNode, LabelNode>()

        override fun size(): Int {
            return internal.size()
        }

        override fun isEmpty(): Boolean {
            return internal.isEmpty()
        }

        override fun containsKey(key: Any?): Boolean {
            return internal.containsKey(key)
        }

        override fun containsValue(value: Any?): Boolean {
            return internal.containsValue(value)
        }

        override fun get(key: Any?): LabelNode {
            val k = key as LabelNode
            if (!internal.containsKey(key)) {
                internal.put(k, LabelNode())
            }
            return internal.get(key)
        }

        override fun put(key: LabelNode, value: LabelNode): LabelNode? {
            return internal.put(key, value)
        }

        override fun remove(key: Any?): LabelNode {
            return internal.remove(key)
        }

        override fun putAll(m: Map<out LabelNode, LabelNode>) {
            internal.putAll(m)
        }

        override fun clear() {
            internal.clear()
        }

        override fun keySet(): MutableSet<LabelNode> {
            return internal.keySet()
        }

        override fun values(): MutableCollection<LabelNode> {
            return internal.values()
        }

        override fun entrySet(): MutableSet<MutableMap.MutableEntry<LabelNode, LabelNode>> {
            return internal.entrySet()
        }
    }
}
