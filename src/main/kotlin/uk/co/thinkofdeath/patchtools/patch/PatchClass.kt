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

import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.Opcodes
import uk.co.thinkofdeath.patchtools.wrappers.FieldWrapper
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.Type
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper
import org.objectweb.asm.tree.MethodNode
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper
import uk.co.thinkofdeath.patchtools.logging.StateLogger

public class PatchClass(clCommand: Command, reader: LineReader) {

    val type: ClassType
    val ident: Ident
    val mode: Mode
    val superModifiers = arrayListOf<ModifierClass>()
    val interfaceModifiers = arrayListOf<ModifierClass>()

    val methods = arrayListOf<PatchMethod>()
    val fields = arrayListOf<PatchField>()

        ;{
        if (clCommand.args.size != 1) {
            throw ValidateException("Incorrect number of arguments for class").setLineNumber(reader.lineNumber)
        }
        type = ClassType.valueOf(clCommand.name.toUpperCase())
        ident = Ident(clCommand.args[0])
        mode = clCommand.mode
        var line: String? = null
        while ({ line = reader.readLine(); line != null }()) {
            val line = line!!.trim()
            if (line.startsWith("//") || line.length() == 0) continue

            val command = Command.from(line)
            if (mode == Mode.ADD && command.mode != Mode.ADD) {
                throw ValidateException("In added classes everything must be +").setLineNumber(reader.lineNumber)
            } else if (mode == Mode.REMOVE && command.mode != Mode.REMOVE) {
                throw ValidateException("In removed classes everything must be -").setLineNumber(reader.lineNumber)
            }
            when (command.name) {
                "super" -> {
                    if (command.args.size != 1) throw IllegalArgumentException("super requires 1 parameter")
                    superModifiers.add(ModifierClass(Ident(command.args[0]), command.mode))
                }
                "interface" -> {
                    if (command.args.size != 1)
                        throw IllegalArgumentException("interface requires 1 parameter")
                    interfaceModifiers.add(ModifierClass(Ident(command.args[0]), command.mode))
                }
                "method" -> methods.add(PatchMethod(this, command, reader))
                "field" -> try {
                    fields.add(PatchField(this, command))
                } catch (e: ValidateException) {
                    throw e.setLineNumber(reader.lineNumber)
                }

                else -> {
                    if (!command.name.endsWith("end-" + type.name().toLowerCase())) {
                        throw ValidateException(command.toString()).setLineNumber(reader.lineNumber)
                    }
                    break
                }
            }
        }
    }

