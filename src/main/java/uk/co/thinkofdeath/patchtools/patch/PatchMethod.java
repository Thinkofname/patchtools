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
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.instructions.TryCatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class PatchMethod {

    private PatchClass owner;
    private Ident ident;
    private String desc;
    private Mode mode;
    private boolean isStatic;
    private boolean isPrivate;
    private boolean isProtected;

    private List<PatchInstruction> instructions = new ArrayList<>();

    public PatchMethod(PatchClass owner, Command mCommand, BufferedReader reader) throws IOException {
        this.owner = owner;
        if (mCommand.args.length < 2) throw new IllegalArgumentException();
        ident = new Ident(mCommand.args[0]);
        mode = mCommand.mode;
        desc = mCommand.args[1];
        if (mCommand.args.length >= 3) {
            for (int i = 2; i < mCommand.args.length; i++) {
                if (mCommand.args[i].equalsIgnoreCase("static")) {
                    isStatic = true;
                } else if (mCommand.args[i].equalsIgnoreCase("private")) {
                    isPrivate = true;
                } else if (mCommand.args[i].equalsIgnoreCase("protected")) {
                    isProtected = true;
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
            if (command.name.equalsIgnoreCase("end-method")) {
                return;
            }

            instructions.add(new PatchInstruction(command, reader));
        }

    }

    public Ident getIdent() {
        return ident;
    }

    public Type getDesc() {
        return Type.getMethodType(desc);
    }

    public String getDescRaw() {
        return desc;
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

    public boolean isProtected() {
        return isProtected;
    }

    public void apply(ClassSet classSet, PatchScope scope, MethodNode methodNode) {
        methodNode.access &= ~(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PUBLIC);
        if (isStatic) {
            methodNode.access |= Opcodes.ACC_STATIC;
        }
        if (isPrivate) {
            methodNode.access |= Opcodes.ACC_PRIVATE;
        } else if (isProtected) {
            methodNode.access |= Opcodes.ACC_PROTECTED;
        } else {
            methodNode.access |= Opcodes.ACC_PUBLIC;
        }
        InsnList insns = new InsnList();
        LabelCloneMap cloneMap = new LabelCloneMap();
        for (AbstractInsnNode insnNode : methodNode.instructions.toArray()) {
            insns.add(insnNode.clone(cloneMap));
        }

        List<TryCatchBlockNode> trys = new ArrayList<>();
        for (TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
            TryCatchBlockNode newTry = new TryCatchBlockNode(
                cloneMap.get(tryCatchBlockNode.start),
                cloneMap.get(tryCatchBlockNode.end),
                cloneMap.get(tryCatchBlockNode.handler),
                tryCatchBlockNode.type
            );
            trys.add(newTry);
        }
        methodNode.tryCatchBlocks = trys;

        Map<PatchInstruction, Integer> insnMap = scope.getInstructMap(methodNode);
        int position = 0;
        int offset = 0;

        for (PatchInstruction patchInstruction : instructions) {
            if (patchInstruction.mode == Mode.ADD) {
                if (patchInstruction.instruction == Instruction.TRY_CATCH) {
                    TryCatchInstruction.create(classSet, scope, patchInstruction, methodNode, cloneMap);
                    continue;
                }
                AbstractInsnNode newIn = patchInstruction.instruction.getHandler()
                    .create(classSet, scope, patchInstruction, methodNode);
                if (position - 1 >= 0) {
                    insns.insert(insns.get(position - 1), newIn.clone(cloneMap));
                } else {
                    insns.insert(newIn.clone(cloneMap));
                }
                position++;
                offset++;
                continue;
            }

            if (patchInstruction.instruction == Instruction.TRY_CATCH) {
                if (patchInstruction.mode == Mode.REMOVE) {
                    TryCatchBlockNode match = TryCatchInstruction.match(classSet, scope, patchInstruction, methodNode);
                    methodNode.tryCatchBlocks.remove(match);
                }
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

        methodNode.instructions = insns;
    }

    public boolean check(ClassSet classSet, PatchScope scope, MethodNode methodNode) {
        boolean ok = false;
        try {
            if (!getIdent().isWeak()
                && !methodNode.name.equals(getIdent().getName())) {
                return false;
            }

            Type patchDesc = getDesc();
            Type desc = Type.getMethodType(methodNode.desc);

            if (patchDesc.getArgumentTypes().length != desc.getArgumentTypes().length) {
                return false;
            }

            for (int i = 0; i < patchDesc.getArgumentTypes().length; i++) {
                Type pt = patchDesc.getArgumentTypes()[i];
                Type t = desc.getArgumentTypes()[i];

                if (!PatchClass.checkTypes(classSet, scope, pt, t)) {
                    return false;
                }
            }

            if (!PatchClass.checkTypes(classSet, scope, patchDesc.getReturnType(), desc.getReturnType())) {
                return false;
            }

            int position = 0;
            InsnList insns = methodNode.instructions;

            if (((methodNode.access & Opcodes.ACC_STATIC) == 0) == isStatic) {
                return false;
            }
            if (((methodNode.access & Opcodes.ACC_PRIVATE) == 0) == isPrivate) {
                return false;
            }
            if (((methodNode.access & Opcodes.ACC_PROTECTED) == 0) == isProtected) {
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
                        position = insns.size();
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
                        && (patchInstruction.instruction == Instruction.LABEL
                        || patchInstruction.instruction == Instruction.TRY_CATCH);

                    if (!(insn instanceof LineNumberNode) && !(insn instanceof FrameNode)
                        && (!(insn instanceof LabelNode) || allowLabel)) {
                        if (patchInstruction.instruction.getHandler()
                            .check(classSet, scope, patchInstruction, methodNode, insn)) {
                            if (patchInstruction.instruction == Instruction.TRY_CATCH) continue check;
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

            if (scope != null) {
                scope.putInstructMap(methodNode, insnMap);
            }
            ok = true;
            return true;
        } finally {
            if (!ok) {
                if (scope != null) {
                    scope.clearLabels(methodNode);
                    scope.clearInstructions(methodNode);
                }
            }
        }
    }

    private class LabelCloneMap implements Map<LabelNode, LabelNode> {

        private HashMap<LabelNode, LabelNode> internal = new HashMap<>();

        @Override
        public int size() {
            return internal.size();
        }

        @Override
        public boolean isEmpty() {
            return internal.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return internal.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return internal.containsValue(value);
        }

        @Override
        public LabelNode get(Object key) {
            LabelNode k = (LabelNode) key;
            if (!internal.containsKey(key)) {
                internal.put(k, new LabelNode());
            }
            return internal.get(key);
        }

        @Override
        public LabelNode put(LabelNode key, LabelNode value) {
            return internal.put(key, value);
        }

        @Override
        public LabelNode remove(Object key) {
            return internal.remove(key);
        }

        @Override
        public void putAll(Map<? extends LabelNode, ? extends LabelNode> m) {
            internal.putAll(m);
        }

        @Override
        public void clear() {
            internal.clear();
        }

        @NotNull
        @Override
        public Set<LabelNode> keySet() {
            return internal.keySet();
        }

        @NotNull
        @Override
        public Collection<LabelNode> values() {
            return internal.values();
        }

        @NotNull
        @Override
        public Set<Entry<LabelNode, LabelNode>> entrySet() {
            return internal.entrySet();
        }
    }
}
