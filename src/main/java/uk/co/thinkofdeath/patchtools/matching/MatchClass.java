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

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MatchClass {

    private final String cls;
    private MatchClass superClass;
    private List<MatchClass> interfaces = new ArrayList<>();
    private List<MatchMethod> methods = new ArrayList<>();
    private List<MatchField> fields = new ArrayList<>();

    private List<ClassNode> matchedClasses = new ArrayList<>();
    private Set<ClassNode> checkedClasses = new HashSet<>();

    public MatchClass(@NotNull String cls) {
        this.cls = cls;
    }

    public String getName() {
        return cls;
    }

    public void setSuperClass(@NotNull MatchClass matchClass) {
        if (superClass != null) {
            throw new IllegalArgumentException("Multiple super classes");
        }
        superClass = matchClass;
    }

    public void addInterface(@NotNull MatchClass matchClass) {
        interfaces.add(matchClass);
    }

    public MatchMethod addMethod(@NotNull MatchMethod method) {
        if (!methods.contains(method)) {
            methods.add(method);
            return method;
        } else {
            return methods.stream().filter(method::equals).findAny().orElse(method);
        }
    }

    public MatchField addField(@NotNull MatchField field) {
        if (!fields.contains(field)) {
            fields.add(field);
            return field;
        } else {
            return fields.stream().filter(field::equals).findAny().orElse(field);
        }
    }

    public void addMatch(@NotNull ClassNode classNode) {
        if (!checkedClasses.contains(classNode) && !matchedClasses.contains(classNode)) {
            matchedClasses.add(classNode);
        }
    }

    public void removeMatch(@NotNull ClassNode classNode) {
        matchedClasses.remove(classNode);
    }

    public void addChecked(@NotNull ClassNode classNode) {
        checkedClasses.add(classNode);
    }

    public boolean hasUnchecked() {
        return matchedClasses.stream().anyMatch(c -> !checkedClasses.contains(c));
    }

    public ClassNode[] getUncheckedClasses() {
        return matchedClasses.stream()
            .filter(c -> !checkedClasses.contains(c))
            .toArray(ClassNode[]::new);
    }

    public List<ClassNode> getMatches() {
        return matchedClasses;
    }

    public MatchClass getSuperClass() {
        return superClass;
    }

    public List<MatchClass> getInterfaces() {
        return interfaces;
    }

    public List<MatchMethod> getMethods() {
        return methods;
    }

    public List<MatchField> getFields() {
        return fields;
    }

    public boolean hasChecked(int length) {
        return checkedClasses.size() == length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MatchClass that = (MatchClass) o;
        return cls.equals(that.cls);
    }

    @Override
    public int hashCode() {
        return cls.hashCode();
    }
}
