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

import java.io.InputStream
import org.objectweb.asm.tree.ClassNode
import uk.co.thinkofdeath.patchtools.PatchScope
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.Remapper
import java.util.HashMap
import java.util.ArrayList
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.RemappingClassAdapter
import java.util.HashSet

public class ClassSet(private val classPath: ClassPathWrapper) : Iterable<String> {

    private val classes = HashMap<String, ClassWrapper>()

    private var simplified: Boolean = false

    public fun simplify() {
        if (simplified) return
        simplified = true
        // Safety copy
        ArrayList(classes.values())
            .filter { !it.isHidden() }
            .forEach {
                val v = it
                it.methods
                    .filter { !it.isHidden() }
                    .forEach {
                        val node = v.getMethodNode(it)!!
                        if (((node.access and Opcodes.ACC_PUBLIC) != 0
                            || (node.access and Opcodes.ACC_PROTECTED) != 0)
                            && (node.access and Opcodes.ACC_STATIC) == 0) {
                            for (inter in v.node.interfaces) {
                                replaceMethod(it, inter)
                            }
                            replaceMethod(it, v.node.superName)
                        }
                    }
            }

        // Second pass to add everything to each other
        ArrayList(classes.values())
            .filter { !it.isHidden() }
            .forEach { grab(it, it) }
    }

    private fun grab(root: ClassWrapper, current: ClassWrapper?) {
        if (current == null) return
        current.fields
            .filter { !it.isHidden() }
            .filter {
                val node = current.getFieldNode(it)!!
                ((node.access and Opcodes.ACC_PUBLIC) != 0
                    || (node.access and Opcodes.ACC_PROTECTED) != 0)
                    && (node.access and Opcodes.ACC_STATIC) == 0
            }
            .forEach { it.add(root) }
        current.methods
            .filter { !it.isHidden() }
            .filter {
                val node = current.getMethodNode(it)!!
                ((node.access and Opcodes.ACC_PUBLIC) != 0
                    || (node.access and Opcodes.ACC_PROTECTED) != 0)
                    && (node.access and Opcodes.ACC_STATIC) == 0
            }
            .forEach { it.add(root) }

        for (inter in current.node.interfaces) {
            grab(root, getClassWrapper(inter))
        }
        grab(root, getClassWrapper(current.node.superName))
    }

    private fun replaceMethod(methodWrapper: MethodWrapper, clazz: String?) {
        if (clazz == null) return
        val cl = getClassWrapper(clazz)
        if (cl == null) {
            return
        }
        val target = cl.methods
            .filter { it.name == methodWrapper.name }
            .filter { it.desc == methodWrapper.desc }
            .filter {
                val node = cl.getMethodNode(it)!!
                (((node.access and Opcodes.ACC_PUBLIC) != 0
                    || (node.access and Opcodes.ACC_PROTECTED) != 0))
                    && (node.access and Opcodes.ACC_STATIC) == 0
            }
            .first
        if (target != null) {
            if (target.isHidden()) {
                methodWrapper.hidden = true
            }
            cl.methods.remove(target)
            cl.methods.add(methodWrapper)
            methodWrapper.add(target)
        }
        for (inter in cl.node.interfaces) {
            replaceMethod(methodWrapper, inter)
        }
        replaceMethod(methodWrapper, cl.node.superName)

    }

    public fun add(clazz: InputStream) {
        add(clazz, true)
    }

    public fun add(clazz: InputStream, close: Boolean) {
        try {
            add(clazz.readBytes())
        } finally {
            if (close) {
                clazz.close()
            }
        }
    }

    public fun add(clazz: ByteArray) {
        val classReader = ClassReader(clazz)
        val node = ClassNode(Opcodes.ASM5)
        classReader.accept(node, 0)
        add(node)
    }

    public fun add(node: ClassNode) {
        classes.put(node.name, ClassWrapper(this, node))
    }

    public fun remove(name: String) {
        classes.remove(name)
    }

