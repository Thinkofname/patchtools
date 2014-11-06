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
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import uk.co.thinkofdeath.patchtools.logging.StateLogger
import uk.co.thinkofdeath.patchtools.patch.Ident
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

import java.util.*

public class MatchField(public val owner: MatchClass, public val name: String, public val desc: String) {
    public var `type`: Type? = null

    private val matchedFields = ArrayList<FieldPair>()
    private val checkedFields = HashSet<FieldPair>()

    public fun addMatch(owner: ClassNode, fieldNode: FieldNode) {
        if (!checkedFields.contains(FieldPair(owner, fieldNode))) {
            matchedFields.add(FieldPair(owner, fieldNode))
        }
    }

    public fun removeMatch(owner: ClassNode, fieldNode: FieldNode) {
        matchedFields.remove(FieldPair(owner, fieldNode))
    }

    public fun removeMatch(clazz: ClassNode) {
        val it = matchedFields.listIterator()
        while (it.hasNext()) {
            val pair = it.next()
            if (pair.owner == clazz) {
                it.remove()
            }
        }
    }

    public fun addChecked(owner: ClassNode, fieldNode: FieldNode) {
        checkedFields.add(FieldPair(owner, fieldNode))
    }

    public fun hasUnchecked(): Boolean {
        return matchedFields.any { it !in checkedFields }
    }

    public fun getUncheckedMethods(): Array<FieldPair> {
        return matchedFields
            .filter { it !in checkedFields }
            .copyToArray()
    }

    public fun getMatches(): List<FieldNode> {
        return matchedFields
            .map { it.node }
    }

    public fun getMatches(owner: ClassNode): List<FieldNode> {
        return matchedFields
            .filter { it.owner == owner }
            .map { it.node }
    }

    public fun usesNode(clazz: ClassNode): Boolean {
        return matchedFields.any { it.owner == clazz }
    }

    public fun check(logger: StateLogger, classSet: ClassSet, group: MatchGroup, pair: FieldPair) {
        val node = pair.node
        addChecked(pair.owner, pair.node)

        logger.println("- " + pair.owner.name + "." + node.name)
        logger.indent()

        val `type` = Type.getType(node.desc)
        if (`type`.getSort() != type.getSort()) {
            logger.println(StateLogger.typeMismatch(type, `type`))
            removeMatch(pair.owner, node)
        } else if (`type`.getSort() == Type.OBJECT) {
            val retCls = group.getClass(MatchClass(Ident(type.getInternalName()).name))
            val wrapper = classSet.getClassWrapper(`type`.getInternalName())
            if (wrapper != null && !wrapper.isHidden()) {
                logger.println("Adding " + wrapper.node.name + " as a possible match for " + type.getInternalName())
                retCls.addMatch(wrapper.node)
            }
        }

        logger.unindent()
    }

    data class FieldPair(public val owner: ClassNode, public val node: FieldNode)

    override fun equals(other: Any?): Boolean {
        if (this.identityEquals(other)) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as MatchField

        return desc == that.desc && name == that.name && owner == that.owner

    }

    override fun hashCode(): Int {
        var result = owner.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + desc.hashCode()
        return result
    }
}
