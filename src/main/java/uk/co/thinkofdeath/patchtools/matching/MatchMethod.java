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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

public class MatchMethod {

    private final MatchClass owner;
    private final String name;
    private final String desc;
    private final List<Type> arguments = new ArrayList<>();
    private Type returnType;

    private List<MethodPair> matchedMethods = new ArrayList<>();
    private Set<MethodPair> checkedMethods = new HashSet<>();

    public MatchMethod(MatchClass owner, String name, String desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public void addArgument(Type type) {
        arguments.add(type);
    }

    public void setReturn(Type type) {
        returnType = type;
    }

    public List<Type> getArguments() {
        return arguments;
    }

    public Type getReturnType() {
        return returnType;
    }

    public MatchClass getOwner() {
        return owner;
    }

    public String getDesc() {
        return desc;
    }

    public void addMatch(@NotNull ClassNode owner, @NotNull MethodNode methodNode) {
        if (!checkedMethods.contains(new MethodPair(owner, methodNode))) {
            matchedMethods.add(new MethodPair(owner, methodNode));
        }
    }

    public void removeMatch(@NotNull ClassNode owner, @NotNull MethodNode methodNode) {
        matchedMethods.remove(new MethodPair(owner, methodNode));
    }

    public void removeMatch(ClassNode clazz) {
        ListIterator<MethodPair> it = matchedMethods.listIterator();
        while (it.hasNext()) {
            MethodPair pair = it.next();
            if (pair.owner == clazz) {
                it.remove();
            }
        }
    }

    public void addChecked(@NotNull ClassNode owner, @NotNull MethodNode methodNode) {
        checkedMethods.add(new MethodPair(owner, methodNode));
    }

    public boolean hasUnchecked() {
        return matchedMethods.stream().anyMatch(methodPair -> !checkedMethods.contains(methodPair));
    }

    public MethodPair[] getUncheckedMethods() {
        return matchedMethods.stream()
                .filter(c -> !checkedMethods.contains(c))
                .toArray(MethodPair[]::new);
    }

    public List<MethodNode> getMatches() {
        return Arrays.asList(matchedMethods.stream().map(MethodPair::getNode).toArray(MethodNode[]::new));
    }

    public List<MethodNode> getMatches(ClassNode owner) {
        return Arrays.asList(matchedMethods.stream()
                .filter(m -> m.getOwner() == owner)
                .map(MethodPair::getNode).toArray(MethodNode[]::new));
    }

    public boolean usesNode(ClassNode clazz) {
        return matchedMethods.stream()
                .anyMatch(m -> m.owner == clazz);
    }

    public static class MethodPair {
        private ClassNode owner;
        private MethodNode node;

        public MethodPair(ClassNode owner, MethodNode node) {
            this.owner = owner;
            this.node = node;
        }

        public ClassNode getOwner() {
            return owner;
        }

        public MethodNode getNode() {
            return node;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MethodPair that = (MethodPair) o;

            return node.equals(that.node) && owner.equals(that.owner);

        }

        @Override
        public int hashCode() {
            int result = owner.hashCode();
            result = 31 * result + node.hashCode();
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MatchMethod that = (MatchMethod) o;

        return desc.equals(that.desc)
                && name.equals(that.name)
                && owner.equals(that.owner);

    }

    @Override
    public int hashCode() {
        int result = owner.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + desc.hashCode();
        return result;
    }
}
