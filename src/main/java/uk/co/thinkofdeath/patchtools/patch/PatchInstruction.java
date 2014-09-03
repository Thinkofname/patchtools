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

import uk.co.thinkofdeath.patchtools.instruction.Instruction;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PatchInstruction {

    public Mode mode;
    public Instruction instruction;
    public String[] params;
    public List<String> meta = new ArrayList<>();

    public PatchInstruction(Command command, BufferedReader reader) throws IOException {
        mode = command.mode;
        instruction = Instruction.valueOf(command.name.toUpperCase().replace('-', '_'));
        params = command.args;
        if (instruction.isMetaRequired()) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("//") || line.length() == 0) continue;

                if (line.equalsIgnoreCase(".-+".charAt(mode.ordinal()) + "end-" + command.name.toLowerCase())) {
                    break;
                }

                meta.add(line);
            }
        }
    }

    @Override
    public String toString() {
        return "PatchInstruction{" +
            "mode=" + mode +
            ", instruction=" + instruction +
            ", params=" + Arrays.toString(params) +
            ", meta=" + meta +
            '}';
    }
}
