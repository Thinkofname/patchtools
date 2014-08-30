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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.matching.MatchClass;
import uk.co.thinkofdeath.patchtools.matching.MatchMethod;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.util.List;

public class PushStringInstruction implements InstructionHandler {
    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof LdcInsnNode)) {
            return false;
        }
        LdcInsnNode ldcInsnNode = (LdcInsnNode) insn;
        String cst = Joiner.on(' ').join(patchInstruction.params);

        if (cst.equals("*")) {
            return true;
        }

        if (ldcInsnNode.cst instanceof String) {
            if (!cst.startsWith("\"") || !cst.endsWith("\"")) {
                return false;
            }
            cst = cst.substring(1, cst.length() - 1);
            if (!ldcInsnNode.cst.equals(cst)) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        String cst = Joiner.on(' ').join(instruction.params);
        return new LdcInsnNode(Utils.parseConstant(cst));
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof LdcInsnNode) || !(((LdcInsnNode) insn).cst instanceof String)) {
            return false;
        }
        patch.append("push-string ");
        Utils.printConstant(patch, ((LdcInsnNode) insn).cst);
        return true;
    }

    @Override
    public List<MatchClass> getReferencedClasses(PatchInstruction instruction) {
        return ImmutableList.of();
    }

    @Override
    public List<MatchMethod> getReferencedMethods(PatchInstruction instruction) {
        return ImmutableList.of();
    }
}
