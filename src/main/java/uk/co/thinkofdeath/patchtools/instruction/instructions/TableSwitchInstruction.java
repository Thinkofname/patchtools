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
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.Ident;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class TableSwitchInstruction implements InstructionHandler {
    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method, AbstractInsnNode insn) {
        if (instruction.params.length != 3
            || !(insn instanceof TableSwitchInsnNode)) {
            return false;
        }
        TableSwitchInsnNode insnNode = (TableSwitchInsnNode) insn;

        if (!Utils.equalOrWild(instruction.params[0], insnNode.min)
            || !Utils.equalOrWild(instruction.params[1], insnNode.max)
            || !Utils.checkOrSetLabel(scope, method, instruction.params[2], insnNode.dflt)) {
            return false;
        }

        if (insnNode.labels.size() < instruction.meta.size()) return false;

        for (int i = 0; i < instruction.meta.size(); i++) {
            if (!Utils.checkOrSetLabel(scope, method, instruction.meta.get(i), insnNode.labels.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        if (instruction.params.length != 3) {
            throw new RuntimeException("Incorrect number of arguments for switch-table");
        }
        TableSwitchInsnNode insnNode = new TableSwitchInsnNode(
            Integer.parseInt(instruction.params[0]),
            Integer.parseInt(instruction.params[1]),
            Utils.getLabel(scope, method, instruction.params[2])
        );
        instruction.meta.stream()
            .map(label -> Utils.getLabel(scope, method, label))
            .forEach(insnNode.labels::add);
        return insnNode;
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof TableSwitchInsnNode)) {
            return false;
        }

        TableSwitchInsnNode insnNode = (TableSwitchInsnNode) insn;

        patch.append("switch-table ")
            .append(insnNode.min)
            .append(' ')
            .append(insnNode.max)
            .append(' ')
            .append('~')
            .append(insnNode.dflt.getLabel())
            .append('\n');
        for (LabelNode label : insnNode.labels) {
            patch.append("    ")
                .append("    ")
                .append("    ")
                .append('~')
                .append(label.getLabel())
                .append('\n');
        }
        patch.append("    ")
            .append("    ")
            .append(".end-switch-table");
        return true;
    }

    private boolean checkLabel(PatchScope scope, MethodNode method, String name, LabelNode label) {
        Ident ident = new Ident(name);
        if (!ident.isWeak()) {
            if (!ident.getName().equals("*")) {
                return false;
            }
        } else if (scope != null) {
            scope.putLabel(method, label, ident.getName());
        }
        return true;
    }
}
