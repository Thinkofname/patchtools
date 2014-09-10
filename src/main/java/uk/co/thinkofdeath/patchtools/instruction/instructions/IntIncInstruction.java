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

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.patch.ValidateException;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class IntIncInstruction implements InstructionHandler {
    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof IincInsnNode)) {
            return false;
        }
        int var = 0;
        boolean any = false;
        if (instruction.params[0].equals("*")) {
            any = true;
        } else {
            var = Integer.parseInt(instruction.params[0]);
        }
        int other = ((IincInsnNode) insn).var;
        if (any || var == other) {
            int val = 0;
            any = false;
            if (instruction.params[1].equals("*")) {
                any = true;
            } else {
                val = Integer.parseInt(instruction.params[1]);
            }
            other = ((IincInsnNode) insn).incr;
            return !(!any && other != val);
        }
        return false;
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        int var = Integer.parseInt(instruction.params[0]);
        int val = Integer.parseInt(instruction.params[1]);
        return new IincInsnNode(var, val);
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (insn instanceof IincInsnNode) {
            patch.append("inc-int ")
                .append(((IincInsnNode) insn).var)
                .append(' ')
                .append(((IincInsnNode) insn).incr);
            return true;
        }
        return false;
    }

    @Override
    public void validate(PatchInstruction instruction) throws ValidateException {
        if (instruction.params.length != 2) {
            throw new ValidateException("Incorrect number of arguments for int-inc");
        }
        try {
            if (!instruction.params[0].equals("*")) {
                Integer.parseInt(instruction.params[0]);
            }
            if (!instruction.params[1].equals("*")) {
                Integer.parseInt(instruction.params[1]);
            }
        } catch (NumberFormatException e) {
            throw new ValidateException("Invalid number " + e.getMessage());
        }
    }
}
