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

package uk.co.thinkofdeath.patchtools.wrappers

import org.objectweb.asm.tree.MethodNode
import java.util.HashSet

public class MethodWrapper(classWrapper: ClassWrapper, node: MethodNode) {

    private val classSet: ClassSet
    private val classWrappers = HashSet<ClassWrapper>()
    public val name: String
    public val desc: String
    var hidden: Boolean = false

    {
        this.classSet = classWrapper.classSet
        classWrappers.add(classWrapper)
        name = node.name
        desc = node.desc
    }

    public fun isHidden(): Boolean {
        return hidden
    }

    public fun add(classWrapper: ClassWrapper) {
        classWrappers.add(classWrapper)
    }

    public fun add(methodWrapper: MethodWrapper) {
        for (cls in methodWrapper.classWrappers) {
            add(cls)
        }
    }

    public fun has(classWrapper: ClassWrapper): Boolean {
        return classWrapper in classWrappers
    }

    override fun toString(): String {
        return "MethodWrapper{" + name + desc + "}"
    }
}