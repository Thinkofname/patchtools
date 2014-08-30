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

package uk.co.thinkofdeath.patchtools.patch;

import org.jetbrains.annotations.Contract;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PatchClasses {

    private List<PatchClass> classes = new ArrayList<>();

    public PatchClasses(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("//") || line.length() == 0) continue;

            Command command = Command.from(line);
            switch (command.name) {
                case "interface":
                case "enum":
                case "class":
                    classes.add(new PatchClass(command, reader));
                    break;
                default:
                    throw new IllegalArgumentException(command.toString());
            }
        }
    }

    public List<PatchClass> getClasses() {
        return classes;
    }

    @Contract("null -> null")
    public PatchClass getClass(String name) {
        return classes.stream()
            .filter(c -> c.getIdent().getName().equals(name))
            .findFirst()
            .orElse(null);
    }
}
