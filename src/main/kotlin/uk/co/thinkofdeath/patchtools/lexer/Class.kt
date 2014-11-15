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

private val modifiers = setOf(
    "add",
    "remove",
    "public",
    "private",
    "protected",
    "static",
    "final",
    "synchronized"
)

private fun Lexer.classBody(): StateFunc? {
    val c = peekChar()
    if (c == '/') {
        return lexComment(skipWhitespace(toStateFunc(::classBody)))
    }
    if (c == '#') {
        return lexPatchAnnotation(skipWhitespace(toStateFunc(::classBody)))
    }
    if (c == '}') {
        skip()
        emit(TokenType.EXIT_BLOCK)
        return skipWhitespace(toStateFunc(::lexRoot), true)
    }
    for (modifier in modifiers) {
        if (nextWordMatches(modifier)) {
            revert()
            return modifierList(
                ident(toStateFunc {
                    val isArray = handleArrayType(this)

                    skipWhitespace(
                        ident(
                            skipWhitespace(toStateFunc(::methodOrField))
                        ),
                        requireOne = !isArray
                    )
                }, allowDot = true)
            )
        }
    }
    throw UnexpectedCharacterException(this, peekChar())
}

private fun Lexer.argumentList(): StateFunc? {
    if (peekChar() == ')' ) {
        skip()
        emit(TokenType.ARGUMENT_LIST_END)
        return skipWhitespace(toStateFunc(::enterMethod))
    }
    return ident(skipWhitespace(
        toStateFunc {
            handleArrayType(this)

            skipWhitespace(ident(skipWhitespace(
                toStateFunc {
                    handleArrayType(this)
                    skipWhitespace(toStateFunc @a {
                        Lexer.(): StateFunc ->
                        if (peekChar() == ',') {
                            skip()
                            emit(TokenType.ARGUMENT_LIST_NEXT)
                            return@a skipWhitespace(toStateFunc(::argumentList))
                        } else if (peekChar() == ')' ) {
                            skip()
                            emit(TokenType.ARGUMENT_LIST_END)
                            return@a skipWhitespace(toStateFunc(::enterMethod))
                        }
                        throw UnexpectedCharacterException(this, peekChar())
                    })
                }
            )))
        }
    ), true)
}


private fun handleArrayType(lexer: Lexer): Boolean {
    while (lexer.peekChar() == '[') {
        // Arrays are weird :3
        lexer.skip()
        var ok = false
        while (!lexer.isEOF()) {
            if (lexer.peekChar().isWhitespace()) {
                lexer.skip()
                continue
            }
            if (lexer.peekChar() == ']') {
                lexer.skip()
                lexer.emit(TokenType.ARRAY_TYPE)
                ok = true
                break
            }
            throw UnexpectedCharacterException(lexer, lexer.peekChar())
        }
        if (!ok) throw LexerException(lexer, "Unclosed array type")
    }
    return false
}

private fun Lexer.methodOrField(): StateFunc? {
    if (peekChar() == '(') {
        // Method
        skip()
        emit(TokenType.ARGUMENT_LIST)
        return skipWhitespace(toStateFunc(::argumentList))
    }
    // Field
    if (peekChar() == ';') {
        skip()
        emit(TokenType.FIELD_END)
        return skipWhitespace(toStateFunc(::classBody))
    }
    if (peekChar() == '=') {
        skip()
        drop()
        return skipWhitespace(toStateFunc {
            while (!isEOF() && peekChar() != ';') {
                skip()
            }
            emit(TokenType.FIELD_VALUE)
            skip()
            drop()
            skipWhitespace(toStateFunc(::classBody))
        })
    }
    throw UnexpectedCharacterException(this, peekChar())
}

private fun Lexer.modifierList(cb: StateFunc): StateFunc {
    return toStateFunc {
        Lexer.(): StateFunc ->
        for (modifier in modifiers) {
            if (nextWordMatches(modifier)) {
                emit(TokenType.MODIFIER)
                return@toStateFunc skipWhitespace(modifierList(cb), requireOne = true)
            }
        }
        cb
    }
}

private fun Lexer.enterClass(): StateFunc? {
    return toStateFunc {
        if (peekChar() == '{') {
            skip()
            emit(TokenType.ENTER_BLOCK)
            skipWhitespace(toStateFunc(::classBody))
        } else if (nextWordMatches("extends")) {
            emit(TokenType.EXTENDS_LIST)
            skipWhitespace(identList())
        } else if (nextWordMatches("implements")) {
            emit(TokenType.IMPLEMENTS_LIST)
            skipWhitespace(identList())
        } else {
            throw UnexpectedCharacterException(this, peekChar())
        }
    }
}

private fun Lexer.identList(identNeeded: Boolean = true): StateFunc {
    return toStateFunc {
        if (!identNeeded) {
            if (peekChar() != ',') {
                emit(TokenType.IDENT_LIST_END)
                toStateFunc(::enterClass)
            } else {
                skip()
                emit(TokenType.IDENT_LIST_NEXT)
                skipWhitespace(identList(true))
            }
        } else {
            ident(skipWhitespace(identList(false)), true)
        }

    }
}