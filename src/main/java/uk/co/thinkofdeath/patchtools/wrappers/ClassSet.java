package uk.co.thinkofdeath.patchtools.wrappers;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ClassSet implements Iterable<String> {

    private final Map<String, ClassWrapper> classes = new HashMap<>();
    private final ClassPathWrapper classPath;

    private boolean simplified;

    public ClassSet(ClassPathWrapper wrapper) {
        classPath = wrapper;
    }

    public void simplify() {
        if (simplified) return;
        simplified = true;
        // Safety copy
        new ArrayList<>(classes.values()).stream()
                .filter(v -> !v.isHidden())
                .forEach(v -> v.getMethods().stream()
                        .filter(m -> !m.isHidden())
                        .forEach(m -> {
                            MethodNode node = v.getMethodNode(m);
                            if (((node.access & Opcodes.ACC_PUBLIC) != 0
                                    || (node.access & Opcodes.ACC_PROTECTED) != 0)
                                    && (node.access & Opcodes.ACC_STATIC) == 0) {
                                for (String inter : v.getNode().interfaces) {
                                    replaceMethod(m, inter);
                                }
                                replaceMethod(m, v.getNode().superName);
                            }
                        }));
    }

    private void replaceMethod(MethodWrapper methodWrapper, String clazz) {
        if (clazz == null) return;
        ClassWrapper cl = classes.get(clazz);
        if (cl == null) {
            cl = classPath.find(this, clazz);
            if (cl == null) throw new RuntimeException(clazz);
            classes.put(cl.getNode().name, cl);
        }
        final ClassWrapper finalCl = cl;
        MethodWrapper target = cl.getMethods().stream()
                .filter(m -> m.getName().equals(methodWrapper.getName())
                        && m.getDesc().equals(methodWrapper.getDesc()))
                .filter(m -> {
                    MethodNode node = finalCl.getMethodNode(m);
                    return (((node.access & Opcodes.ACC_PUBLIC) != 0
                            || (node.access & Opcodes.ACC_PROTECTED) != 0)
                            && (node.access & Opcodes.ACC_STATIC) == 0);
                })
                .findFirst().orElse(null);
        if (target != null) {
            if (target.isHidden()) {
                methodWrapper.hidden = true;
            }
            cl.getMethods().remove(target);
            cl.getMethods().add(methodWrapper);
            methodWrapper.add(cl);
        }
        for (String inter : cl.getNode().interfaces) {
            replaceMethod(methodWrapper, inter);
        }
        replaceMethod(methodWrapper, cl.getNode().superName);

    }

    public void add(InputStream clazz) {
        add(clazz, true);
    }

    public void add(InputStream clazz, boolean close) {
        try {
            add(ByteStreams.toByteArray(clazz));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (close) {
                Closeables.closeQuietly(clazz);
            }
        }
    }

    public void add(byte[] clazz) {
        ClassReader classReader = new ClassReader(clazz);
        ClassNode node = new ClassNode(Opcodes.ASM5);
        classReader.accept(node, 0);
        classes.put(classReader.getClassName(), new ClassWrapper(this, node));
    }

    public void remove(String name) {
        classes.remove(name);
    }

    public byte[] getClass(String name) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassWrapper wrapper = classes.get(name);
        if (wrapper == null || wrapper.isHidden()) {
            return null;
        }
        wrapper.getNode().accept(classWriter);
        return classWriter.toByteArray();
    }

    public ClassWrapper getClassWrapper(String name) {
        return classes.get(name);
    }

    public String[] classes() {
        return classes.keySet().toArray(new String[classes.keySet().size()]);
    }

    private String[] hiddenStrippedCache;

    public String[] classes(boolean stripHidden) {
        if (!stripHidden) {
            return classes();
        }
        if (hiddenStrippedCache == null) {
            hiddenStrippedCache = classes.entrySet().stream()
                    .filter(v -> !v.getValue().isHidden())
                    .map(Map.Entry::getKey)
                    .toArray(String[]::new);
        }
        return hiddenStrippedCache;
    }

    @Override
    public Iterator<String> iterator() {
        return classes.keySet().iterator();
    }
}
