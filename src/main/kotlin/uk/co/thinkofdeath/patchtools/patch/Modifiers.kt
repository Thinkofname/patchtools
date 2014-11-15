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

import org.objectweb.asm.Opcodes

private val modifierAccess = mapOf(
    "private" to Opcodes.ACC_PRIVATE,
    "public" to Opcodes.ACC_PUBLIC,
    "protected" to Opcodes.ACC_PROTECTED,
    "static" to Opcodes.ACC_STATIC,
    "final" to Opcodes.ACC_FINAL,
    "synchronized" to Opcodes.ACC_SYNCHRONIZED
)

private val classModifiers =
    Opcodes.ACC_PUBLIC or
        Opcodes.ACC_PRIVATE or
        Opcodes.ACC_PROTECTED or
        Opcodes.ACC_FINAL

private val fieldModifiers =
    Opcodes.ACC_PUBLIC or
        Opcodes.ACC_PRIVATE or
        Opcodes.ACC_PROTECTED or
        Opcodes.ACC_FINAL or
        Opcodes.ACC_STATIC

private val methodModifiers =
    Opcodes.ACC_PUBLIC or
        Opcodes.ACC_PRIVATE or
        Opcodes.ACC_PROTECTED or
        Opcodes.ACC_FINAL or
        Opcodes.ACC_STATIC or
        Opcodes.ACC_SYNCHRONIZED