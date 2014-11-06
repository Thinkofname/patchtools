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

package uk.co.thinkofdeath.patchtools.testcode;

public class LoopTestClass {

    public static void testMethod() {
        LoopTestClass test = new LoopTestClass();
        int[] hello = new int[5];
    }

    public static int testTwo(int val) {
        if (val == 0) {
            val++;
            val *= 5;
            return val;
        } else if (val > 5) {
            return val / 5;
        }
        return val;
    }

    public static int testThree(int val) {
        for (int i = 0; i < val; i++) {
            val *= 2;
        }
        return val;
    }

    public static int switchTest(int val) {
        switch (val) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            default:
                return -1;
        }
    }

    public static int lookupTest(int val) {
        switch (val) {
            case 0:
                return 1;
            case 10:
                return 2;
            case 100:
                return 3;
            case 1000:
                return 4;
            default:
                return -1;
        }
    }

    public static void exception() {
        try {
            throw new RuntimeException();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
