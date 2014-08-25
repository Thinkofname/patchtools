package uk.co.thinkofdeath.patchtools.patch;

import com.google.common.collect.Maps;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.PatchVerifyException;
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
        ;
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
                AbstractInsnNode newIn = patchInstruction.instruction.getCreator()
                        .create(classSet, scope, patchInstruction);
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

    public void checkInstructions(ClassSet classSet, PatchScope scope, MethodNode methodNode) {
        int position = 0;
        InsnList insns = methodNode.instructions;

        if (((methodNode.access & Opcodes.ACC_STATIC) == 0) == isStatic) {
            throw new PatchVerifyException("Expected " + (isStatic ? "static" : "non-static"));
        }
        if (((methodNode.access & Opcodes.ACC_PRIVATE) == 0) == isPrivate) {
            throw new PatchVerifyException("Expected " + (isStatic ? "private" : "non-private"));
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
                        throw new PatchVerifyException();
                    }
                    break;
                }
                AbstractInsnNode insn = insns.get(position);

                if (!(insn instanceof LineNumberNode)) {
                    try {
                        patchInstruction.instruction.getChecker()
                                .check(classSet, scope, patchInstruction, insn);
                        if (wildcard) {
                            wildcardPosition = position;
                            wildcardPatchPosition = i;
                        }
                        insnMap.put(patchInstruction, position);
                        wildcard = false;
                        position++;
                        continue check;
                    } catch (PatchVerifyException e) {
                        if (!wildcard) {
                            if (wildcardPosition != -1) {
                                wildcard = true;
                                position = ++wildcardPosition;
                                i = --wildcardPatchPosition;
                                continue check;
                            } else {
                                throw e;
                            }
                        }
                    }
                }
                position++;
            }
            throw new PatchVerifyException();
        }

        if (position != insns.size()) {
            throw new PatchVerifyException(position + " : " + insns.size());
        }

        scope.putInstructMap(methodNode, insnMap);
    }

}