    public fun getClass(name: String): ByteArray? {
        val classWriter = ClassSetWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
        val wrapper = classes.get(name)
        if (wrapper == null || wrapper.isHidden()) {
            return null
        }
        wrapper.node.version = Opcodes.V1_8
        wrapper.node.accept(classWriter)
        return classWriter.toByteArray()
    }

    public fun getClass(name: String, scope: PatchScope): ByteArray? {
        val classReader = ClassReader(getClass(name))
        val classWriter = ClassSetWriter(0)
        val wrapper = classes.get(name)
        if (wrapper == null || wrapper.isHidden()) {
            return null
        }
        classReader.accept(RemappingClassAdapter(classWriter, ClassRemapper(scope)), ClassReader.EXPAND_FRAMES)
        return classWriter.toByteArray()
    }

    public fun getClassWrapper(name: String?): ClassWrapper? {
        if (name == null) return null
        var cl: ClassWrapper? = classes.get(name)
        if (cl == null) {
            cl = classPath.find(this, name)
            if (cl == null) return null
            classes.put(cl!!.node.name, cl)
        }
        return cl
    }

    public fun classes(): Array<String> {
        return classes.keySet().copyToArray()
    }

    private var hiddenStrippedCache: Array<String>? = null

    public fun classes(stripHidden: Boolean): Array<String> {
        if (!stripHidden) {
            return classes()
        }
        if (hiddenStrippedCache == null) {
            hiddenStrippedCache = classes
                .filterValues { !it.isHidden() }
                .map { it.getKey() }
                .copyToArray()
        }
        return hiddenStrippedCache!!
    }

    override fun iterator(): Iterator<String> {
        return classes.keySet().iterator()
    }

    private inner class ClassSetWriter(flags: Int) : ClassWriter(flags) {

        override fun getCommonSuperClass(type1: String, type2: String): String {
            if (type1 == type2) {
                return type1
            }

            val supers = HashSet<String>()
            supers.add(type1)
            supers.add(type2)

            var run1 = true
            var run2 = true
            var t1 = getClassWrapper(type1)
            var t2 = getClassWrapper(type2)

            if (t1 == null || t2 == null) {
                return "java/lang/Object"
            }

            if ((t1!!.node.access and Opcodes.ACC_INTERFACE) != 0
                || (t2!!.node.access and Opcodes.ACC_INTERFACE) != 0) {
                return "java/lang/Object"
            }

            while (run1 || run2) {
                if (run1) {
                    if (t1 == null || t1!!.node.superName == null) {
                        run1 = false
                    } else {
                        t1 = getClassWrapper(t1!!.node.superName)
                        if (t1 != null && !supers.add(t1!!.node.name)) {
                            return t1!!.node.name
                        }
                    }
                }
                if (run2) {
                    if (t2 == null || t2!!.node.superName == null) {
                        run2 = false
                    } else {
                        t2 = getClassWrapper(t2!!.node.superName)
                        if (t2 != null && !supers.add(t2!!.node.name)) {
                            return t2!!.node.name
                        }
                    }
                }
            }
            return "java/lang/Object"
        }
    }

    private inner class ClassRemapper(private val scope: PatchScope) : Remapper() {

        override fun map(typeName: String): String {
            val cls = getClassWrapper(typeName)
            if (cls != null) {
                return scope.getClass(cls) ?: typeName
            }
            return typeName
        }

        override fun mapMethodName(owner: String, name: String, desc: String): String {
            val cls = getClassWrapper(owner)
            if (cls != null) {
                val methodWrapper = cls.getMethod(name, desc)
                if (methodWrapper != null) {
                    val nName = scope.getMethod(methodWrapper)
                    return if (nName == null) name else nName.split("\\(")[0]
                }
            }
            return name
        }

        override fun mapFieldName(owner: String, name: String, desc: String): String {
            val cls = getClassWrapper(owner)
            if (cls != null) {
                val fieldWrapper = cls.getField(name, desc)
                if (fieldWrapper != null) {
                    val nName = scope.getField(fieldWrapper)
                    return if (nName == null) name else nName.split("::")[0]
                }
            }
            return name
        }
    }
}