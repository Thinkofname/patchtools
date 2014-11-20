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

import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.instruction.Instruction
import uk.co.thinkofdeath.patchtools.instruction.instructions.TryCatchInstruction
import uk.co.thinkofdeath.patchtools.logging.StateLogger
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

import java.util.*
import uk.co.thinkofdeath.patchtools.lexer.Token
import uk.co.thinkofdeath.patchtools.lexer.TokenType

public class PatchMethod(public val owner: PatchClass,
                         it: Iterator<Token>,
                         type: Ident,
                         retDimCount: Int,
                         public val ident: Ident,
                         modifiers: Set<String>,
                         public val patchAnnotations: List<String>
) {
    public val descRaw: String
    public val desc: Type
        get() = Type.getMethodType(descRaw)
    public val mode: Mode
    val access: Int

    public val instructions: MutableList<PatchInstruction> = ArrayList()

    // Used by countArrayTypes
    var dimCount = 0

    {
        var access = 0
        for (modifier in modifiers) {
            if (modifier in modifierAccess) {
                access = access or modifierAccess[modifier]!!
            }
        }
        this.access = access and methodModifiers

        val descBuilder = StringBuilder("(")
        var token = it.next()
        var dims = 0
        while (token.type != TokenType.ARGUMENT_LIST_END) {
            val type = token.expect(TokenType.IDENT).value
            token = countArrayTypes(it)
            dims += dimCount
            token.expect(TokenType.IDENT)

            token = countArrayTypes(it)
            dims += dimCount

            for (i in 1..dims) {
                descBuilder.append('[')
            }
            PatchClass.appendType(descBuilder, owner.classes.scanImports(type).toString())

            dims = 0
            if (token.type == TokenType.ARGUMENT_LIST_NEXT) {
                token = it.next()
            }
        }
        descBuilder.append(")")
        for (i in 1..retDimCount) {
            descBuilder.append('[')
        }
        PatchClass.appendType(descBuilder, type.toString())
        descRaw = descBuilder.toString()

        it.next().expect(TokenType.ENTER_BLOCK)

        mode = if ("add" in modifiers) Mode.ADD
        else if ("remove" in modifiers) Mode.REMOVE
        else Mode.MATCH

        token = it.next()

        var patchAnnotations: MutableList<String>? = null
        while (token.type != TokenType.EXIT_BLOCK) {
            var mode: Mode
            when (token.type) {
                TokenType.COMMENT -> {
                    token = it.next()
                    continue
                }
                TokenType.PATCH_ANNOTATION -> {
                    if (patchAnnotations == null) {
                        throw ValidateException("Unexpected patch annotation")
                            .setLineNumber(token.lineNumber)
                            .setLineOffset(token.lineOffset)
                    }
                    patchAnnotations?.add(token.value.trim())
                    token = it.next()
                    continue
                }
                TokenType.REMOVE_INSTRUCTION -> mode = Mode.REMOVE
                TokenType.ADD_INSTRUCTION -> mode = Mode.ADD
                TokenType.MATCH_INSTRUCTION -> mode = Mode.MATCH
                else -> throw ValidateException("Unexpected ${token.type}")
                    .setLineNumber(token.lineNumber)
                    .setLineOffset(token.lineOffset)
            }
            val insn = PatchInstruction(mode, it)
            if (insn.instruction.handler != null) {
                insn.instruction.handler!!.validate(insn)
            }
            patchAnnotations = insn.meta
            instructions.add(insn)
            token = it.next()
        }
    }

    fun countArrayTypes(it: Iterator<Token>): Token {
        dimCount = 0
        while (true) {
            val token = it.next()
            if (token.type != TokenType.ARRAY_TYPE) return token
            dimCount++
        }
    }

    public fun apply(classSet: ClassSet, scope: PatchScope, methodNode: MethodNode) {
        methodNode.access = access
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

            if (methodNode.access and methodModifiers != access) {
                logger.println("Incorrect access modifiers " +
                    "${Integer.toBinaryString(methodNode.access and methodModifiers)} != " +
                    "${Integer.toBinaryString(access)}")
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
                                logger.println { i.toString() + ": " + patchInstruction + " succeeded on " + insn }
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
                                logger.println { i.toString() + ": " + patchInstruction + " failed on " + insn }
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
