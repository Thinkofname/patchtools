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
import org.objectweb.asm.tree.FieldNode;
import uk.co.thinkofdeath.patchtools.logging.StateLogger;
import uk.co.thinkofdeath.patchtools.patch.Ident;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;

import java.util.*;
import java.util.stream.Collectors;

public class MatchField {

    private final MatchClass owner;
    private final String name;
    private final String desc;
    private Type type;

    private List<FieldPair> matchedFields = new ArrayList<>();
    private Set<FieldPair> checkedFields = new HashSet<>();

    public MatchField(MatchClass owner, String name, String desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public MatchClass getOwner() {
        return owner;
    }

    public String getDesc() {
        return desc;
    }

    public void addMatch(@NotNull ClassNode owner, @NotNull FieldNode fieldNode) {
        if (!checkedFields.contains(new FieldPair(owner, fieldNode))) {
            matchedFields.add(new FieldPair(owner, fieldNode));
        }
    }

    public void removeMatch(@NotNull ClassNode owner, @NotNull FieldNode fieldNode) {
        matchedFields.remove(new FieldPair(owner, fieldNode));
    }

    public void removeMatch(ClassNode clazz) {
        ListIterator<FieldPair> it = matchedFields.listIterator();
        while (it.hasNext()) {
            FieldPair pair = it.next();
            if (pair.owner == clazz) {
                it.remove();
            }
        }
    }

    public void addChecked(@NotNull ClassNode owner, @NotNull FieldNode fieldNode) {
        checkedFields.add(new FieldPair(owner, fieldNode));
    }

    public boolean hasUnchecked() {
        return matchedFields.stream().anyMatch(fieldPair -> !checkedFields.contains(fieldPair));
    }

    public FieldPair[] getUncheckedMethods() {
        return matchedFields.stream()
            .filter(c -> !checkedFields.contains(c))
            .toArray(FieldPair[]::new);
    }

    public List<FieldNode> getMatches() {
        return matchedFields.stream()
            .map(FieldPair::getNode)
            .collect(Collectors.toList());
    }

    public List<FieldNode> getMatches(ClassNode owner) {
        return matchedFields.stream()
            .filter(m -> m.getOwner() == owner)
            .map(FieldPair::getNode)
            .collect(Collectors.toList());
    }

    public boolean usesNode(ClassNode clazz) {
        return matchedFields.stream()
            .anyMatch(m -> m.owner == clazz);
    }

    public void check(StateLogger logger, ClassSet classSet, MatchGroup group, FieldPair pair) {
        FieldNode node = pair.getNode();
        addChecked(pair.getOwner(), pair.getNode());

        logger.println("- " + node.name);
        logger.indent();

        Type type = Type.getType(node.desc);
        if (type.getSort() != getType().getSort()) {
            logger.println(StateLogger.typeMismatch(getType(), type));
            removeMatch(pair.getOwner(), node);
        } else if (type.getSort() == Type.OBJECT) {
            MatchClass retCls = group.getClass(new MatchClass(new Ident(getType().getInternalName()).getName()));
            ClassWrapper wrapper = classSet.getClassWrapper(type.getInternalName());
            if (wrapper != null && !wrapper.isHidden()) {
                logger.println("Adding " + wrapper.getNode().name
                    + " as a possible match for " + getType().getInternalName());
                retCls.addMatch(wrapper.getNode());
            }
        }

        logger.unindent();
    }

    public static class FieldPair {
        private ClassNode owner;
        private FieldNode node;

        public FieldPair(ClassNode owner, FieldNode node) {
            this.owner = owner;
            this.node = node;
        }

        public ClassNode getOwner() {
            return owner;
        }

        public FieldNode getNode() {
            return node;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FieldPair that = (FieldPair) o;

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

        MatchField that = (MatchField) o;

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
