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

package uk.co.thinkofdeath.patchtools.matching


import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import uk.co.thinkofdeath.patchtools.logging.StateLogger
import uk.co.thinkofdeath.patchtools.patch.*
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

import java.util.*

public class MatchMethod(public val owner: MatchClass, public val name: String, public val desc: String) {
    val arguments = ArrayList<Type>()
    public var returnType: Type? = null
        private set

    private val matchedMethods = ArrayList<MethodPair>()
    private val checkedMethods = HashSet<MethodPair>()

    public fun addArgument(type: Type) {
        arguments.add(type)
    }

    public fun setReturn(type: Type) {
        returnType = type
    }

    public fun addMatch(owner: ClassNode, methodNode: MethodNode) {
        if (!checkedMethods.contains(MethodPair(owner, methodNode))) {
            matchedMethods.add(MethodPair(owner, methodNode))
        }
    }

    public fun removeMatch(owner: ClassNode, methodNode: MethodNode) {
        matchedMethods.remove(MethodPair(owner, methodNode))
    }

    public fun removeMatch(clazz: ClassNode) {
        val it = matchedMethods.listIterator()
        while (it.hasNext()) {
            val pair = it.next()
            if (pair.owner == clazz) {
                it.remove()
            }
        }
    }

    public fun addChecked(owner: ClassNode, methodNode: MethodNode) {
        checkedMethods.add(MethodPair(owner, methodNode))
    }

    public fun hasUnchecked(): Boolean {
        return matchedMethods.any { it !in checkedMethods }
    }

    public fun getUncheckedMethods(): Array<MethodPair> {
        return matchedMethods
            .filter { it !in checkedMethods }
            .copyToArray()
    }

    public fun getMatches(): List<MethodNode> {
        return matchedMethods
            .map { it.node }
    }

    public fun getMatches(owner: ClassNode): List<MethodNode> {
        return matchedMethods
            .filter { it.owner == owner }
            .map { it.node }
    }

    public fun usesNode(clazz: ClassNode): Boolean {
        return matchedMethods.any { it.owner == clazz }
    }