    public fun apply(scope: PatchScope, classSet: ClassSet) {
        if (mode == Mode.REMOVE) {
            classSet.remove(ident.name)
            return
        } else if (mode == Mode.ADD) {
            val classNode = ClassNode(Opcodes.ASM5)
            classNode.version = Opcodes.V1_7
            classNode.access = Opcodes.ACC_PUBLIC
            classNode.name = ident.name
            classNode.superName = "java/lang/Object"
            when (type) {
                ClassType.ENUM -> classNode.access = classNode.access or Opcodes.ACC_ENUM
                ClassType.INTERFACE -> classNode.access = classNode.access or Opcodes.ACC_ENUM
            }
            classSet.add(classNode)
            scope.putClass(classSet.getClassWrapper(classNode.name)!!, classNode.name)
        }

        val classWrapper = scope.getClass(ident.name)!!

        for (superModifier in superModifiers) {
            if (superModifier.mode == Mode.MATCH) continue

            val name = superModifier.ident
            var clName = name.name
            if (name.isWeak()) {
                val cl = scope.getClass(clName)
                if (cl == null) throw IllegalStateException()
                clName = cl.node.name
            }
            if (superModifier.mode == Mode.ADD) {
                classWrapper.node.superName = clName
            } else if (superModifier.mode == Mode.REMOVE) {
                if (clName == "*" || clName == classWrapper.node.superName) {
                    classWrapper.node.superName = "java/lang/Object"
                }
            }
        }

        @interLoop for (interfaceModifier in interfaceModifiers) {
            val name = interfaceModifier.ident
            if (interfaceModifier.mode != Mode.ADD) {
                var clName = name.name
                if (clName == "*" && classWrapper.node.interfaces.size() > 0) {
                    continue
                }
                for (inter in classWrapper.node.interfaces) {
                    if (name.isWeak()) {
                        val cl = scope.getClass(clName)
                        if (cl == null) {
                            throw RuntimeException()
                        }
                        clName = cl.node.name
                    }
                    if (clName == inter) {
                        continue@interLoop
                    }
                }
                throw RuntimeException()
            } else {
                var clName = name.name
                if (name.isWeak()) {
                    val cl = scope.getClass(clName)
                    if (cl == null) {
                        throw RuntimeException()
                    }
                    clName = cl.node.name
                }
                classWrapper.node.interfaces.add(clName)
            }
        }


        fields
            .filter { it.mode != Mode.MATCH }
            .forEach {
                if (it.mode == Mode.REMOVE) {
                    val fieldWrapper = scope.getField(classWrapper, it.ident.name, it.descRaw)!!
                    classWrapper.node.fields.remove(classWrapper.getFieldNode(fieldWrapper))
                } else {
                    val mappedDesc = StringBuilder()
                    val desc = it.getDesc()
                    updatedTypeString(classSet, scope, mappedDesc, desc)

                    val access = (if (it.isPrivate()) Opcodes.ACC_PRIVATE else Opcodes.ACC_PUBLIC) or
                        (if (it.isStatic()) Opcodes.ACC_STATIC else 0)

                    var name = it.ident.name
                    if (it.ident.isWeak()) {
                        val field = scope.getField(classWrapper, name, mappedDesc.toString())!!
                        name = field.name
                    }

                    val node = FieldNode(Opcodes.ASM5, access, name, mappedDesc.toString(), null, it.value)
                    val fieldWrapper = FieldWrapper(classWrapper, node)
                    scope.putField(fieldWrapper, it.ident.name, it.descRaw)
                    classWrapper.fields.add(fieldWrapper)
                    classWrapper.node.fields.add(node)
                }
            }

        methods.forEach {
            if (it.mode == Mode.ADD) {
                val mappedDesc = StringBuilder("(")
                val desc = it.getDesc()
                for (type in desc.getArgumentTypes()) {
                    updatedTypeString(classSet, scope, mappedDesc, type)
                }
                mappedDesc.append(")")
                updatedTypeString(classSet, scope, mappedDesc, desc.getReturnType())

                val access = (if (it.isPrivate()) Opcodes.ACC_PRIVATE else Opcodes.ACC_PUBLIC) or
                    (if (it.isStatic()) Opcodes.ACC_STATIC else 0)
                var methodWrapper: MethodWrapper? = null

                if (((access and Opcodes.ACC_PUBLIC) != 0
                    || (access and Opcodes.ACC_PROTECTED) != 0)
                    && (access and Opcodes.ACC_STATIC) == 0) {
                    methodWrapper = searchParent(classSet, scope, classWrapper, it.ident, mappedDesc.toString())
                }

                val name = if (methodWrapper == null) it.ident.name else methodWrapper!!.name

                val node = MethodNode(Opcodes.ASM5, access, name, mappedDesc.toString(), null, null)
                if (methodWrapper == null) {
                    methodWrapper = MethodWrapper(classWrapper, node)
                } else {
                    methodWrapper!!.add(classWrapper)
                }
                scope.putMethod(methodWrapper!!, it.ident.name, it.descRaw!!)
                classWrapper.methods.add(methodWrapper!!)
                classWrapper.node.methods.add(node)
            }

            val methodWrapper = scope.getMethod(classWrapper, it.ident.name, it.descRaw!!)!!

            it.apply(classSet, scope, classWrapper.getMethodNode(methodWrapper)!!)
        }
    }

    private fun searchParent(classSet: ClassSet, scope: PatchScope, classWrapper: ClassWrapper?, name: Ident, desc: String): MethodWrapper? {
        if (classWrapper == null) {
            return null
        }
        var mw: MethodWrapper? = null

        try {
            var pName = name.name
            if (name.isWeak()) {
                val wrapper = scope.getMethod(classWrapper, pName, desc)
                if (wrapper == null) throw IllegalStateException()
                pName = wrapper!!.name
            }
            val mappedDesc = StringBuilder("(")
            val pDesc = Type.getMethodType(desc)
            for (`type` in pDesc.getArgumentTypes()) {
                updatedTypeString(classSet, scope, mappedDesc, `type`)
            }
            mappedDesc.append(")")
            updatedTypeString(classSet, scope, mappedDesc, pDesc.getReturnType())

            mw = classWrapper.getMethods(true)
                .filter { it.name == pName && it.desc == mappedDesc.toString() }
                .first
        } catch (ignored: IllegalStateException) {
        }

        if (mw == null) {
            mw = searchParent(classSet, scope, classSet.getClassWrapper(classWrapper!!.node.superName), name, desc)
            if (mw == null) {
                for (inter in classWrapper.node.interfaces) {
                    mw = searchParent(classSet, scope, classSet.getClassWrapper(inter), name, desc)
                    if (mw != null) {
                        break
                    }
                }
            }
        }
        return mw
    }

