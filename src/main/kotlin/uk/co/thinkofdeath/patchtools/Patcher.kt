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

import uk.co.thinkofdeath.patchtools.wrappers.ClassSet
import java.io.InputStream
import java.io.Reader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import uk.co.thinkofdeath.patchtools.patch.PatchClasses
import uk.co.thinkofdeath.patchtools.matching.MatchGenerator
import uk.co.thinkofdeath.patchtools.lexer.Lexer

class Patcher(val classes: ClassSet) {
    {
        classes.simplify()
    }

    fun apply(inputStream: InputStream): PatchScope {
        return apply(InputStreamReader(inputStream, StandardCharsets.UTF_8))
    }

    fun apply(reader: Reader): PatchScope {
        return apply(reader, PatchScope())
    }

    fun apply(reader: Reader, patchScope: PatchScope): PatchScope {
        reader.use {
            return apply(PatchClasses(Lexer(reader.readText())), patchScope)
        }
    }

    fun apply(patchClasses: PatchClasses, patchScope: PatchScope): PatchScope {
        val generator = MatchGenerator(classes, patchClasses, patchScope)
        val foundScope = generator.apply()
        patchClasses.classes.forEach {
            it.apply(foundScope, classes)
        }
        return foundScope
    }
}