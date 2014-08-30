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

package uk.co.thinkofdeath.patchtools.matching;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;

import java.util.Collection;
import java.util.Map;

public class MatchGroup {

    private Map<String, MatchClass> classes = Maps.newHashMap();
    private MatchClass first;

    public void add(@NotNull MatchClass cls, ClassSet classSet) {
        if (first == null) first = cls;
        ClassWrapper wrp = classSet.getClassWrapper(cls.getName());
        if (cls.getName().equals("*") || (wrp != null && wrp.isHidden())) {
            return;
        }
        if (!classes.containsKey(cls.getName())) {
            classes.put(cls.getName(), cls);
        }
    }

    public void merge(MatchGroup other) {
        classes.putAll(other.classes);
    }

    public Collection<MatchClass> getClasses() {
        return classes.values();
    }

    public MatchClass getClass(MatchClass owner) {
        return classes.values().stream()
            .filter(owner::equals)
            .findAny().orElse(owner);
    }

    public MatchClass getFirst() {
        return first;
    }
}
