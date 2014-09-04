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
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class LookupSwitchInstruction implements InstructionHandler {
    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method, AbstractInsnNode insn) {
        if (instruction.params.length != 1
            || !(insn instanceof LookupSwitchInsnNode)) {
            return false;
        }
        LookupSwitchInsnNode insnNode = (LookupSwitchInsnNode) insn;

        if (!Utils.checkOrSetLabel(scope, method, instruction.params[0], insnNode.dflt)) {
            return false;
        }

        if (insnNode.labels.size() < instruction.meta.size()) return false;

        for (int i = 0; i < instruction.meta.size(); i++) {
            String[] parts = instruction.meta.get(i).split(":");
            String key = parts[0].trim();
            String label = parts[1].trim();
            if (!Utils.equalOrWild(key, insnNode.keys.get(i))
                || !Utils.checkOrSetLabel(scope, method, label, insnNode.labels.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        if (instruction.params.length != 1) {
            throw new RuntimeException("Incorrect number of arguments for switch-table");
        }
        LookupSwitchInsnNode insnNode = new LookupSwitchInsnNode(
            Utils.getLabel(scope, method, instruction.params[0]),
            null, null
        );
        instruction.meta.stream()
            .map(label -> label.split(":")[1].trim())
            .map(label -> Utils.getLabel(scope, method, label))
            .forEach(insnNode.labels::add);
        instruction.meta.stream()
            .map(label -> label.split(":")[0].trim())
            .map(Integer::parseInt)
            .forEach(insnNode.keys::add);
        return insnNode;
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof LookupSwitchInsnNode)) {
            return false;
        }

        LookupSwitchInsnNode insnNode = (LookupSwitchInsnNode) insn;

        patch.append("switch-lookup ")
            .append('~')
            .append(Utils.printLabel(method, insnNode.dflt))
            .append('\n');
        for (int i = 0; i < insnNode.labels.size(); i++) {
            LabelNode label = insnNode.labels.get(i);
            int key = insnNode.keys.get(i);
            patch.append("    ")
                .append("    ")
                .append("    ")
                .append(Integer.toString(key))
                .append(':')
                .append('~')
                .append(Utils.printLabel(method, label))
                .append('\n');
        }
        patch.append("    ")
            .append("    ")
            .append(".end-switch-lookup");
        return true;
    }
}
