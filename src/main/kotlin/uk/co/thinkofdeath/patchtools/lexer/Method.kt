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

package uk.co.thinkofdeath.patchtools.lexer

private fun Lexer.enterMethod(): StateFunc? {
    if (nextChar() != '{') {
        revert()
        throw UnexpectedCharacterException(this, peekChar())
    }
    emit(TokenType.ENTER_BLOCK)
    return skipWhitespace(methodBlock(toStateFunc(::classBody)))
}

private fun Lexer.methodBlock(cb: StateFunc): StateFunc {
    return toStateFunc {
        Lexer.(): StateFunc ->
        val c = peekChar()
        if (c == '/') {
            return@toStateFunc lexComment(skipWhitespace(methodBlock(cb)))
        }
        if (c == '#') {
            return@toStateFunc lexPatchAnnotation(skipWhitespace(methodBlock(cb)))
        }
        if (c == '+') {
            skip()
            emit(TokenType.ADD_INSTRUCTION)
            return@toStateFunc lexInstruction(skipWhitespace(methodBlock(cb)))
        }
        if (c == '-') {
            skip()
            emit(TokenType.REMOVE_INSTRUCTION)
            return@toStateFunc lexInstruction(skipWhitespace(methodBlock(cb)))
        }
        if (c == '.') {
            skip()
            emit(TokenType.MATCH_INSTRUCTION)
            return@toStateFunc lexInstruction(skipWhitespace(methodBlock(cb)))
        }
        if (c == '}') {
            skip()
            emit(TokenType.EXIT_BLOCK)
            return@toStateFunc skipWhitespace(cb)
        }
        throw UnexpectedCharacterException(this, peekChar())
    }
}

private fun Lexer.lexInstruction(cb: StateFunc): StateFunc {
    return toStateFunc {
        while (true) {
            if (isEOF()) throw LexerException(this, "Unexpected end of file")
            if (peekChar() == '\n') {
                emit(TokenType.INSTRUCTION)
                break
            }
            skip()
        }
        cb
    }
}