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

/**
 * Initial start of the lexer, supports
 * - Comments
 * - Decompiler annotations
 * - classes
 */
private fun Lexer.lexRoot(): StateFunc? {
    val c = peekChar()
    if (c == '/') {
        return lexComment(skipWhitespace(toStateFunc(::lexRoot), true))
    }
    if (c == '#') {
        return lexPatchAnnotation(skipWhitespace(toStateFunc(::lexRoot), true))
    }
    for (modifier in modifiers) {
        if (nextWordMatches(modifier)) {
            revert()
            return modifierList(toStateFunc(::lexRoot))
        }
    }
    if (nextWordMatches("class")) {
        emit(TokenType.CLASS)
        return skipWhitespace(
            ident(
                skipWhitespace(toStateFunc(::enterClass), requireOne = true),
                allowDot = true
            )
            , requireOne = true)
    }
    if (nextWordMatches("import")) {
        emit(TokenType.IMPORT)
        return skipWhitespace(
            ident(
                skipWhitespace(toStateFunc {
                    if (peekChar() != ';') {
                        throw UnexpectedCharacterException(this, peekChar())
                    }
                    skip()
                    drop()
                    skipWhitespace(toStateFunc(::lexRoot))
                }),
                allowDot = true
            ), requireOne = true
        )
    }
    throw UnexpectedCharacterException(this, peekChar())
}

/**
 * Parses a single identifier, allowDot controls whether
 * '.'s should be allowed in identifiers (e.g. class+package
 * names)
 */
private fun Lexer.ident(cb: StateFunc, allowDot: Boolean = false): StateFunc {
    return toStateFunc {
        var count = 0
        while (true) {
            if (isEOF()) throw LexerException(this, "Unexpected end of file")
            if (peekChar().isJavaIdentifierPart()
                || (count == 0 && peekChar().isJavaIdentifierStart())
                || (allowDot && peekChar() == '.')
                || peekChar() == '~'
                || peekChar() == '+'
                || peekChar() == '-'
                || peekChar() == '<' // TODO: Shouldn't really do it this way
                || peekChar() == '>'
            ) {
                count++
                skip()
            } else {
                break
            }
        }
        if (count == 0) throw LexerException(this, "Expected identifier")
        emit(TokenType.IDENT)
        cb
    }
}

private fun Lexer.lexPatchAnnotation(cb: StateFunc): StateFunc {
    return toStateFunc {
        skip()
        drop()
        while (true) {
            if (isEOF()) throw LexerException(this, "Unexpected end of file")
            if (peekChar() == '\n') {
                emit(TokenType.PATCH_ANNOTATION)
                break
            }
            skip()
        }
        cb
    }
}

private fun Lexer.lexComment(cb: StateFunc): StateFunc {
    return toStateFunc {
        skip()
        when (nextChar()) {
            '/' -> {
                drop()
                while (true) {
                    if (isEOF()) throw LexerException(this, "Unexpected end of file")
                    if (peekChar() == '\n') {
                        emit(TokenType.COMMENT)
                        break
                    }
                    skip()
                }
            }
            '*' -> {
                drop()
                var couldEnd = false
                while (true) {
                    if (isEOF()) throw LexerException(this, "Unexpected end of file")
                    if (peekChar() == '*') {
                        couldEnd = true
                    } else if (peekChar() == '/' && couldEnd) {
                        back()
                        emit(TokenType.COMMENT)
                        skip(2)
                        drop()
                        break;
                    } else {
                        couldEnd = false
                    }
                    skip()
                }
            }
            else -> {
                revert()
                throw UnexpectedCharacterException(this, nextChar())
            }
        }
        cb
    }
}

/**
 * Skips whitespace characters until a non-whitespace
 * character is encountered. If EOF is reached then a
 * IllegalStateException will be thrown unless endIfEOF
 * is true
 */
private fun Lexer.skipWhitespace(cb: StateFunc, endIfEOF: Boolean = false, requireOne: Boolean = false): StateFunc {
    return toStateFunc {
        var count = 0
        while (!isEOF()) {
            val c = nextChar()
            if (!c.isWhitespace()) {
                revert()
                break
            }
            count++
            drop()
        }
        if (count == 0 && requireOne) {
            throw LexerException(this, "Expected whitespace")
        }
        if (isEOF()) {
            if (endIfEOF) {
                null
            } else {
                throw LexerException(this, "Unexpected end of file")
            }
        } else {
            cb
        }
    }
}


private fun toStateFunc(func: Lexer.() -> StateFunc?): StateFunc = StateWrapper(func)
class StateWrapper(private val func: Lexer.() -> StateFunc?) : StateFunc {
    override fun exec(lex: Lexer): StateFunc? = lex.func()
}
