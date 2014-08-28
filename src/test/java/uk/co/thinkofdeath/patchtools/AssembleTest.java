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

import org.junit.Assert;
import org.junit.Test;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class AssembleTest {

    @Test
    public void assemble() throws Exception {
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        Patcher patcher = new Patcher(classSet);
        patcher.apply(getClass().getResourceAsStream("/writing.jpatch"));

        ClassSetLoader loader = new ClassSetLoader(classSet);
        Class<?> res = loader.loadClass("think.TestClass");

        int val = (int) res.getMethod("doMagic").invoke(null);
        Assert.assertEquals(5, val);
    }
}
