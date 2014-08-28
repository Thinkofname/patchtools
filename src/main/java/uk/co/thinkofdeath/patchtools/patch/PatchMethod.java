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

package uk.co.thinkofdeath.patchtools.patch;

import com.google.common.collect.Maps;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PatchMethod {

    private PatchClass owner;
    private Ident ident;
    private String desc;
    private Mode mode;
    private boolean isStatic;
    private boolean isPrivate;

    private List<PatchInstruction> instructions = new ArrayList<>();

    public PatchMethod(PatchClass owner, Command mCommand, BufferedReader reader) throws IOException {
        this.owner = owner;
        if (mCommand.args.length < 2) throw new IllegalArgumentException();
        ident = new Ident(mCommand.args[0]);
        mode = mCommand.mode;
        desc = mCommand.args[1];
        if (mCommand.args.length >= 3) {
            for (int i = 2; i < mCommand.args.length; i++) {
                if (mCommand.args[i].equals("static")) {
                    isStatic = true;
                } else if (mCommand.args[i].equals("private")) {
                    isPrivate = true;
                }
            }
        }

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("//") || line.length() == 0) continue;

            Command command = Command.from(line);
            if (mode == Mode.ADD && command.mode != Mode.ADD) {
                throw new IllegalArgumentException("In added methods everything must be +");
            } else if (mode == Mode.REMOVE && command.mode != Mode.REMOVE) {
                throw new IllegalArgumentException("In removed methods everything must be -");
            }
            if (command.name.equals("end-method")) {
                return;
            }

            instructions.add(new PatchInstruction(command));
        }

    }

    public Ident getIdent() {
        return ident;
    }

    public Type getDesc() {
        return Type.getMethodType(desc);
    }

    public PatchClass getOwner() {
        return owner;
    }

    public Mode getMode() {
        return mode;
    }

    public List<PatchInstruction> getInstructions() {
        return instructions;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void apply(ClassSet classSet, PatchScope scope, MethodNode methodNode) {
        methodNode.access &= ~(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PUBLIC);
        if (isStatic) {
            methodNode.access |= Opcodes.ACC_STATIC;
        }
        if (isPrivate) {
            methodNode.access |= Opcodes.ACC_PRIVATE;
        } else {
            methodNode.access |= Opcodes.ACC_PUBLIC;
        }
        InsnList insns = methodNode.instructions;

        Map<PatchInstruction, Integer> insnMap = scope.getInstructMap(methodNode);
        int position = 0;
        int offset = 0;

        for (PatchInstruction patchInstruction : instructions) {
            if (patchInstruction.mode == Mode.ADD) {
                AbstractInsnNode newIn = patchInstruction.instruction.getHandler()
                        .create(classSet, scope, patchInstruction, methodNode);
                if (position - 1 >= 0) {
                    insns.insert(insns.get(position - 1), newIn);
                } else {
                    insns.insert(newIn);
                }
                position++;
                offset++;
                continue;
            }

            if (patchInstruction.instruction == Instruction.ANY) {
                continue;
            }

            int pos = insnMap.get(patchInstruction);
            if (patchInstruction.mode == Mode.REMOVE) {
                insns.remove(insns.get(pos));
                offset--;
            }
            position = pos + offset + 1;
        }
    }

    public boolean checkInstructions(ClassSet classSet, PatchScope scope, MethodNode methodNode) {
        int position = 0;
        InsnList insns = methodNode.instructions;

        if (((methodNode.access & Opcodes.ACC_STATIC) == 0) == isStatic) {
            return false;
        }
        if (((methodNode.access & Opcodes.ACC_PRIVATE) == 0) == isPrivate) {
            return false;
        }

        boolean wildcard = false;
        int wildcardPosition = -1;
        int wildcardPatchPosition = -1;

        Map<PatchInstruction, Integer> insnMap = Maps.newHashMap();

        check:
        for (int i = 0; i < instructions.size(); i++) {
            PatchInstruction patchInstruction = instructions.get(i);
            if (patchInstruction.mode == Mode.ADD) continue;

            if (patchInstruction.instruction == Instruction.ANY) {
                wildcard = true;
                wildcardPosition = -1;
                wildcardPatchPosition = -1;
                if (i == instructions.size() - 1) {
                    position = methodNode.instructions.size();
                }
                continue;
            }
            while (true) {

                if (position >= insns.size()) {
                    if (!wildcard) {
                        return false;
                    }
                    break;
                }
                AbstractInsnNode insn = insns.get(position);

                boolean allowLabel = insn instanceof LabelNode
                        && patchInstruction.instruction == Instruction.LABEL;

                if (!(insn instanceof LineNumberNode)
                        && (!(insn instanceof LabelNode) || allowLabel)) {
                    if (patchInstruction.instruction.getHandler()
                            .check(classSet, scope, patchInstruction, methodNode, insn)) {
                        if (wildcard) {
                            wildcardPosition = position;
                            wildcardPatchPosition = i;
                        }
                        insnMap.put(patchInstruction, position);
                        wildcard = false;
                        position++;
                        continue check;
                    } else {
                        if (!wildcard) {
                            if (wildcardPosition != -1) {
                                wildcard = true;
                                position = ++wildcardPosition;
                                i = --wildcardPatchPosition;
                                continue check;
                            } else {
                                return false;
                            }
                        }
                    }
                }
                position++;
            }
            return false;
        }

        for (; position < insns.size(); position++) {
            AbstractInsnNode insn = insns.get(position);
            if (insn instanceof LineNumberNode
                    || insn instanceof LabelNode) {
                continue;
            }
            return false;
        }

        scope.putInstructMap(methodNode, insnMap);
        return true;
    }

}
