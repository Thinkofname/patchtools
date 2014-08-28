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

import com.google.common.collect.Maps;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.util.Map;

public class ClassSetLoader extends ClassLoader {

    private final ClassSet classSet;
    private final Map<String, Class<?>> classes = Maps.newHashMap();

    public ClassSetLoader(ClassSet classSet) {
        this.classSet = classSet;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        String normalName = name.replace('.', '/');
        if (!classes.containsKey(normalName)) {
            byte[] clz = classSet.getClass(normalName);
            if (clz == null) {
                return super.loadClass(name);
            }
            Class<?> c = defineClass(name, clz, 0, clz.length);
            classes.put(normalName, c);
        }
        return classes.get(normalName);
    }
}
