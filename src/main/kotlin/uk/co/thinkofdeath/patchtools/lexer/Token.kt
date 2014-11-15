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

data class Token(
    val lineNumber: Int,
    val lineOffset: Int,
    val type: TokenType,
    val value: String
)

enum class TokenType {
    COMMENT
    PATCH_ANNOTATION
    CLASS
    IDENT
    ENTER_BLOCK
    EXIT_BLOCK
    EXTENDS_LIST
    IMPLEMENTS_LIST
    IDENT_LIST_NEXT
    IDENT_LIST_END
    MODIFIER
    ARGUMENT_LIST
    ARGUMENT_LIST_END
    ARGUMENT_LIST_NEXT
    ARRAY_TYPE
    INSTRUCTION
    MATCH_INSTRUCTION
    ADD_INSTRUCTION
    REMOVE_INSTRUCTION
    FIELD_END
    FIELD_VALUE
    IMPORT
}