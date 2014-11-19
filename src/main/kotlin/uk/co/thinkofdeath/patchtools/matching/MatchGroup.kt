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

import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

public class MatchGroup(private val classSet: ClassSet) {

    private val classes = hashMapOf<String, MatchClass>()
    public val first: MatchClass
        get() = classes.values().first()

    public fun add(cls: MatchClass) {
        val wrp = classSet.getClassWrapper(cls.name)
        if (cls.name == "*" || (wrp != null && wrp.isHidden())) {
            return
        }
        if (!classes.containsKey(cls.name)) {
            classes.put(cls.name, cls)
        }
    }

    public fun merge(other: MatchGroup) {
        classes.putAll(other.classes)
    }

    public fun getClasses(): Collection<MatchClass> {
        return classes.values()
    }

    public fun getClass(owner: MatchClass): MatchClass {
        var o = classes.values()
            .filter { it == owner }
            .first ?: owner
        return o
    }
}
