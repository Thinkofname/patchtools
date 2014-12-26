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

import java.util.ArrayList

class Lexer(private val data: String) : Iterable<Token> {

    private var stateFunc: StateFunc? = skipWhitespace(toStateFunc(::lexRoot), true);
    internal val tokenQueue: MutableList<Token> = ArrayList()

    private var offset: Int = 0
    private var readOffset: Int = 0

    private fun getNextToken(): Token? {
        while (stateFunc != null && tokenQueue.empty) {
            stateFunc = stateFunc?.exec(this)
        }
        return if (tokenQueue.notEmpty) tokenQueue.remove(0) else null
    }

    internal fun emit(type: TokenType) {
        tokenQueue.add(Token(
            currentLine(offset),
            lineOffset(offset),
            type,
            data.substring(offset, readOffset)
        ))
        offset = readOffset
    }

    internal fun isEOF(): Boolean {
        return readOffset == data.size
    }

    internal fun nextChar(): Char {
        return data.charAt(readOffset++)
    }

    internal fun peekChar(): Char {
        return data.charAt(readOffset)
    }

    internal fun drop() {
        offset = readOffset
    }

    internal fun revert() {
        readOffset = offset
    }

    internal fun back(count: Int = 1) {
        if (readOffset - count < offset) throw IllegalStateException()
        readOffset -= count
    }

    internal fun skip(count: Int = 1) {
        if (readOffset + count > data.size) throw IllegalStateException()
        readOffset += count
    }

    internal fun nextWordMatches(word: String): Boolean {
        if (readOffset + word.size > data.size) return false
        for (i in 0..word.size - 1) {
            if (data.charAt(readOffset + i) != word.charAt(i)) {
                return false
            }
        }
        readOffset += word.size
        return true
    }

    internal fun currentLine(off: Int = readOffset): Int {
        return data.substring(0, off).count { it == '\n' } + 1
    }

    internal fun lineOffset(off: Int = readOffset): Int {
        return off - data.substring(0, off).lastIndexOf('\n')
    }

    override fun iterator(): Iterator<Token> {
        return object : Iterator<Token> {
            var nextToken: Token? = null

            override fun next(): Token {
                val token = nextToken;
                if (token != null) {
                    nextToken = null
                    return token
                }
                return getNextToken() ?: throw IllegalStateException()
            }

            override fun hasNext(): Boolean {
                if (nextToken == null) {
                    nextToken = getNextToken()
                    return nextToken != null
                }
                return true
            }
        }
    }

}

private trait StateFunc {
    fun exec(lex: Lexer): StateFunc?
}

class UnexpectedCharacterException(lexer: Lexer, c: Char) : Exception("Unexpected $c at L${lexer.currentLine()}:${lexer.lineOffset()}")

class LexerException(lexer: Lexer, msg: String = "Error") : Exception("$msg at L${lexer.currentLine()}:${lexer.lineOffset()}")