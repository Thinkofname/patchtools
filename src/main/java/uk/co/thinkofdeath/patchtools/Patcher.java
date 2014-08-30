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

package uk.co.thinkofdeath.patchtools;

import uk.co.thinkofdeath.patchtools.matching.MatchGenerator;
import uk.co.thinkofdeath.patchtools.patch.PatchClass;
import uk.co.thinkofdeath.patchtools.patch.PatchClasses;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Patcher {

    private final ClassSet classSet;

    public Patcher(ClassSet classSet) {
        this.classSet = classSet;
        classSet.simplify();
    }

    public PatchScope apply(InputStream inputStream) {
        return apply(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    public PatchScope apply(Reader reader) {
        return apply(new BufferedReader(reader));
    }

    public PatchScope apply(BufferedReader reader) {
        return apply(reader, new PatchScope());
    }

    public PatchScope apply(BufferedReader reader, PatchScope patchScope) {
        PatchClasses patchClasses;
        try (BufferedReader ignored = reader) {
            patchClasses = new PatchClasses(reader);
            return apply(patchClasses, patchScope);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public PatchScope apply(PatchClasses patchClasses, PatchScope patchScope) {
        MatchGenerator generator = new MatchGenerator(classSet, patchClasses, patchScope);
        PatchScope foundScope = generator.apply((group, scope) -> {
                PatchClass[] classes = group.getClasses().stream()
                    .map(c -> patchClasses.getClass(c.getName()))
                    .toArray(PatchClass[]::new);
                return Arrays.stream(classes).allMatch(c -> c.checkAttributes(scope, classSet))
                    && Arrays.stream(classes).allMatch(c -> c.checkFields(scope, classSet))
                    && Arrays.stream(classes).allMatch(c -> c.checkMethods(scope, classSet))
                    && Arrays.stream(classes).allMatch(c -> c.checkMethodsInstructions(scope, classSet));
            }
        );
        if (foundScope == null) {
            return null;
        }
        patchClasses.getClasses().forEach(c -> c.apply(foundScope, classSet));
        return foundScope;
    }

    public ClassSet getClasses() {
        return classSet;
    }
}