    public fun checkAttributes(logger: StateLogger, scope: PatchScope, classSet: ClassSet): Boolean {
        if (mode == Mode.ADD) return true
        val classWrapper = scope.getClass(ident.name)!!
        logger.println("- " + ident + " testing " + classWrapper.node.name)
        logger.indent()
        try {
            if (!ident.isWeak() && classWrapper.node.name != ident.name) {
                logger.println("Name mis-match " + ident + " != " + classWrapper.node.name)
                return false
            }

            var mask = 0
            when (type) {
                ClassType.CLASS -> {
                }
                ClassType.INTERFACE -> mask = Opcodes.ACC_INTERFACE
                ClassType.ENUM -> mask = Opcodes.ACC_ENUM
            }

            if (mask != 0 && (classWrapper.node.access and mask) == 0) {
                logger.println("Incorrect class type")
                return false
            }

            for (superModifier in superModifiers) {
                if (superModifier.mode != Mode.ADD) {
                    val name = superModifier.ident
                    var clName = name.name
                    if (name.isWeak()) {
                        var cl = scope.getClass(clName)
                        if (cl == null) {
                            cl = classSet.getClassWrapper(classWrapper.node.superName)
                            scope.putClass(cl!!, clName)
                        }
                        clName = cl!!.node.name
                    }
                    if (clName != "*" && clName != classWrapper.node.superName) {
                        logger.println(clName + " != " + classWrapper.node.superName)
                        return false
                    }
                }
            }

            @interLoop for (interfaceModifier in interfaceModifiers) {
                if (interfaceModifier.mode != Mode.ADD) {
                    val name = interfaceModifier.ident
                    var clName = name.name
                    if (clName == "*" && classWrapper.node.interfaces.size() > 0) {
                        continue
                    }
                    for (inter in classWrapper.node.interfaces) {
                        if (name.isWeak()) {
                            var cl = scope.getClass(clName)
                            if (cl == null) {
                                cl = classSet.getClassWrapper(inter)
                                scope.putClass(cl!!, clName)
                            }
                            clName = cl!!.node.name
                        }
                        if (clName == inter) {
                            logger.println(clName + " == " + inter)
                            continue@interLoop
                        }
                        logger.println(clName + " != " + inter)
                    }
                    logger.println("interface matching failed")
                    return false
                }
            }
            return true
        } finally {
            logger.unindent()
        }
    }

    public fun checkFields(logger: StateLogger, scope: PatchScope, classSet: ClassSet): Boolean {
        if (mode == Mode.ADD) return true
        val classWrapper = scope.getClass(ident.name)!!
        logger.println(" - " + ident + " testing " + classWrapper.node.name)
        logger.indent()
        try {
            for (f in fields) {
                if (f.mode == Mode.ADD) continue

                val fieldWrapper = scope.getField(classWrapper, f.ident.name, f.descRaw)!!

                logger.println("- " + f.ident)
                logger.println(" testing " + fieldWrapper.name)

                if (!f.ident.isWeak() && fieldWrapper.name != f.ident.name) {
                    logger.println("Name mis-match " + f.ident + " != " + fieldWrapper.name)
                    return false
                }

                val patchDesc = f.getDesc()
                val desc = Type.getType(fieldWrapper.desc)

                if (!checkTypes(classSet, scope, patchDesc, desc)) {
                    logger.println(StateLogger.typeMismatch(patchDesc, desc))
                    return false
                }

                val fieldNode = classWrapper.getFieldNode(fieldWrapper)!!

                if (((fieldNode.access and Opcodes.ACC_STATIC) == 0) == f.isStatic()) {
                    logger.println(if (f.isStatic()) "Required static" else "Required non-static")
                    return false
                }
                if (((fieldNode.access and Opcodes.ACC_PRIVATE) == 0) == f.isPrivate()) {
                    logger.println(if (f.isPrivate()) "Required private" else "Required non-private")
                    return false
                }

                if (fieldNode.value != f.value) {
                    logger.println(fieldNode.value.toString() + " != " + f.value)
                    return false
                }
                logger.println("ok")
            }
            return true
        } finally {
            logger.unindent()
        }
    }

