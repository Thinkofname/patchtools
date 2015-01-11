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

package uk.co.thinkofdeath.patchtools.issues;

import org.junit.Test;
import uk.co.thinkofdeath.patchtools.ClassSetLoader;
import uk.co.thinkofdeath.patchtools.Patcher;
import uk.co.thinkofdeath.patchtools.Util;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class IssuePackagePrivate {

    @Test
    public void packagePrivate() throws Exception {
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        classSet.add(
                Util.getClass("uk/co/thinkofdeath/patchtools/issues/IssuePackagePrivate$TestClass")
        );

        Patcher patcher = new Patcher(classSet);

        patcher.apply(
                getClass().getResourceAsStream("/issues/packagePrivate.jpatch")
        );

        ClassSetLoader loader = new ClassSetLoader(classSet);
        Class<?> res = loader.loadClass("uk.co.thinkofdeath.patchtools.issues.IssuePackagePrivate$TestClass");

        Method method = res.getDeclaredMethod("hello");
        method.setAccessible(true);
        assertEquals("Hello jim", method.invoke(
                res.newInstance()
        ));
    }

    public static class TestClass {

        String hello() {
            return "Hello bob";
        }
    }
}
