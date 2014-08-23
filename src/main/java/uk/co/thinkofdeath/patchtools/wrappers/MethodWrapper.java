package uk.co.thinkofdeath.patchtools.wrappers;

import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.ClassSet;

import java.util.HashSet;
import java.util.Set;

public class MethodWrapper {

    private final ClassSet classSet;
    private final Set<ClassWrapper> classWrappers = new HashSet<>();
    private final String name;
    private final String desc;

    public MethodWrapper(ClassWrapper classWrapper, MethodNode node) {
        this.classSet = classWrapper.getClassSet();
        classWrappers.add(classWrapper);
        name = node.name;
        desc = node.desc;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public boolean has(ClassWrapper classWrapper) {
        return classWrappers.contains(classWrapper);
    }

    @Override
    public String toString() {
        return "MethodWrapper{" + name + desc + "}";
    }
}
