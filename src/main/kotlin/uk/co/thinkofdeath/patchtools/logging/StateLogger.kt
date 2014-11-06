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

package uk.co.thinkofdeath.patchtools.logging

import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import uk.co.thinkofdeath.patchtools.matching.MatchClass
import uk.co.thinkofdeath.patchtools.matching.MatchGroup

import java.io.PrintWriter
import java.io.StringWriter
import java.util.LinkedHashMap

public class StateLogger {

    var groups = LinkedHashMap<MatchGroup, LoggedGroup>()
    var failedTicks: Long = 0
    var writer = StringWriter()
    private var currentLevel = 0
    private val isActive = System.getProperty("patchLogging") != null

    public fun createGroup(group: MatchGroup) {
        groups.put(group, LoggedGroup(group))
    }

    public fun failedTicks(tick: Long) {
        this.failedTicks = tick
    }

    public fun println(str: String) {
        if (!isActive) return
        for (i in 0..currentLevel - 1) {
            writer.write("  ")
        }
        writer.write(str)
        writer.write('\n'.toInt())
    }

    public fun println(str: () -> String) {
        if (!isActive) return
        println(str())
    }

    public fun indent() {
        currentLevel++
    }

    public fun unindent() {
        currentLevel--
    }

    public fun getPrintWriter(): PrintWriter {
        return PrintWriter(writer)
    }

    class object {

        public fun typeMismatch(required: Type, got: Type): () -> String {
            return { "The type " + got + " did not match the required type " + required }
        }

        public fun match(node: ClassNode, clazz: MatchClass): () -> String {
            return { "Adding " + node.name + " as a possible match for " + clazz.name }
        }
    }
}
