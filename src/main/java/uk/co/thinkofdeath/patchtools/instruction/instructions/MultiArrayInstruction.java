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

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.PatchClass;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class MultiArrayInstruction implements InstructionHandler {

    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, MethodNode method, AbstractInsnNode insn) {
        if (patchInstruction.params.length != 2
            || !(insn instanceof MultiANewArrayInsnNode)) {
            return false;
        }

        MultiANewArrayInsnNode insnNode = (MultiANewArrayInsnNode) insn;

        Type pType = Type.getType(patchInstruction.params[0]);
        int dims = patchInstruction.params[1].equals("*") ? -1 : Integer.parseInt(patchInstruction.params[1]);

        return PatchClass.checkTypes(classSet, scope, pType, Type.getType(insnNode.desc))
            && (dims == -1 || dims == insnNode.dims);
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        if (instruction.params.length != 1) {
            throw new RuntimeException();
        }
        String desc = instruction.params[0];
        int dims = Integer.parseInt(instruction.params[1]);
        return new MultiANewArrayInsnNode(desc, dims);
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (insn instanceof MultiANewArrayInsnNode) {
            patch.append("new-array-multi ")
                .append(((MultiANewArrayInsnNode) insn).desc)
                .append(' ')
                .append(((MultiANewArrayInsnNode) insn).dims);
            return true;
        }
        return false;
    }
}