    public fun checkMethods(logger: StateLogger, scope: PatchScope, classSet: ClassSet): Boolean {
        if (mode == Mode.ADD) return true
        val classWrapper = scope.getClass(ident.name)!!
        logger.println("- " + ident + " testing " + classWrapper.node.name)
        logger.indent()
        try {
            for (m in methods) {
                if (m.mode == Mode.ADD) continue

                val methodWrapper = scope.getMethod(classWrapper, m.ident.name, m.descRaw!!)!!

                logger.println("- " + m.ident + m.descRaw)
                logger.println(" testing " + methodWrapper.name + methodWrapper.desc)

                if (!m.ident.isWeak() && methodWrapper.name != m.ident.name) {
                    logger.println("Name mis-match " + m.ident + " != " + methodWrapper.name)
                    return false
                }

                val patchDesc = m.getDesc()
                val desc = Type.getMethodType(methodWrapper.desc)

                if (patchDesc.getArgumentTypes().size != desc.getArgumentTypes().size) {
                    logger.println("Argument size mis-match " + patchDesc.getArgumentTypes().size + " != " + desc.getArgumentTypes().size)
                    return false
                }

                for (i in 0..patchDesc.getArgumentTypes().size - 1) {
                    val pt = patchDesc.getArgumentTypes()[i]
                    val t = desc.getArgumentTypes()[i]

                    if (!checkTypes(classSet, scope, pt, t)) {
                        logger.println(StateLogger.typeMismatch(pt, t))
                        return false
                    }
                }

                if (!checkTypes(classSet, scope, patchDesc.getReturnType(), desc.getReturnType())) {
                    logger.println(StateLogger.typeMismatch(patchDesc.getReturnType(), desc.getReturnType()))
                    return false
                }
                logger.println("ok")
            }
            return true
        } finally {
            logger.unindent()
        }
    }

    public fun checkMethodsInstructions(logger: StateLogger, scope: PatchScope, classSet: ClassSet): Boolean {
        val classWrapper = scope.getClass(ident.name)!!
        logger.println("- " + ident + " testing " + classWrapper.node.name)
        logger.indent()
        try {
            for (m in methods) {
                if (m.mode == Mode.ADD) continue

                val methodWrapper = scope.getMethod(classWrapper, m.ident.name, m.descRaw!!)!!

                logger.println("- " + m.ident + m.descRaw + " testing " + methodWrapper.name + methodWrapper.desc + " instructions")

                if (!m.check(logger, classSet, scope, classWrapper.getMethodNode(methodWrapper)!!)) {
                    return false
                }
            }
            return true
        } finally {
            logger.unindent()
        }
    }

    class object {

        public fun updatedTypeString(classSet: ClassSet, scope: PatchScope, builder: StringBuilder, type: Type) {
            if (type.getSort() == Type.OBJECT) {
                builder.append("L")
                val id = Ident(type.getInternalName())
                var cls = id.name
                if (id.isWeak()) {
                    val ptcls = scope.getClass(cls)!!
                    cls = ptcls.node.name
                }
                builder.append(cls)
                builder.append(";")
            } else if (type.getSort() == Type.ARRAY) {
                for (i in 0..type.getDimensions() - 1) {
                    builder.append("[")
                }
                updatedTypeString(classSet, scope, builder, type.getElementType())
            } else {
                builder.append(type.getDescriptor())
            }
        }

        public fun checkTypes(classSet: ClassSet, scope: PatchScope?, pt: Type, t: Type): Boolean {
            if (pt.getSort() != t.getSort()) {
                return false
            }

            if (pt.getSort() == Type.OBJECT) {
                val id = Ident(pt.getInternalName())
                var cls = id.name
                if (cls != "*") {
                    if (scope != null || !id.isWeak()) {
                        if (id.isWeak()) {
                            val ptcls = scope!!.getClass(cls)
                            if (ptcls == null) {
                                // Assume true
                                cls = t.getInternalName()
                                scope.putClass(classSet.getClassWrapper(cls)!!, cls)
                                return true
                            }
                            cls = ptcls.node.name
                        }
                        if (cls != t.getInternalName()) {
                            return false
                        }
                    }
                }
            } else if (pt.getSort() == Type.ARRAY) {
                return pt.getDimensions() == t.getDimensions() && checkTypes(classSet, scope, pt.getElementType(), t.getElementType())
            } else {
                if (pt != t) {
                    return false
                }
            }
            return true
        }
    }
}