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

package uk.co.thinkofdeath.patchtools.instruction.instructions;

public class Utils {
    public static Object parseConstant(String cst) {
        if (cst.startsWith("\"") && cst.endsWith("\"")) {
            return cst.substring(1, cst.length() - 1);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static void printConstant(StringBuilder patch, Object value) {
        if (value instanceof String) {
            patch.append('"').append(value).append('"');
        } else {
            //throw new UnsupportedOperationException("Unsupported " + value.getClass());
            patch.append("unsupported: ").append(value);
        }
    }
}
