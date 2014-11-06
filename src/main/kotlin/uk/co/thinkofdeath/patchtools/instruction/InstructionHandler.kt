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

package uk.co.thinkofdeath.patchtools.instruction

import com.google.common.collect.ImmutableList
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodNode
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.matching.MatchClass
import uk.co.thinkofdeath.patchtools.matching.MatchField
import uk.co.thinkofdeath.patchtools.matching.MatchMethod
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet

public trait InstructionHandler {

    public fun check(classSet: ClassSet, scope: PatchScope?, instruction: PatchInstruction, method: MethodNode, insn: AbstractInsnNode): Boolean

    public fun create(classSet: ClassSet, scope: PatchScope, instruction: PatchInstruction, method: MethodNode): AbstractInsnNode

    public fun print(instruction: Instruction, patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean

    public fun validate(instruction: PatchInstruction)

    public fun getReferencedClasses(instruction: PatchInstruction): List<MatchClass> {
        return ImmutableList.of<MatchClass>()
    }

    public fun getReferencedMethods(instruction: PatchInstruction): List<MatchMethod> {
        return ImmutableList.of<MatchMethod>()
    }

    public fun getReferencedFields(instruction: PatchInstruction): List<MatchField> {
        return ImmutableList.of<MatchField>()
    }
}
