package uk.co.thinkofdeath.patchtools.wrappers;

import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;

public class MethodWrapper {

    private final ClassSet classSet;
    private final Set<ClassWrapper> classWrappers = new HashSet<>();
    private final String name;
    private final String desc;
    boolean hidden;

    public MethodWrapper(ClassWrapper classWrapper, MethodNode node) {
        this.classSet = classWrapper.getClassSet();
        classWrappers.add(classWrapper);
        name = node.name;
        desc = node.desc;
    }

    public boolean isHidden() {
        return hidden;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public void add(ClassWrapper classWrapper) {
        classWrappers.add(classWrapper);
    }

    public boolean has(ClassWrapper classWrapper) {
        return classWrappers.contains(classWrapper);
    }

    @Override
    public String toString() {
        return "MethodWrapper{" + name + desc + "}";
    }
}
