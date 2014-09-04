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

package uk.co.thinkofdeath.patchtools.disassemble;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.instructions.Utils;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;

import java.util.Arrays;

public class Disassembler {

    private final ClassSet classSet;

    public Disassembler(ClassSet classSet) {
        this.classSet = classSet;
    }

    public String disassemble(String cls) {
        ClassWrapper classWrapper = classSet.getClassWrapper(cls);

        StringBuilder patch = new StringBuilder("\n");

        ClassNode node = classWrapper.getNode();

        patch.append(".class ")
            .append(node.name)
            .append('\n');

        if (node.superName != null) {
            patch.append("    .super ")
                .append(node.superName)
                .append('\n');
        }

        for (String inter : node.interfaces) {
            patch.append("    .interface ")
                .append(inter)
                .append('\n');

        }

        node.fields.forEach(f -> {
            patch.append("    ")
                .append(".field ")
                .append(f.name)
                .append(' ')
                .append(f.desc);
            if ((f.access & Opcodes.ACC_STATIC) != 0) {
                patch.append(" static");
            }
            if ((f.access & Opcodes.ACC_PRIVATE) != 0) {
                patch.append(" private");
            }
            if (f.value != null) {
                patch.append(" ");
                Utils.printConstant(patch, f.value);
            }
            patch.append('\n');
        });

        patch.append('\n');

        node.methods.forEach(m -> {
            patch.append("    ")
                .append(".method ")
                .append(m.name)
                .append(' ')
                .append(m.desc);
            if ((m.access & Opcodes.ACC_STATIC) != 0) {
                patch.append(" static");
            }
            if ((m.access & Opcodes.ACC_PRIVATE) != 0) {
                patch.append(" private");
            }
            if ((m.access & Opcodes.ACC_PROTECTED) != 0) {
                patch.append(" protected");
            }
            patch.append('\n');

            Instruction.TRY_CATCH.getHandler().print(
                Instruction.TRY_CATCH,
                patch,
                m,
                null
            );

            Arrays.stream(m.instructions.toArray())
                .filter(i -> !(i instanceof LineNumberNode))
                .filter(i -> !(i instanceof FrameNode))
                .filter(i -> !(i instanceof LabelNode) || isInUse(m, (LabelNode) i))
                .forEach(i -> {
                    patch.append("    ")
                        .append("    ")
                        .append('.');
                    if (!Instruction.print(patch, m, i)) {
                        // TODO: throw new UnsupportedOperationException(i.toString());
                        patch.append("unsupported ")
                            .append(i.getOpcode())
                            .append(' ')
                            .append(i);
                    }
                    patch.append('\n');
                });

            patch.append("    ")
                .append(".end-method\n");
            patch.append('\n');
        });

        patch.append(".end-class\n");

        return patch.toString();
    }

    private boolean isInUse(MethodNode m, LabelNode label) {
        for (TryCatchBlockNode tryNode : m.tryCatchBlocks) {
            if (tryNode.start == label) return true;
            if (tryNode.end == label) return true;
            if (tryNode.handler == label) return true;
        }
        for (AbstractInsnNode insnNode : m.instructions.toArray()) {
            if (insnNode instanceof JumpInsnNode) {
                if (((JumpInsnNode) insnNode).label == label) {
                    return true;
                }
            } else if (insnNode instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode lookup = (LookupSwitchInsnNode) insnNode;
                if (lookup.dflt == label) {
                    return true;
                }
                if (lookup.labels.stream().anyMatch(label::equals)) {
                    return true;
                }
            } else if (insnNode instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode lookup = (TableSwitchInsnNode) insnNode;
                if (lookup.dflt == label) {
                    return true;
                }
                if (lookup.labels.stream().anyMatch(label::equals)) {
                    return true;
                }
            }
        }
        return false;
    }
}
