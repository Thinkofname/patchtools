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

package uk.co.thinkofdeath.patchtools

import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper
import uk.co.thinkofdeath.patchtools.wrappers.FieldWrapper
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.LabelNode

class PatchScope(private val parent: PatchScope? = null) {

    private val classMappings = hashMapOf<String, ClassWrapper>()
    private val methodMappings = hashMapOf<MethodWrapper, String>()
    private val fieldMappings = hashMapOf<FieldWrapper, String>()
    private val methodInstructionMap = hashMapOf<MethodNode, MutableMap<PatchInstruction, Int>>()
    private val methodLabelMap = hashMapOf<MethodNode, MutableMap<String, LabelNode>>()

    fun duplicate(): PatchScope {
        val patchScope = PatchScope()
        patchScope.classMappings.putAll(classMappings)
        patchScope.methodMappings.putAll(methodMappings)
        patchScope.fieldMappings.putAll(fieldMappings)
        return patchScope
    }

    fun putClass(cw: ClassWrapper, name: String): Boolean {
        return classMappings.put(name, cw) != null
    }

    fun getClass(name: String): ClassWrapper? {
        val cls = classMappings[name]
        if (cls == null) {
            return parent?.getClass(name)
        }
        return cls
    }

    fun getClass(cls: ClassWrapper): String? {
        return classMappings
            .filterValues { it == cls }
            .map { it.getKey() }
            .first()
    }

    fun putMethod(mw: MethodWrapper, name: String, desc: String): Boolean {
        return methodMappings.put(mw, name + desc) != null
    }

    fun getMethod(mw: MethodWrapper): String? {
        return methodMappings[mw]
    }

    fun getMethod(owner: ClassWrapper, name: String, desc: String): MethodWrapper? {
        val joined = name + desc
        return methodMappings.keySet()
            .filter { it.has(owner) }
            .filter { getMethodDesc(it).equals(joined) }
            .first() ?: parent?.getMethod(owner, name, desc)
    }

    private fun getMethodDesc(mw: MethodWrapper): String? {
        val key = methodMappings[mw]
        if (key == null) {
            return parent?.getMethodDesc(mw)
        }
        return key
    }

    fun putField(mw: FieldWrapper, name: String, desc: String): Boolean {
        return fieldMappings.put(mw, name + "::" + desc) != null
    }

    fun getField(mw: FieldWrapper): String? {
        return fieldMappings[mw]
    }

    fun getField(owner: ClassWrapper, name: String, desc: String): FieldWrapper? {
        val joined = name + "::" + desc
        return fieldMappings.keySet()
            .filter { it.has(owner) }
            .filter { getFieldDesc(it).equals(joined) }
            .first() ?: parent?.getField(owner, name, desc)
    }

    private fun getFieldDesc(mw: FieldWrapper): String? {
        val key = fieldMappings[mw]
        if (key == null) {
            return parent?.getFieldDesc(mw)
        }
        return key
    }

    fun getInstructMap(node: MethodNode): MutableMap<PatchInstruction, Int>? {
        return methodInstructionMap[node]
    }

    fun putInstructMap(node: MethodNode, instMap: MutableMap<PatchInstruction, Int>) {
        methodInstructionMap[node] = instMap
    }

    fun getLabel(node: MethodNode, name: String): LabelNode? {
        return methodLabelMap[node]?.get(name)
    }

    fun putLabel(node: MethodNode, label: LabelNode, name: String) {
        if (node !in methodLabelMap) {
            methodLabelMap[node] = hashMapOf()
        }
        methodLabelMap[node][name] = label
    }

    fun merge(scope: PatchScope) {
        classMappings.putAll(scope.classMappings)
        methodMappings.putAll(scope.methodMappings)
        fieldMappings.putAll(scope.fieldMappings)
        methodInstructionMap.putAll(scope.methodInstructionMap)
        methodLabelMap.putAll(scope.methodLabelMap)
    }

    fun clearLabels(node: MethodNode) {
        methodLabelMap.remove(node)
    }

    fun clearInstructions(node: MethodNode) {
        methodInstructionMap.remove(node)
    }

    override fun toString(): String {
        return "PatchScope(cm=$classMappings, fm=$fieldMappings, mm=$methodMappings)"
    }
}

