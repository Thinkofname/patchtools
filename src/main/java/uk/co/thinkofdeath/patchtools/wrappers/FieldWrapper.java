package uk.co.thinkofdeath.patchtools.wrappers;

import org.objectweb.asm.tree.FieldNode;
import uk.co.thinkofdeath.patchtools.ClassSet;

public class FieldWrapper {

    private final ClassSet classSet;
    private final String name;
    private final String desc;

    public FieldWrapper(ClassWrapper classWrapper, FieldNode node) {
        this.classSet = classWrapper.getClassSet();
        name = node.name;
        desc = node.desc;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    @Override
    public String toString() {
        return "FieldWrapper{" + name + "}";
    }
}
