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

import gnu.trove.map.hash.TObjectIntHashMap
import org.objectweb.asm.Type
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.logging.LoggableException
import uk.co.thinkofdeath.patchtools.logging.StateLogger
import uk.co.thinkofdeath.patchtools.patch.*
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

import java.util.*

public class MatchGenerator(private val classSet: ClassSet, private val patchClasses: PatchClasses, private val scope: PatchScope) {
    private val groups = ArrayList<MatchGroup>()

    private val state = TObjectIntHashMap<Any>()

    private val logger = StateLogger()

        ;{

        try {
            // To work out the links between the patch classes
            // we start with a the first class and branch out.
            // Then we move onto the next unvisited class since
            // not every class in a patch may be linked. This
            // allows us to split some patches into smaller
            // sets which are quicker to match and apply
            generateGroups()
            for (it in groups) {
                logger.createGroup(it)
            }

            // As a base every class would be matched to every
            // class in the class set, for patches with more
            // than one class this becomes a large number of
            // tests to work with. To reduce the number of
            // groups only the first class is given every
            // class in the set and then the patch is partially
            // tested (without the checking of class names just
            // types and instructions) to reduce the number of
            // classes, the references from the remaining classes
            // are used to match the others up. With patches that
            // have a good amount of information this normally
            // leaves one or two classes per a patch class
            reduceGroups()

            // Setup the initial state

            groups
                .flatMap { it.getClasses() }
                .forEach {
                    state.put(it, 0)
                    it.methods.forEach { state.put(it, 0) }
                    it.fields.forEach { state.put(it, 0) }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            e.printStackTrace(logger.getPrintWriter())
            throw LoggableException(logger)
        }

    }

    private fun reduceGroups() {
        for (group in groups) {

            val first = group.first!!

            // Add every class as a match to the first
            // patch class in the set
            classSet.classes(true)
                .map { classSet.getClassWrapper(it)!! }
                .map { it.node }
                .forEach {
                    first.addMatch(it)
                }

            logger.println("Adding all classes to " + first.name)

            // Marks whether we made any changes in the last
            // cycle
            var doneSomething = true
            while (doneSomething) {
                doneSomething = false
                while (true) {
                    val clazz = group.getClasses()
                        .filter { it.hasUnchecked() }
                        .first
                    if (clazz == null) {
                        break
                    }
                    doneSomething = true
                    logger.println("Checking " + clazz.name)
                    logger.indent()

                    val unchecked = clazz.getUncheckedClasses()
                    unchecked.forEach {
                        clazz.check(logger, classSet, it)
                    }

                    logger.unindent()
                }

                while (true) {
                    val field = group.getClasses()
                        .flatMap { it.fields }
                        .filter { it.hasUnchecked() }
                        .first
                    if (field == null) {
                        break
                    }
                    doneSomething = true
                    logger.println("Checking " + field.owner.name + "." + field.name)
                    logger.indent()

                    val unchecked = field.getUncheckedMethods()
                    unchecked.forEach {
                        field.check(logger, classSet, group, it)
                    }
                    logger.unindent()
                }

                while (true) {
                    val method = group.getClasses()
                        .flatMap { it.methods }
                        .filter { it.hasUnchecked() }
                        .first
                    if (method == null) {
                        break
                    }
                    doneSomething = true
                    logger.println("Checking " + method.owner.name + "::" + method.name + method.desc)
                    logger.indent()

                    val unchecked = method.getUncheckedMethods()
                    unchecked.forEach {
                        method.check(logger, classSet, patchClasses, group, it)
                    }

                    logger.unindent()
                }

                if (!doneSomething) {
                    val classes = classSet.classes(true)
                    // Check for classes without a match and as a last ditch
                    // method check against the rest of the classes
                    val anyUnmatched = group.getClasses()
                        .filter { it.matches.isEmpty() }
                        .any { !it.hasChecked(classes.size) }

                    if (anyUnmatched) {
                        group.getClasses()
                            .filter { it.matches.isEmpty() }
                            .filter { !it.hasChecked(classes.size) }
                            .forEach {
                                val c = it
                                classes
                                    .map { classSet.getClassWrapper(it)!! }
                                    .map { it.node }
                                    .forEach {
                                        c.addMatch(it)
                                    }
                            }
                        doneSomething = true
                    }
                }
            }

            // Remove incomplete classes
            for (cls in group.getClasses()) {
                val matches = ArrayList(cls.matches)
                matches
                    .filter {
                        val clazz = it
                        cls.methods.any { !it.usesNode(clazz) }
                            || cls.fields.any { !it.usesNode(clazz) }
                    }
                    .forEach {
                        val clazz = it
                        cls.removeMatch(clazz)
                        cls.methods.forEach { it.removeMatch(clazz) }
                        cls.fields.forEach { it.removeMatch(clazz) }
                    }
            }
        }
    }

    private fun generateGroups() {
        val visited = HashMap<MatchClass, MatchGroup>()
        patchClasses.classes
            .filter { it.mode != Mode.ADD }
            .forEach {
                val cls = MatchClass(it.ident.name)
                if (cls in visited) {
                    return
                }

                val group = MatchGroup(classSet)

                visited.put(cls, group)
                groups.add(group)
                val visitList = Stack<MatchClass>()
                visitList.add(cls)
                while (!visitList.isEmpty()) {
                    val mc = group.getClass(visitList.pop())
                    val pc = patchClasses.getClass(mc.name)
                    if (pc == null || pc.mode == Mode.ADD) continue
                    group.add(mc)

                    pc.superModifiers
                        .filter { it.mode != Mode.ADD }
                        .forEach {
                            val matchClass = group.getClass(MatchClass(it.ident.name))
                            mc.superClass = matchClass
                            addToVisited(visited, visitList, matchClass, group)
                        }
                    pc.interfaceModifiers
                        .filter { it.mode != Mode.ADD }
                        .forEach {
                            val matchClass = group.getClass(MatchClass(it.ident.name))
                            mc.addInterface(matchClass)
                            addToVisited(visited, visitList, matchClass, group)
                        }
                    pc.fields
                        .filter { it.mode != Mode.ADD }
                        .forEach {
                            val type = it.desc

                            var field = MatchField(mc, it.ident.name, it.descRaw)
                            field = mc.addField(field)

                            val rt = getRootType(type)
                            if (rt.getSort() == Type.OBJECT) {
                                val argCls = MatchClass(Ident(rt.getInternalName()).name)
                                addToVisited(visited, visitList, group.getClass(argCls), group)
                            }
                            field.type = type
                        }
                    pc.methods
                        .filter { it.mode != Mode.ADD }
                        .forEach {
                            val desc = it.desc

                            val mTemp = MatchMethod(mc, it.ident.name, it.descRaw)
                            val method = mc.addMethod(mTemp)

                            if (mTemp == method) {
                                for (type in desc.getArgumentTypes()) {
                                    val rt = getRootType(type)
                                    if (rt.getSort() == Type.OBJECT) {
                                        val argCls = MatchClass(Ident(rt.getInternalName()).name)
                                        addToVisited(visited, visitList, group.getClass(argCls), group)
                                    }
                                    method.addArgument(type)
                                }
                                val type = desc.getReturnType()
                                val rt = getRootType(type)
                                if (rt.getSort() == Type.OBJECT) {
                                    val argCls = MatchClass(Ident(rt.getInternalName()).name)
                                    addToVisited(visited, visitList, group.getClass(argCls), group)
                                }
                                method.setReturn(type)
                            }

                            for (instruction in it.instructions) {
                                val insn = instruction.instruction
                                if (insn.handler == null || instruction.mode == Mode.ADD) continue
                                insn.handler!!.getReferencedClasses(instruction).forEach {
                                    addToVisited(visited, visitList, it, group)
                                }
                                insn.handler!!.getReferencedMethods(instruction).forEach {
                                    val owner = group.getClass(it.owner)
                                    owner.addMethod(it)
                                }
                                insn.handler!!.getReferencedFields(instruction).forEach {
                                    val owner = group.getClass(it.owner)
                                    owner.addField(it)
                                }
                            }
                        }
                }
            }
    }

    public fun apply(): PatchScope {
        try {
            val scopes = ArrayList<PatchScope>()
            @groupCheck for (group in groups) {

                val tickList = generateTickList(group)

                var tick: Long = 0

                do {
                    tick++

                    val testScope = generateScope(group, PatchScope(scope))
                    if (testScope == null) continue

                    if (test(group, testScope)) {
                        scopes.add(testScope)
                        continue@groupCheck
                    }
                } while (tick(tickList))
                logger.failedTicks(tick)
                throw LoggableException(logger)
            }
            val finalScope = PatchScope(scope)
            scopes.forEach { finalScope.merge(it) }
            return finalScope
        } catch (e: Exception) {
            e.printStackTrace(logger.getPrintWriter())
            if (e is LoggableException) {
                throw e
            }
            throw LoggableException(logger)
        }

    }

    private fun test(group: MatchGroup, scope: PatchScope): Boolean {
        val classes = group.getClasses()
            .map { patchClasses.getClass(it.name) }
            .filterNotNull()
        // Slightly faster to do it this way since the instruction checking is the heaviest
        return classes.all { it.checkAttributes(logger, scope, classSet) }
            && classes.all { it.checkFields(logger, scope, classSet) }
            && classes.all { it.checkMethods(logger, scope, classSet) }
            && classes.all { it.checkMethodsInstructions(logger, scope, classSet) }
    }

    private fun generateTickList(group: MatchGroup): List<Any> {
        val tickList = ArrayList<Any>()
        group.getClasses().forEach {
            tickList.add(it)
            it.fields.forEach { tickList.add(it) }
            it.methods.forEach { tickList.add(it) }
        }
        return tickList
    }

    private fun tick(tickList: List<Any>): Boolean {
        var i = tickList.size() - 1
        while (i >= 0) {
            val o = tickList.get(i)
            var index = state.get(o) + 1
            if (o is MatchClass) {
                if (index >= o.matches.size()) {
                    index = 0
                    state.put(o, index)
                    i--
                    continue
                }
                state.put(o, index)
                return true
            } else if (o is MatchMethod) {
                val matchClass = nearestClass(tickList, i)
                val cls = matchClass.matches.get(state.get(matchClass))
                if (index >= o.getMatches(cls).size()) {
                    index = 0
                    state.put(o, index)
                    i--
                    continue
                }
                state.put(o, index)
                return true
            } else if (o is MatchField) {
                val matchClass = nearestClass(tickList, i)
                val cls = matchClass.matches.get(state.get(matchClass))
                if (index >= o.getMatches(cls).size()) {
                    index = 0
                    state.put(o, index)
                    i--
                    continue
                }
                state.put(o, index)
                return true
            }
        }
        return false
    }

    private fun nearestClass(tickList: List<Any>, index: Int): MatchClass {
        var i = index
        while (i >= 0) {
            if (tickList.get(i) is MatchClass) {
                return tickList.get(i) as MatchClass
            }
            i--
        }
        throw IllegalStateException()
    }

    private fun generateScope(group: MatchGroup, scope: PatchScope): PatchScope? {
        for (c in group.getClasses()) {

            if (c.matches.isEmpty()) {
                throw LoggableException(logger)
            }

            val cls = classSet.getClassWrapper(c.matches.get(state.get(c)).name)!!
            if (scope.putClass(cls, c.name)) {
                return null
            }

            for (f in c.fields) {
                val matches = f.getMatches(cls.node)
                if (matches.isEmpty()) {
                    throw LoggableException(logger)
                }
                val node = matches.get(state.get(f))
                val met = cls.getField(node.name, node.desc)!!
                if (scope.putField(met, f.name, f.desc)) {
                    return null
                }
            }

            for (m in c.methods) {
                val matches = m.getMatches(cls.node)
                if (matches.isEmpty()) {
                    throw LoggableException(logger)
                }
                val node = matches.get(state.get(m))
                val met = cls.getMethod(node.name, node.desc)!!
                if (scope.putMethod(met, m.name, m.desc)) {
                    return null
                }
            }
        }
        return scope
    }

    private fun addToVisited(visited: MutableMap<MatchClass, MatchGroup>, visitList: Stack<MatchClass>, matchClass: MatchClass, group: MatchGroup) {
        if (patchClasses.getClass(matchClass.name) == null) return
        if (!visited.containsKey(matchClass)) {
            visited[matchClass] = group
            visitList.push(matchClass)
        } else if (visited.get(matchClass) != group) {
            val other = visited.get(matchClass)!!
            group.merge(other)
            other.getClasses().forEach {
                visited[it] = group
            }
            groups.remove(other)
        }
    }

    class object {

        public fun getRootType(`type`: Type): Type {
            if (`type`.getSort() == Type.ARRAY) {
                return getRootType(`type`.getElementType())
            }
            return `type`
        }
    }
}