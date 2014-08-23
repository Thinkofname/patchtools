package uk.co.thinkofdeath.patchtools.wrappers;

import org.objectweb.asm.tree.FieldNode;
import uk.co.thinkofdeath.patchtools.ClassSet;

public class FieldWrapper {

    private final ClassSet classSet;
    private final FieldNode node;

    public FieldWrapper(ClassWrapper classWrapper, FieldNode node) {
        this.classSet = classWrapper.getClassSet();
        this.node = node;
    }

    public FieldNode getNode() {
        return node;
    }
}
