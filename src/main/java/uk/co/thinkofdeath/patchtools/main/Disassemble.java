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

import com.google.common.io.Files;
import uk.co.thinkofdeath.patchtools.disassemble.Disassembler;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;

public class Disassemble {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java <j-args> <jar> <out>");
            return;
        }
        File inJar = new File(args[0]);
        File outDir = new File(args[1]);

        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new RuntimeException("Failed to setup output dir");
        }

        File dep = new File(inJar.getPath().replaceAll("\\.jar", ".json"));
        File[] deps = new File[0];
        if (dep.exists()) {
            System.out.println("Loading deps");
            deps = MinecraftLibraryDownloader.downloadVersion(dep);
        }
        System.out.println("Loading classes");

        ClassSet classSet = new ClassSet(new ClassPathWrapper(deps));

        try (ZipFile zipFile = new ZipFile(inJar)) {
            zipFile.stream()
                .filter(c -> c.getName().endsWith(".class"))
                .forEach(c -> {
                    try {
                        classSet.add(zipFile.getInputStream(c));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        }
        classSet.simplify();

        System.out.println("Disassembling classes");

        Disassembler disassembler = new Disassembler(classSet);
        for (String cls : classSet.classes(true)) {
            File target = new File(outDir, cls + ".jpatch");
            File dir = target.getParentFile();
            if (!dir.exists() && !dir.mkdirs()) {
                throw new RuntimeException("Failed to create " + dir);
            }
            if (target.exists()) target.delete();
            Files.write(disassembler.disassemble(cls), target, StandardCharsets.UTF_8);
        }
    }
}
