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

import org.objectweb.asm.tree.ClassNode
import uk.co.thinkofdeath.patchtools.logging.StateLogger
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

import java.util.ArrayList
import java.util.HashSet

public class MatchClass(public val name: String) {
    public var superClass: MatchClass? = null
        set (matchClass) {
            if ($superClass != null) {
                throw IllegalArgumentException("Multiple super classes")
            }
            $superClass = matchClass
        }
    val interfaces = ArrayList<MatchClass>()
    val methods = ArrayList<MatchMethod>()
    val fields = ArrayList<MatchField>()

    val matches = ArrayList<ClassNode>()
    private val checkedClasses = HashSet<ClassNode>()

    public fun addInterface(matchClass: MatchClass) {
        interfaces.add(matchClass)
    }

    public fun addMethod(method: MatchMethod): MatchMethod {
        if (!methods.contains(method)) {
            methods.add(method)
            return method
        } else {
            return methods.filter { method == it }.first ?: method
        }
    }

    public fun addField(field: MatchField): MatchField {
        if (!fields.contains(field)) {
            fields.add(field)
            return field
        } else {
            return fields.filter { field == it }.first ?: field
        }
    }

    public fun addMatch(classNode: ClassNode) {
        if (!checkedClasses.contains(classNode) && !matches.contains(classNode)) {
            matches.add(classNode)
        }
    }

    public fun removeMatch(classNode: ClassNode) {
        matches.remove(classNode)
    }

    public fun addChecked(classNode: ClassNode) {
        checkedClasses.add(classNode)
    }

    public fun hasUnchecked(): Boolean {
        return matches.any { it !in checkedClasses }
    }

    public fun getUncheckedClasses(): Array<ClassNode> {
        return matches
            .filter { it !in checkedClasses }
            .copyToArray()
    }

    public fun hasChecked(length: Int): Boolean {
        return checkedClasses.size() == length
    }

    override fun equals(other: Any?): Boolean {
        if (this.identityEquals(other)) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as MatchClass
        return name == that.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    public fun check(logger: StateLogger, classSet: ClassSet, node: ClassNode) {
        addChecked(node)
        logger.println("- " + node.name)
        logger.indent()

        if (superClass != null) {
            val su = classSet.getClassWrapper(node.superName)
            if (su != null && !su.isHidden()) {
                logger.println(StateLogger.match(su.node, superClass!!))
                superClass!!.addMatch(su.node)
            }
        }

        for (inter in node.interfaces) {
            val su = classSet.getClassWrapper(inter)
            if (su != null && !su.isHidden()) {
                logger.println("Adding " + su.node.name + " as a possible match for " + interfaces.size() + " interfaces")
                interfaces.forEach { it.addMatch(su.node) }
            }
        }

        logger.println("Adding methods/fields to be tested")

        fields.forEach {
            val f = it
            node.fields.forEach {
                f.addMatch(node, it)
            }
        }
        methods.forEach {
            val m = it
            node.methods.forEach {
                m.addMatch(node, it)
            }
        }

        logger.unindent()
    }
}
