package uk.co.thinkofdeath.patchtools.wrappers;

import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.ClassSet;

import java.util.HashSet;
import java.util.Set;

public class MethodWrapper {

    private final ClassSet classSet;
    private final Set<ClassWrapper> classWrappers = new HashSet<>();
    private final MethodNode node;

    public MethodWrapper(ClassWrapper classWrapper, MethodNode node) {
        this.classSet = classWrapper.getClassSet();
        this.node = node;
        classWrappers.add(classWrapper);
    }

    public MethodNode getNode() {
        return node;
    }
}
