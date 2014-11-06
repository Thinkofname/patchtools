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

import java.io.File
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date

public class LoggableException(logger: StateLogger) : RuntimeException(LoggableException.dump(logger)) {

    class object {
        private fun dump(logger: StateLogger): String {
            var name = SimpleDateFormat("yyyy-mm-dd-hh-mm-ss-S").format(Date()) + ".log"
            try {
                PrintWriter(File(name)).use { writer ->
                    writer.println("Groups: " + logger.groups.size())
                    for (group in logger.groups.keySet()) {
                        writer.print("  Classes: ")
                        writer.println(group.getClasses().size())
                        for (clazz in group.getClasses()) {
                            writer.print("    ")
                            writer.print(clazz.name)
                            writer.print(" ")
                            writer.println(clazz.matches.size())
                            writer.print("    [ ")
                            writer.print(clazz.matches
                                .map { it.name }
                                .join(", "))
                            writer.println(" ]")
                        }
                    }
                    writer.println("Failed after " + logger.failedTicks + " tests")
                    writer.println("Walk-through: ")
                    writer.println(logger.writer.getBuffer().toString())
                }
            } catch (e: FileNotFoundException) {
                name = "failed to create log: " + e.getMessage()
            }

            return name
        }
    }

}
