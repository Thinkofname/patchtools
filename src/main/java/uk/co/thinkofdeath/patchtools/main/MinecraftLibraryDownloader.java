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
import com.google.common.io.Resources;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MinecraftLibraryDownloader {

    public static final File DEP_STORE = new File("libraries");

    public static File[] downloadVersion(File info) throws IOException {

        ArrayList<File> libraries = new ArrayList<>();

        // Find and download dependencies
        try (Reader reader = Files.newReader(info, StandardCharsets.UTF_8)) {
            MojangVersion mojangVersion = new Gson().fromJson(reader, MojangVersion.class);
            if (mojangVersion.minimumLauncherVersion > 14) {
                throw new RuntimeException("Unsupported launcher version");
            }
            for (MojangVersion.Library library : mojangVersion.libraries) {
                if (library.natives != null) continue;
                String[] parts = library.name.split(":");
                String pck = parts[0].replace('.', '/');
                String name = parts[1];
                String ver = parts[2];
                String path = pck + "/" + name + "/" + ver + "/" + name + "-" + ver + ".jar";
                File dep = new File(DEP_STORE, path);
                if (!dep.exists()) {
                    System.out.println("Downloading " + name + " " + ver);
                    dep.getParentFile().mkdirs();
                    try (FileOutputStream outputStream = new FileOutputStream(dep)) {
                        Resources.copy(new URL("https://libraries.minecraft.net/" + path), outputStream);
                    }
                }
                libraries.add(dep);
            }
        }

        return libraries.toArray(new File[libraries.size()]);
    }
}
