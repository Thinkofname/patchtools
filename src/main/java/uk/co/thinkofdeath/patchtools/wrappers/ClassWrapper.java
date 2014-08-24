package uk.co.thinkofdeath.patchtools.wrappers;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class ClassWrapper {

    private final ClassNode node;
    private final ClassSet classSet;
    private final boolean hidden;
    private final List<MethodWrapper> methods = new ArrayList<>();
    private final List<FieldWrapper> fields = new ArrayList<>();

    public ClassWrapper(ClassSet classSet, ClassNode node) {
        this(classSet, node, false);
    }

    public ClassWrapper(ClassSet classSet, ClassNode node, boolean hidden) {
        this.classSet = classSet;
        this.node = node;
        this.hidden = hidden;

        node.methods.forEach(v -> methods.add(new MethodWrapper(this, v)));
        node.fields.forEach(v -> fields.add(new FieldWrapper(this, v)));
        if (hidden) {
            methods.forEach(v -> v.hidden = true);
            fields.forEach(v -> v.hidden = true);
        }
    }

    public ClassNode getNode() {
        return node;
    }

    public boolean isHidden() {
        return hidden;
    }

    public ClassSet getClassSet() {
        return classSet;
    }

    public List<MethodWrapper> getMethods() {
        return methods;
    }

    public MethodWrapper[] getMethods(boolean stripHidden) {
        if (stripHidden) {
            return getMethods().stream()
                    .filter(m -> !m.isHidden())
                    .toArray(MethodWrapper[]::new);
        }
        return getMethods().toArray(new MethodWrapper[getMethods().size()]);
    }

    public MethodNode getMethodNode(MethodWrapper wrapper) {
        return node.methods.stream()
                .filter(m -> m.name.equals(wrapper.getName())
                        && m.desc.equals(wrapper.getDesc()))
                .findFirst().orElse(null);
    }

    public MethodWrapper getMethod(String name, String desc) {
        return methods.stream()
                .filter(m -> m.getName().equals(name)
                        && m.getDesc().equals(desc))
                .findFirst().orElse(null);
    }

    public List<FieldWrapper> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        return "ClassWrapper{" + node.name + "}";
    }
}
