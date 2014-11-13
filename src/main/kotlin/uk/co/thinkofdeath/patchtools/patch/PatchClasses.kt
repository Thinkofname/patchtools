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

import java.util.ArrayList

public class PatchClasses(reader: LineReader) {

    public val classes: MutableList<PatchClass> = ArrayList()

        ;{
        var line: String? = null
        while ({ line = reader.readLine(); line != null }()) {
            val l = line!!.trim()
            if (l.startsWith("//") || l.length() == 0) continue

            val command = Command.from(l)
            when (command.name) {
                "interface", "enum", "class" -> classes.add(PatchClass(command, reader))
                else -> throw ValidateException("Unexpected " + command.name).setLineNumber(reader.lineNumber)
            }
        }
    }

    public fun getClass(name: String): PatchClass? {
        return classes
            .filter { it.ident.name == name }
            .first
    }
}

