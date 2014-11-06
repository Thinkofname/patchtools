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

package uk.co.thinkofdeath.patchtools.wrappers

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.FieldNode

public class ClassWrapper(public val classSet: ClassSet, public val node: ClassNode, private val hidden: Boolean = false) {
    public val methods: MutableList<MethodWrapper> = arrayListOf()
    public val fields: MutableList<FieldWrapper> = arrayListOf();

    {
        node.methods.forEach {
            methods.add(MethodWrapper(this, it))
        }
        node.fields.forEach {
            fields.add(FieldWrapper(this, it))
        }
    }

    public fun isHidden(): Boolean {
        return hidden
    }

    // Shouldn't ever been updated so we cache
    private var methodCache: Array<MethodWrapper>? = null

    public fun getMethods(stripHidden: Boolean): Array<MethodWrapper> {
        if (stripHidden) {
            if (methodCache == null) {
                methodCache = methods
                    .filter { !it.isHidden() }
                    .copyToArray()
            }
            return methodCache!!
        }
        return methods.copyToArray()
    }

    public fun getMethodNode(wrapper: MethodWrapper): MethodNode? {
        var mn: MethodNode? = node.methods
            .filter { it.name == wrapper.name }
            .filter { it.desc == wrapper.desc }
            .first
        if (mn == null && node.superName != null) {
            val owner = classSet.getClassWrapper(node.superName)
            if (owner != null) {
                mn = owner.getMethodNode(wrapper)
            }
        }
        return mn
    }

    public fun getMethod(name: String, desc: String): MethodWrapper? {
        var wrap: MethodWrapper? = methods
            .filter { it.name == name }
            .filter { it.desc == desc }
            .first
        if (wrap == null && node.superName != null) {
            val owner = classSet.getClassWrapper(node.superName)
            if (owner != null) {
                wrap = owner.getMethod(name, desc)
            }
        }
        return wrap
    }

    // Shouldn't ever been updated so we cache
    private var fieldCache: Array<FieldWrapper>? = null

    public fun getFields(stripHidden: Boolean): Array<FieldWrapper> {
        if (stripHidden) {
            if (fieldCache == null) {
                fieldCache = fields
                    .filter { !it.hidden }
                    .copyToArray()
            }
            return fieldCache!!
        }
        return fields.copyToArray()
    }

    public fun getField(name: String, desc: String): FieldWrapper? {
        var wrap: FieldWrapper? = fields
            .filter { it.name == name }
            .filter { it.desc == desc }
            .first
        if (wrap == null && node.superName != null) {
            val owner = classSet.getClassWrapper(node.superName)
            if (owner != null) {
                wrap = owner.getField(name, desc)
            }
        }
        return wrap
    }

    public fun getFieldNode(fieldWrapper: FieldWrapper): FieldNode? {
        var fn: FieldNode? = node.fields
            .filter { it.name == fieldWrapper.name }
            .filter { it.desc == fieldWrapper.desc }
            .first
        if (fn == null && node.superName != null) {
            val owner = classSet.getClassWrapper(node.superName)
            if (owner != null) {
                fn = owner.getFieldNode(fieldWrapper)
            }
        }
        return fn
    }

    override fun toString(): String {
        return "ClassWrapper{" + node.name + "}"
    }
}