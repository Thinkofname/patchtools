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

import uk.co.thinkofdeath.patchtools.instruction.Instruction

import java.util.Arrays

public class PatchInstruction(command: Command, reader: LineReader) {

    public var mode: Mode
    public var instruction: Instruction
    public var params: Array<String>
    public var meta: MutableList<String> = arrayListOf()

        ;{
        mode = command.mode
        instruction = Instruction.valueOf(command.name.toUpperCase().replace('-', '_'))
        params = command.args
        if (instruction.isMetaRequired()) {
            reader.whileHasLine {
                (it: String): Boolean ->
                val l = it.trim()
                if (l.startsWith("//") || l.length() == 0) {
                    return@whileHasLine false
                }

                if (l.equalsIgnoreCase(".-+".charAt(mode.ordinal()) + "end-" + command.name.toLowerCase())) {
                    return@whileHasLine true
                }

                meta.add(l)
                return@whileHasLine false
            }
        }
    }

    override fun toString(): String {
        return "PatchInstruction{" +
            "mode=" + mode +
            ", instruction=" + instruction +
            ", params=" + Arrays.toString(params) +
            ", meta=" + meta +
            '}'
    }
}
