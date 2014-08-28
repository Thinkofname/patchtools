package uk.co.thinkofdeath.patchtools;

import com.google.common.collect.Maps;
import org.objectweb.asm.Label;
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
    private Map<MethodNode, Map<String, Label>> methodLabelMap = Maps.newHashMap();
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

    public void putClass(ClassWrapper classWrapper, String name) {
        classMappings.put(name, classWrapper);
    }

    public ClassWrapper getClass(String name) {
        ClassWrapper cls = classMappings.get(name);
        if (cls == null && parent != null) {
            cls = parent.getClass(name);
        }
        return cls;
    }

    public boolean hasMethod(MethodWrapper methodWrapper) {
        return methodMappings.containsKey(methodWrapper)
                || (parent != null && parent.hasMethod(methodWrapper));
    }

    public void putMethod(MethodWrapper methodWrapper, String name, String desc) {
        methodMappings.put(methodWrapper, name + desc);
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

    public void putField(FieldWrapper fieldWrapper, String name, String descriptor) {
        fieldMappings.put(fieldWrapper, name + "::" + descriptor);
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

    public Label getLabel(MethodNode node, String name) {
        if (methodLabelMap.containsKey(node)) {
            return methodLabelMap.get(node).get(name);
        }
        return null;
    }

    public void putLabel(MethodNode node, Label label, String name) {
        if (!methodLabelMap.containsKey(node)) {
            methodLabelMap.put(node, Maps.newHashMap());
        }
        methodLabelMap.get(node).put(name, label);
    }
}