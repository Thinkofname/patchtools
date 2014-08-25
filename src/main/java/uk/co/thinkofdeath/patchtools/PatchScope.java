package uk.co.thinkofdeath.patchtools;

import com.google.common.collect.Maps;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.FieldWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

import java.util.Map;

public class PatchScope {

    private Map<String, ClassWrapper> classMappings = Maps.newHashMap();
    private Map<MethodWrapper, String> methodMappings = Maps.newHashMap();
    private Map<FieldWrapper, String> fieldMappings = Maps.newHashMap();
    private Map<MethodNode, Map<PatchInstruction, Integer>> methodInstructionMap = Maps.newHashMap();

    public PatchScope duplicate() {
        PatchScope patchScope = new PatchScope();
        patchScope.classMappings = Maps.newHashMap(classMappings);
        patchScope.methodMappings = Maps.newHashMap(methodMappings);
        patchScope.fieldMappings = Maps.newHashMap(fieldMappings);
        patchScope.methodInstructionMap = Maps.newHashMap(methodInstructionMap);
        return patchScope;
    }

    @Override
    public String toString() {
        return "PatchScope{" +
                "classMappings=" + classMappings +
                ", methodMappings=" + methodMappings +
                ", fieldMappings=" + fieldMappings +
                ", methodInstructionMap" + methodInstructionMap +
                '}';
    }

    public boolean hasClass(ClassWrapper classWrapper) {
        return classMappings.containsValue(classWrapper);
    }

    public void putClass(ClassWrapper classWrapper, String name) {
        classMappings.put(name, classWrapper);
    }

    public ClassWrapper getClass(String name) {
        return classMappings.get(name);
    }

    public boolean hasMethod(MethodWrapper methodWrapper) {
        return methodMappings.containsKey(methodWrapper);
    }

    public void putMethod(MethodWrapper methodWrapper, String name, String desc) {
        methodMappings.put(methodWrapper, name + desc);
    }

    public MethodWrapper getMethod(ClassWrapper owner, String name, String desc) {
        return methodMappings.keySet().stream()
                .filter(m -> m.has(owner))
                .filter(m -> methodMappings.get(m).equals(name + desc))
                .findFirst().orElse(null);
    }

    public boolean hasField(FieldWrapper field) {
        return fieldMappings.containsKey(field);
    }

    public void putField(FieldWrapper fieldWrapper, String name, String descriptor) {
        fieldMappings.put(fieldWrapper, name + "::" + descriptor);
    }

    public FieldWrapper getField(ClassWrapper owner, String name, String desc) {
        return fieldMappings.keySet().stream()
                .filter(f -> f.getOwner() == owner)
                .filter(f -> fieldMappings.get(f).equals(name + "::" + desc))
                .findFirst().orElse(null);
    }

    public Map<PatchInstruction, Integer> getInstructMap(MethodNode node) {
        return methodInstructionMap.get(node);
    }

    public void putInstructMap(MethodNode node, Map<PatchInstruction, Integer> instMap) {
        methodInstructionMap.put(node, instMap);
    }
}