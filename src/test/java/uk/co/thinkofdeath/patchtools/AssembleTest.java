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
import org.junit.Ignore;
import org.junit.Test;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

@Ignore // FIXME
public class AssembleTest {

    private static Class<?> testClass;

    static {
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        Patcher patcher = new Patcher(classSet);
        patcher.apply(AssembleTest.class.getResourceAsStream("/writing.jpatch"));

        ClassSetLoader loader = new ClassSetLoader(classSet);
        try {
            testClass = loader.loadClass("think.TestClass");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void basic() throws Exception {
        int val = (int) testClass.getMethod("doMagic").invoke(null);
        Assert.assertEquals(5, val);
    }

    @Test
    public void array() throws Exception {
        int[] a1 = (int[]) testClass.getMethod("arrayInt").invoke(null);
        Assert.assertEquals(10, a1.length);

        Object[] a2 = (Object[]) testClass.getMethod("arrayObject").invoke(null);
        Assert.assertEquals(20, a2.length);
    }

    @Test
    public void cast() throws Exception {
        testClass.getMethod("castTest", Object.class).invoke(null, new Object[]{new String[]{"test", "test2"}});
    }

    @Test
    public void branch() throws Exception {
        Assert.assertEquals("not-null", testClass.getMethod("branch", Object.class).invoke(null, ""));
        Assert.assertEquals("null", testClass.getMethod("branch", Object.class).invoke(null, new Object[]{null}));
    }

    @Test
    public void switchTest() throws Exception {
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(5 - i, testClass.getMethod("switch", int.class).invoke(null, i));
        }
        Assert.assertEquals(-1, testClass.getMethod("switch", int.class).invoke(null, 20));
    }

    @Test
    public void switchLookupTest() throws Exception {
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(5 - i, testClass.getMethod("switchL", int.class).invoke(null, (int) Math.pow(10, i)));
        }
        Assert.assertEquals(-1, testClass.getMethod("switchL", int.class).invoke(null, 20));
    }

    @Test
    public void exception() throws Exception {
        Assert.assertEquals(5, testClass.getMethod("exception").invoke(null));
    }
}
