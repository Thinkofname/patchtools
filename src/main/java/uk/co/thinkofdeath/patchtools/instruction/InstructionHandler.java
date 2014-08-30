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

package uk.co.thinkofdeath.patchtools.instruction;

import com.google.common.collect.ImmutableList;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.matching.MatchClass;
import uk.co.thinkofdeath.patchtools.matching.MatchField;
import uk.co.thinkofdeath.patchtools.matching.MatchMethod;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.util.List;

public interface InstructionHandler {

    boolean check(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method, AbstractInsnNode insn);

    AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method);

    boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn);

    default List<MatchClass> getReferencedClasses(PatchInstruction instruction) {
        return ImmutableList.of();
    }

    default List<MatchMethod> getReferencedMethods(PatchInstruction instruction) {
        return ImmutableList.of();
    }

    default List<MatchField> getReferencedFields(PatchInstruction instruction) {
        return ImmutableList.of();
    }
}
