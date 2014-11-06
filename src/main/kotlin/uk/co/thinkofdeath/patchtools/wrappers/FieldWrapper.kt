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

import org.objectweb.asm.tree.FieldNode
import java.util.HashSet

public class FieldWrapper(classWrapper: ClassWrapper, node: FieldNode) {

    private val classSet: ClassSet
    public val name: String
    public val desc: String
    private val value: Any?
    private val classWrappers = HashSet<ClassWrapper>()
    public var hidden: Boolean = false

    {
        this.classSet = classWrapper.classSet
        classWrappers.add(classWrapper)
        name = node.name
        desc = node.desc
        value = node.value
    }

    public fun isHidden(): Boolean {
        return hidden
    }

    override fun toString(): String {
        return "FieldWrapper{" + name + " " + desc + "} " + classWrappers
    }

    public fun add(classWrapper: ClassWrapper) {
        classWrappers.add(classWrapper)
    }

    public fun has(wrapper: ClassWrapper): Boolean {
        return wrapper in classWrappers
    }
}