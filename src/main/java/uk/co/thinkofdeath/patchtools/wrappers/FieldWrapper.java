package uk.co.thinkofdeath.patchtools.wrappers;

import org.objectweb.asm.tree.FieldNode;

public class FieldWrapper {

    private final ClassSet classSet;
    private final String name;
    private final String desc;
    private final Object value;
    public boolean hidden;

    public FieldWrapper(ClassWrapper classWrapper, FieldNode node) {
        this.classSet = classWrapper.getClassSet();
        name = node.name;
        desc = node.desc;
        value = node.value;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isHidden() {
        return hidden;
    }

    @Override
    public String toString() {
        return "FieldWrapper{" + name + "}";
    }
}
