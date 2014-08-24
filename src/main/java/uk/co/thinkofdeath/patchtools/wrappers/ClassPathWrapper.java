package uk.co.thinkofdeath.patchtools.wrappers;

import com.google.common.io.ByteStreams;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassPathWrapper implements Closeable {

    private final ZipFile[] searchFiles;

    public ClassPathWrapper(File... libs) {
        try {
            searchFiles = new ZipFile[libs.length];
            for (int i = 0; i < libs.length; i++) {
                searchFiles[i] = new ZipFile(libs[i]);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ClassWrapper find(ClassSet classSet, String clazz) {
        try {
            InputStream in = null;
            for (ZipFile zip : searchFiles) {
                ZipEntry entry = zip.getEntry(clazz + ".class");
                if (entry == null) continue;
                in = zip.getInputStream(entry);
                break;
            }
            if (in == null) {
                in = getClass().getResourceAsStream("/" + clazz + ".class");
                if (in == null) {
                    return null;
                }
            }
            try (InputStream ignored = in) {
                byte[] data = ByteStreams.toByteArray(in);
                ClassNode node = new ClassNode(Opcodes.ASM5);
                ClassReader reader = new ClassReader(data);
                reader.accept(node, 0);
                return new ClassWrapper(classSet, node, true);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    @Override
    public void close() throws IOException {
        for (ZipFile zip : searchFiles) {
            zip.close();
        }
    }
}
