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

package uk.co.thinkofdeath.patchtools.matching;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

interface MatchPair {

    void apply();

    public static class ClassMatch implements MatchPair {

        private MatchClass matchClass;
        private ClassNode classNode;

        public ClassMatch(MatchClass matchClass, ClassNode classNode) {
            this.matchClass = matchClass;
            this.classNode = classNode;
        }

        @Override
        public void apply() {
            matchClass.addMatch(classNode);
        }
    }

    public static class MethodMatch implements MatchPair {

        private MatchMethod matchMethod;
        private ClassNode classNode;
        private MethodNode methodNode;

        public MethodMatch(MatchMethod matchMethod, ClassNode classNode, MethodNode methodNode) {
            this.matchMethod = matchMethod;
            this.classNode = classNode;
            this.methodNode = methodNode;
        }

        @Override
        public void apply() {
            matchMethod.addMatch(classNode, methodNode);
        }
    }

    public static class FieldMatch implements MatchPair {

        private MatchField matchField;
        private ClassNode classNode;
        private FieldNode fieldNode;

        public FieldMatch(MatchField matchField, ClassNode classNode, FieldNode fieldNode) {
            this.matchField = matchField;
            this.classNode = classNode;
            this.fieldNode = fieldNode;
        }

        @Override
        public void apply() {
            matchField.addMatch(classNode, fieldNode);
        }
    }
}