    public fun check(logger: StateLogger, classSet: ClassSet, patchClasses: PatchClasses, group: MatchGroup, pair: MethodPair) {
        val node = pair.node
        addChecked(pair.owner, pair.node)

        logger.println("- " + pair.owner.name + "::" + node.name + node.desc)
        logger.indent()
        var inCode = false

        try {

            val matchPairs = ArrayList<MatchPair>()

            val type = Type.getMethodType(node.desc)

            if (type.getArgumentTypes().size != arguments.size()) {
                logger.println("Argument size mis-match " + arguments.size() + " != " + type.getArgumentTypes().size)
                removeMatch(pair.owner, node)
                return
            }

            val ret = type.getReturnType()
            if (ret.getSort() != returnType!!.getSort()) {
                removeMatch(pair.owner, node)
                logger.println(StateLogger.typeMismatch(returnType!!, ret))
                return
            } else if (ret.getSort() == Type.OBJECT) {
                val retCls = group.getClass(MatchClass(Ident(returnType!!.getInternalName()).name))
                val wrapper = classSet.getClassWrapper(ret.getInternalName())
                if (wrapper != null && !wrapper.isHidden()) {
                    logger.println(StateLogger.match(wrapper.node, retCls))
                    matchPairs.add(ClassMatch(retCls, wrapper.node))
                }
            }

            val argumentTypes = type.getArgumentTypes()
            for (i in argumentTypes.indices) {
                val arg = argumentTypes[i]
                if (arg.getSort() != arguments.get(i).getSort()) {
                    removeMatch(pair.owner, node)
                    logger.println(StateLogger.typeMismatch(arguments.get(i), arg))
                    return
                } else if (arg.getSort() == Type.OBJECT) {
                    val argCls = group.getClass(MatchClass(Ident(arguments.get(i).getInternalName()).name))
                    val wrapper = classSet.getClassWrapper(arg.getInternalName())
                    if (wrapper != null && !wrapper.isHidden()) {
                        logger.println(StateLogger.match(wrapper.node, argCls))
                        matchPairs.add(ClassMatch(argCls, wrapper.node))
                    }
                }
            }

            val pc = patchClasses.getClass(owner.name)
            if (pc != null) {
                val pm = pc.methods
                    .filter { it.ident.name == name }
                    .filter { it.descRaw == desc }
                    .first

                if (pm != null) {
                    logger.println("Entering method")
                    logger.indent()
                    inCode = true

                    if (!pm.check(logger, classSet, null, node)) {
                        removeMatch(pair.owner, node)
                        return
                    }

                    val it = node.instructions.iterator()
                    val referencedClasses = HashSet<ClassNode>()
                    val referencedMethods = HashSet<MatchMethod.MethodPair>()
                    val referencedFields = HashSet<MatchField.FieldPair>()
                    while (it.hasNext()) {
                        val insn = it.next()

                        if (insn is MethodInsnNode) {
                            val cls = classSet.getClassWrapper(insn.owner)
                            if (cls == null || cls.isHidden()) continue

                            referencedClasses.add(cls.node)

                            val wrap = cls.getMethod(insn.name, insn.desc)
                            if (wrap != null) {
                                referencedMethods.add(MatchMethod.MethodPair(cls.node, cls.getMethodNode(wrap)!!))
                            }
                        } else if (insn is FieldInsnNode) {
                            val cls = classSet.getClassWrapper(insn.owner)
                            if (cls == null || cls.isHidden()) continue

                            referencedClasses.add(cls.node)

                            val wrap = cls.getField(insn.name, insn.desc)
                            if (wrap != null) {
                                referencedFields.add(MatchField.FieldPair(cls.node, cls.getFieldNode(wrap)!!))
                            }
                        } else if (insn is LdcInsnNode) {
                            if (insn.cst is Type) {
                                val cls = classSet.getClassWrapper((insn.cst as Type).getInternalName())
                                if (cls == null || cls.isHidden()) continue

                                referencedClasses.add(cls.node)
                            }
                        } else if (insn is TypeInsnNode) {
                            val desc = insn.desc
                            val cls = classSet.getClassWrapper(MatchGenerator.getRootType(Type.getObjectType(desc)).getInternalName())
                            if (cls == null || cls.isHidden()) continue

                            referencedClasses.add(cls.node)
                        } else if (insn is MultiANewArrayInsnNode) {
                            val desc = insn.desc
                            val cls = classSet.getClassWrapper(MatchGenerator.getRootType(Type.getObjectType(desc)).getInternalName())
                            if (cls == null || cls.isHidden()) continue

                            referencedClasses.add(cls.node)
                        }
                    }

                    for (instruction in pm.instructions) {
                        val insn = instruction.instruction
                        if (insn.handler == null || instruction.mode == Mode.ADD) continue
                        insn.handler!!.getReferencedClasses(instruction).forEach {
                            val c = it
                            referencedClasses.forEach {
                                val wrapper = classSet.getClassWrapper(it.name)!!
                                if (!wrapper.isHidden()) {
                                    matchPairs.add(ClassMatch(group.getClass(c), it))
                                }
                            }

                        }
                        insn.handler!!.getReferencedMethods(instruction).forEach {
                            val me = it
                            val matchClass = group.getClass(me.owner)
                            val fme = matchClass.addMethod(me)
                            referencedMethods.forEach {
                                matchPairs.add(MethodMatch(fme, it.owner, it.node))
                            }
                        }
                        insn.handler!!.getReferencedFields(instruction).forEach {
                            val fe = it
                            val matchClass = group.getClass(fe.owner)
                            val fme = matchClass.addField(fe)
                            referencedFields.forEach {
                                matchPairs.add(FieldMatch(fme, it.owner, it.node))
                            }
                        }
                    }
                    inCode = false
                    logger.unindent()
                }
            }
            logger.println("Adding " + matchPairs.size() + " new matches")

            matchPairs.forEach {
                it.apply()
            }
        } finally {
            logger.unindent()
            if (inCode) logger.unindent()
        }
    }

    data class MethodPair(public val owner: ClassNode, public val node: MethodNode)

    override fun equals(other: Any?): Boolean {
        if (this.identityEquals(other)) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as MatchMethod

        return desc == that.desc && name == that.name && owner == that.owner

    }

    override fun hashCode(): Int {
        var result = owner.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + desc.hashCode()
        return result
    }
}
