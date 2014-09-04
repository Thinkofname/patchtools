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

package uk.co.thinkofdeath.patchtools;

import com.google.common.collect.Maps;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.FieldWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

import java.util.Map;
import java.util.stream.Stream;

public class PatchScope {

    private Map<String, ClassWrapper> classMappings = Maps.newHashMap();
    private Map<MethodWrapper, String> methodMappings = Maps.newHashMap();
    private Map<FieldWrapper, String> fieldMappings = Maps.newHashMap();
    private Map<MethodNode, Map<PatchInstruction, Integer>> methodInstructionMap = Maps.newHashMap();
    private Map<MethodNode, Map<String, LabelNode>> methodLabelMap = Maps.newHashMap();
    private final PatchScope parent;

    public PatchScope() {
        parent = null;
    }

    public PatchScope(PatchScope parent) {
        this.parent = parent;
    }

    public PatchScope duplicate() {
        PatchScope patchScope = new PatchScope();
        patchScope.classMappings = Maps.newHashMap(classMappings);
        patchScope.methodMappings = Maps.newHashMap(methodMappings);
        patchScope.fieldMappings = Maps.newHashMap(fieldMappings);
        return patchScope;
    }

    @Override
    public String toString() {
        return "PatchScope{" +
            "classMappings=" + classMappings +
            ", methodMappings=" + methodMappings +
            ", fieldMappings=" + fieldMappings +
            ", methodInstructionMap=" + methodInstructionMap +
            ", methodLabelMap=" + methodLabelMap +
            '}';
    }

    public boolean hasClass(ClassWrapper classWrapper) {
        return classMappings.containsValue(classWrapper)
            || (parent != null && parent.hasClass(classWrapper));
    }

    public boolean putClass(ClassWrapper classWrapper, String name) {
        return classMappings.put(name, classWrapper) != null;
    }

    public ClassWrapper getClass(String name) {
        ClassWrapper cls = classMappings.get(name);
        if (cls == null && parent != null) {
            cls = parent.getClass(name);
        }
        return cls;
    }

    public String getClass(ClassWrapper cls) {
        return classMappings.entrySet().stream()
            .filter(e -> e.getValue() == cls)
            .map(Map.Entry::getKey)
            .findFirst().orElse(null);
    }

    public boolean hasMethod(MethodWrapper methodWrapper) {
        return methodMappings.containsKey(methodWrapper)
            || (parent != null && parent.hasMethod(methodWrapper));
    }

    public boolean putMethod(MethodWrapper methodWrapper, String name, String desc) {
        if (methodWrapper == null) throw new IllegalArgumentException();
        return methodMappings.put(methodWrapper, name + desc) != null;
    }

    public String getMethod(MethodWrapper methodWrapper) {
        return methodMappings.get(methodWrapper);
    }

    public MethodWrapper getMethod(ClassWrapper owner, String name, String desc) {
        String joined = name + desc;
        return getMethodStream()
            .filter(m -> m.has(owner))
            .filter(m -> getMethodDesc(m).equals(joined))
            .findFirst().orElse(null);
    }

    private Stream<MethodWrapper> getMethodStream() {
        if (parent == null) {
            return methodMappings.keySet().stream();
        }
        return Stream.concat(methodMappings.keySet().stream(), parent.getMethodStream());
    }

    private String getMethodDesc(MethodWrapper methodWrapper) {
        String key = methodMappings.get(methodWrapper);
        if (key == null) {
            return parent.getMethodDesc(methodWrapper);
        }
        return key;
    }

    public boolean hasField(FieldWrapper field) {
        return fieldMappings.containsKey(field)
            || (parent != null && parent.hasField(field));
    }

    public boolean putField(FieldWrapper fieldWrapper, String name, String descriptor) {
        return fieldMappings.put(fieldWrapper, name + "::" + descriptor) != null;
    }

    public String getField(FieldWrapper fieldWrapper) {
        return fieldMappings.get(fieldWrapper);
    }

    public FieldWrapper getField(ClassWrapper owner, String name, String desc) {
        String joined = name + "::" + desc;
        return getFieldStream()
            .filter(f -> f.getOwner() == owner)
            .filter(f -> getFieldDesc(f).equals(joined))
            .findFirst().orElse(null);
    }

    private Stream<FieldWrapper> getFieldStream() {
        if (parent == null) {
            return fieldMappings.keySet().stream();
        }
        return Stream.concat(fieldMappings.keySet().stream(), parent.getFieldStream());
    }

    private String getFieldDesc(FieldWrapper fieldWrapper) {
        String key = fieldMappings.get(fieldWrapper);
        if (key == null) {
            return parent.getFieldDesc(fieldWrapper);
        }
        return key;
    }

    public Map<PatchInstruction, Integer> getInstructMap(MethodNode node) {
        return methodInstructionMap.get(node);
    }

    public void putInstructMap(MethodNode node, Map<PatchInstruction, Integer> instMap) {
        methodInstructionMap.put(node, instMap);
    }

    public LabelNode getLabel(MethodNode node, String name) {
        if (methodLabelMap.containsKey(node)) {
            return methodLabelMap.get(node).get(name);
        }
        return null;
    }

    public void putLabel(MethodNode node, LabelNode label, String name) {
        if (!methodLabelMap.containsKey(node)) {
            methodLabelMap.put(node, Maps.newHashMap());
        }
        methodLabelMap.get(node).put(name, label);
    }

    public void merge(PatchScope scope) {
        classMappings.putAll(scope.classMappings);
        methodMappings.putAll(scope.methodMappings);
        fieldMappings.putAll(scope.fieldMappings);
        methodInstructionMap.putAll(scope.methodInstructionMap);
        methodLabelMap.putAll(scope.methodLabelMap);
    }

    public void clearLabels(MethodNode methodNode) {
        methodLabelMap.remove(methodNode);
    }

    public void clearInstructions(MethodNode methodNode) {
        methodInstructionMap.remove(methodNode);
    }
}
