package uk.co.thinkofdeath.patchtools;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ClassSet implements Iterable<String> {

    private final Map<String, ClassWrapper> classes = new HashMap<>();
    private final ClassPathWrapper classPath;

    public ClassSet(ClassPathWrapper wrapper) {
        classPath = wrapper;
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
        if (wrapper.isHidden()) {
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

    @Override
    public Iterator<String> iterator() {
        return classes.keySet().iterator();
    }
}
