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

package uk.co.thinkofdeath.patchtools.wrappers;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class ClassWrapper {

    private final ClassNode node;
    private final ClassSet classSet;
    private final boolean hidden;
    private final List<MethodWrapper> methods = new ArrayList<>();
    private final List<FieldWrapper> fields = new ArrayList<>();

    public ClassWrapper(ClassSet classSet, ClassNode node) {
        this(classSet, node, false);
    }

    public ClassWrapper(ClassSet classSet, ClassNode node, boolean hidden) {
        this.classSet = classSet;
        this.node = node;
        this.hidden = hidden;

        node.methods.forEach(v -> methods.add(new MethodWrapper(this, v)));
        node.fields.forEach(v -> fields.add(new FieldWrapper(this, v)));
        if (hidden) {
            methods.forEach(v -> v.hidden = true);
            fields.forEach(v -> v.hidden = true);
        }
    }

    public ClassNode getNode() {
        return node;
    }

    public boolean isHidden() {
        return hidden;
    }

    public ClassSet getClassSet() {
        return classSet;
    }

    public List<MethodWrapper> getMethods() {
        return methods;
    }

    // Shouldn't ever been updated so we cache
    private MethodWrapper[] methodCache;

    public MethodWrapper[] getMethods(boolean stripHidden) {
        if (stripHidden) {
            if (methodCache == null) {
                methodCache = getMethods().stream()
                        .filter(m -> !m.isHidden())
                        .toArray(MethodWrapper[]::new);
            }
            return methodCache;
        }
        return getMethods().toArray(new MethodWrapper[getMethods().size()]);
    }

    public MethodNode getMethodNode(MethodWrapper wrapper) {
        MethodNode mn = node.methods.stream()
                .filter(m -> m.name.equals(wrapper.getName())
                        && m.desc.equals(wrapper.getDesc()))
                .findFirst().orElse(null);
        if (mn == null && node.superName != null) {
            mn = classSet.getClassWrapper(node.superName).getMethodNode(wrapper);
        }
        return mn;
    }

    public MethodWrapper getMethod(String name, String desc) {
        MethodWrapper wrap = methods.stream()
                .filter(m -> m.getName().equals(name)
                        && m.getDesc().equals(desc))
                .findFirst().orElse(null);
        if (wrap == null && node.superName != null) {
            wrap = classSet.getClassWrapper(node.superName).getMethod(name, desc);
        }
        return wrap;
    }

    public List<FieldWrapper> getFields() {
        return fields;
    }

    // Shouldn't ever been updated so we cache
    private FieldWrapper[] fieldCache;

    public FieldWrapper[] getFields(boolean stripHidden) {
        if (stripHidden) {
            if (fieldCache == null) {
                fieldCache = getFields().stream()
                        .filter(f -> !f.isHidden())
                        .toArray(FieldWrapper[]::new);
            }
            return fieldCache;
        }
        return getFields().toArray(new FieldWrapper[getFields().size()]);
    }

    public FieldWrapper getField(String name, String desc) {
        FieldWrapper wrap = fields.stream()
                .filter(f -> f.getName().equals(name)
                        && f.getDesc().equals(desc))
                .findFirst().orElse(null);
        if (wrap == null && node.superName != null) {
            wrap = classSet.getClassWrapper(node.superName).getField(name, desc);
        }
        return wrap;
    }

    public FieldNode getFieldNode(FieldWrapper fieldWrapper) {
        FieldNode fn = node.fields.stream()
                .filter(f -> f.name.equals(fieldWrapper.getName())
                        && f.desc.equals(fieldWrapper.getDesc()))
                .findFirst().orElse(null);
        if (fn == null && node.superName != null) {
            fn = classSet.getClassWrapper(node.superName).getFieldNode(fieldWrapper);
        }
        return fn;
    }

    @Override
    public String toString() {
        return "ClassWrapper{" + node.name + "}";
    }
}
