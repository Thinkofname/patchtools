package uk.co.thinkofdeath.patchtools.wrappers;

import org.objectweb.asm.tree.ClassNode;
import uk.co.thinkofdeath.patchtools.ClassSet;

import java.util.ArrayList;
import java.util.List;

// TODO:
// - Inheritance
public class ClassWrapper {

    private final ClassNode node;
    private final ClassSet classSet;
    private final boolean hidden;
    private final List<MethodWrapper> methods = new ArrayList<>();
    private final List<FieldWrapper> fields = new ArrayList<>();

    public ClassWrapper(ClassSet classSet, ClassNode node) {
        this.classSet = classSet;
        this.node = node;
        hidden = false;

        node.methods.forEach(v -> methods.add(new MethodWrapper(this, v)));
        node.fields.forEach(v -> fields.add(new FieldWrapper(this, v)));
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
}
