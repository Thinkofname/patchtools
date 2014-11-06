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

package uk.co.thinkofdeath.patchtools.matching

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

trait MatchPair {
    fun apply()
}


public class ClassMatch(
    private val matchClass: MatchClass,
    private val classNode: ClassNode) : MatchPair {

    override fun apply() {
        matchClass.addMatch(classNode)
    }
}

public class MethodMatch(
    private val matchMethod: MatchMethod,
    private val classNode: ClassNode,
    private val methodNode: MethodNode) : MatchPair {

    override fun apply() {
        matchMethod.addMatch(classNode, methodNode)
    }
}

public class FieldMatch(
    private val matchField: MatchField,
    private val classNode: ClassNode,
    private val fieldNode: FieldNode) : MatchPair {

    override fun apply() {
        matchField.addMatch(classNode, fieldNode)
    }
}