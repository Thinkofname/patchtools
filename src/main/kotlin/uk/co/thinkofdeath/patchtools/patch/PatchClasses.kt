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
import uk.co.thinkofdeath.patchtools.lexer.Lexer
import uk.co.thinkofdeath.patchtools.lexer.TokenType
import uk.co.thinkofdeath.patchtools.lexer.Token

public class PatchClasses(reader: Lexer) {

    public val classes: MutableList<PatchClass> = ArrayList()

        ;{

        val it = reader.iterator()
        val modifiers = hashSetOf<String>()
        val patchAnnotations = arrayListOf<String>()
        for (token in it) {
            if (token.type == TokenType.COMMENT) {
                continue
            }
            if (token.type == TokenType.PATCH_ANNOTATION) {
                patchAnnotations.add(token.value)
                continue
            }

            if (token.type == TokenType.MODIFIER) {
                modifiers.add(token.value)
                continue
            }

            if (token.type == TokenType.CLASS) {
                classes.add(PatchClass(ClassType.CLASS, it, modifiers, patchAnnotations))
                modifiers.clear()
                patchAnnotations.clear()
                continue
            }

            throw ValidateException("Unexpected ${token.type}")
                .setLineNumber(token.lineNumber)
                .setLineOffset(token.lineOffset)
        }
    }

    public fun getClass(name: String): PatchClass? {
        return classes
            .filter { it.ident.name == name }
            .first
    }
}

fun Token.expect(type: TokenType): Token {
    if (this.type != type) {
        throw ValidateException("Expected $type but found ${this.type}")
            .setLineNumber(lineNumber)
            .setLineOffset(lineOffset)
    }
    return this
}

