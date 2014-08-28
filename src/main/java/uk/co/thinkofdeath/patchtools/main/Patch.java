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

package uk.co.thinkofdeath.patchtools.main;

import com.google.common.io.ByteStreams;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.Patcher;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Patch {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java <j-args> <jar> <out-jar> <patch> [map]");
            return;
        }
        File inJar = new File(args[0]);
        File outJar = new File(args[1]);
        File inPatch = new File(args[2]);
        boolean map = args.length >= 4 && args[3].equals("true");

        if (!outJar.getParentFile().exists() && !outJar.getParentFile().mkdirs()) {
            throw new RuntimeException("Failed to setup output dir");
        }

        if (outJar.exists()) outJar.delete();

        File dep = new File(inJar.getPath().replaceAll("\\.jar", ".json"));
        File[] deps = new File[0];
        if (dep.exists()) {
            System.out.println("Loading deps");
            deps = MinecraftLibraryDownloader.downloadVersion(dep);
        }
        System.out.println("Loading classes");

        ClassSet classSet = new ClassSet(new ClassPathWrapper(deps));
        HashMap<String, byte[]> resources = new HashMap<>();

        try (ZipFile zipFile = new ZipFile(inJar)) {
            zipFile.stream()
                    .forEach(c -> {
                        try (InputStream in = zipFile.getInputStream(c)) {
                            if (c.getName().endsWith(".class")) {
                                classSet.add(in, false);
                            } else {

                                resources.put(c.getName(), ByteStreams.toByteArray(in));
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
        classSet.simplify();

        System.out.println("Applying patch");

        long start = System.nanoTime();
        Patcher patcher = new Patcher(classSet);
        PatchScope scope = patcher.apply(new FileInputStream(inPatch));
        System.out.println("Time: " + (System.nanoTime() - start));

        try (ZipOutputStream zop = new ZipOutputStream(new FileOutputStream(outJar))) {
            for (String cls : classSet.classes(true)) {
                System.out.println("Saving " + cls);
                if (!map) {
                    ZipEntry zipEntry = new ZipEntry(cls + ".class");
                    zop.putNextEntry(zipEntry);
                    zop.write(classSet.getClass(cls));
                } else {
                    String mcls = scope.getClass(classSet.getClassWrapper(cls));
                    if (mcls == null) mcls = cls;
                    ZipEntry zipEntry = new ZipEntry(mcls + ".class");
                    zop.putNextEntry(zipEntry);
                    zop.write(classSet.getClass(cls, scope));
                }
            }
            for (Map.Entry<String, byte[]> e : resources.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(e.getKey());
                zop.putNextEntry(zipEntry);
                zop.write(e.getValue());
            }
        }
        System.out.println("Done");
    }
}
