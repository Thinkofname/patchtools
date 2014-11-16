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

import org.junit.Test;
import uk.co.thinkofdeath.patchtools.disassemble.Disassembler;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import static kotlin.io.IoPackage.readBytes;

public class LoopTest {

    @Test
    public void loop() {
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        classSet.add(
                getClass("uk/co/thinkofdeath/patchtools/testcode/LoopTestClass")
        );

        Disassembler disassembler = new Disassembler(classSet);

        String patch = disassembler.disassemble("uk/co/thinkofdeath/patchtools/testcode/LoopTestClass");
        Patcher patcher = new Patcher(classSet);
        patcher.apply(new StringReader(patch));
    }

    public static byte[] getClass(String name) {
        try (InputStream inputStream = PatchTest.class.getResourceAsStream("/" + name + ".class")) {
            return readBytes(inputStream, 64 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
