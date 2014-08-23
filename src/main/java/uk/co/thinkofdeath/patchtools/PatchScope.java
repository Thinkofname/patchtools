package uk.co.thinkofdeath.patchtools;

import com.google.common.collect.Maps;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

import java.util.Map;

public class PatchScope {

    private Map<String, ClassWrapper> classMappings = Maps.newHashMap();
    private Map<MethodWrapper, String> methodMappings = Maps.newHashMap();

    public PatchScope duplicate() {
        PatchScope patchScope = new PatchScope();
        patchScope.classMappings = Maps.newHashMap(classMappings);
        patchScope.methodMappings = Maps.newHashMap(methodMappings);
        return patchScope;
    }

    @Override
    public String toString() {
        return "PatchScope{" +
                "classMappings=" + classMappings +
                ", methodMappings=" + methodMappings +
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

    public void putMethod(MethodWrapper methodWrapper, String name) {
        methodMappings.put(methodWrapper, name);
    }

    public MethodWrapper getMethod(ClassWrapper owner, String name) {
        return methodMappings.keySet().stream()
                .filter(m -> m.has(owner))
                .filter(m -> methodMappings.get(m).equals(name))
                .findFirst().get();
    }
}
