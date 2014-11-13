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

import java.util.Arrays

class Command private(
    internal val mode: Mode,
    internal val name: String,
    internal val args: Array<String>
) {

    override fun toString(): String {
        return "Command{" + "mode=" + mode + ", name='" + name + '\'' + ", args=" + Arrays.toString(args) + '}'
    }

    class object {

        public fun from(line: String): Command {
            val args = line.split(" ")
            if (args.size < 1) throw IllegalArgumentException()
            val mode = args[0].charAt(0)

            val command = Command(
                Mode.values()[".-+".indexOf(mode)],
                args[0].substring(1).toLowerCase(),
                args.copyOfRange(1, args.size)
            )
            return command
        }
    }
}
