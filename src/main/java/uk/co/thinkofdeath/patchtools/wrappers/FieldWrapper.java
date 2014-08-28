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

package uk.co.thinkofdeath.patchtools.wrappers;

import org.objectweb.asm.tree.FieldNode;

public class FieldWrapper {

    private final ClassSet classSet;
    private final String name;
    private final String desc;
    private final Object value;
    private ClassWrapper owner;
    public boolean hidden;

    public FieldWrapper(ClassWrapper classWrapper, FieldNode node) {
        this.classSet = classWrapper.getClassSet();
        owner = classWrapper;
        name = node.name;
        desc = node.desc;
        value = node.value;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isHidden() {
        return hidden;
    }

    @Override
    public String toString() {
        return "FieldWrapper{" + name + "}";
    }

    public ClassWrapper getOwner() {
        return owner;
    }
}
