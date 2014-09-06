/*
 * Copyright 2014 Matthew Collins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.thinkofdeath.patchtools.wrappers;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;

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
        ClassWrapper cl = getClassWrapper(clazz);
        if (cl == null) {
            return;
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
        add(node);
    }

    public void add(ClassNode node) {
        classes.put(node.name, new ClassWrapper(this, node));
    }

    public void remove(String name) {
        classes.remove(name);
    }

    public byte[] getClass(String name) {
        ClassSetWriter classWriter = new ClassSetWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassWrapper wrapper = classes.get(name);
        if (wrapper == null || wrapper.isHidden()) {
            return null;
        }
        wrapper.getNode().version = Opcodes.V1_8;
        wrapper.getNode().accept(classWriter);
        return classWriter.toByteArray();
    }

    public byte[] getClass(String name, PatchScope scope) {
        ClassReader classReader = new ClassReader(getClass(name));
        ClassSetWriter classWriter = new ClassSetWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassWrapper wrapper = classes.get(name);
        if (wrapper == null || wrapper.isHidden()) {
            return null;
        }
        wrapper.getNode().version = Opcodes.V1_8;
        classReader.accept(new RemappingClassAdapter(classWriter, new ClassRemapper(scope)), ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

    public ClassWrapper getClassWrapper(String name) {
        ClassWrapper cl = classes.get(name);
        if (cl == null) {
            cl = classPath.find(this, name);
            if (cl == null) return null;
            classes.put(cl.getNode().name, cl);
        }
        return cl;
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

    private class ClassSetWriter extends ClassWriter {

        public ClassSetWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            if (type1.equals(type2)) {
                return type1;
            }

            HashSet<String> supers = new HashSet<>();
            supers.add(type1);
            supers.add(type2);

            boolean run1 = true;
            boolean run2 = true;
            ClassWrapper t1 = getClassWrapper(type1);
            ClassWrapper t2 = getClassWrapper(type2);

            if (t1 == null || t2 == null) {
                return "java/lang/Object";
            }

            if ((t1.getNode().access & Opcodes.ACC_INTERFACE) != 0
                || (t2.getNode().access & Opcodes.ACC_INTERFACE) != 0) {
                return "java/lang/Object";
            }

            while (run1 || run2) {
                if (run1) {
                    if (t1 == null || t1.getNode().superName == null) {
                        run1 = false;
                    } else {
                        t1 = getClassWrapper(t1.getNode().superName);
                        if (t1 != null && !supers.add(t1.getNode().name)) {
                            return t1.getNode().name;
                        }
                    }
                }
                if (run2) {
                    if (t2 == null || t2.getNode().superName == null) {
                        run2 = false;
                    } else {
                        t2 = getClassWrapper(t2.getNode().superName);
                        if (t2 != null && !supers.add(t2.getNode().name)) {
                            return t2.getNode().name;
                        }
                    }
                }
            }
            return "java/lang/Object";
        }
    }

    private class ClassRemapper extends Remapper {
        private final PatchScope scope;

        public ClassRemapper(PatchScope scope) {
            this.scope = scope;
        }

        @Override
        public String map(String typeName) {
            ClassWrapper cls = getClassWrapper(typeName);
            if (cls != null) {
                String name = scope.getClass(cls);
                return name == null ? typeName : name;
            }
            return typeName;
        }

        @Override
        public String mapMethodName(String owner, String name, String desc) {
            ClassWrapper cls = getClassWrapper(owner);
            if (cls != null) {
                MethodWrapper methodWrapper = cls.getMethod(name, desc);
                String nName = scope.getMethod(methodWrapper);
                return nName == null ? name : nName.split("\\(")[0];
            }
            return name;
        }

        @Override
        public String mapFieldName(String owner, String name, String desc) {
            ClassWrapper cls = getClassWrapper(owner);
            if (cls != null) {
                FieldWrapper fieldWrapper = cls.getField(name, desc);
                String nName = scope.getField(fieldWrapper);
                return nName == null ? name : nName.split("::")[0];
            }
            return name;
        }
    }
}
