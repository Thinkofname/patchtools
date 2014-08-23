package uk.co.thinkofdeath.patchtools.patch;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.ClassSet;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.PatchVerifyException;
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PatchMethod {

    private PatchClass owner;
    private Ident ident;
    private String desc;
    private Mode mode;
    private boolean isStatic;

    private List<PatchInstruction> instructions = new ArrayList<>();

    public PatchMethod(PatchClass owner, Command mCommand, BufferedReader reader) throws IOException {
        this.owner = owner;
        if (mCommand.args.length < 2) throw new IllegalArgumentException();
        ident = new Ident(mCommand.args[0]);
        mode = mCommand.mode;
        desc = mCommand.args[1];
        if (mCommand.args.length >= 3) {
            if (mCommand.args[2].equals("static")) {
                isStatic = true;
            }
        }

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("//") || line.length() == 0) continue;

            Command command = Command.from(line);
            if (mode == Mode.ADD && command.mode != Mode.ADD) {
                throw new IllegalArgumentException("In added classes everything must be +");
            } else if (mode == Mode.REMOVE && command.mode != Mode.REMOVE) {
                throw new IllegalArgumentException("In removed classes everything must be -");
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

    public void apply(ClassSet classSet, PatchScope scope, MethodWrapper methodWrapper) {
        int position = 0;
        MethodNode methodNode = methodWrapper.getNode();
        methodNode.access &= ~Opcodes.ACC_STATIC;
        if (isStatic) {
            methodNode.access |= Opcodes.ACC_STATIC;
        }
        InsnList insns = methodNode.instructions;
        boolean wildcard = false;

        check:
        for (int i = 0; i < instructions.size(); i++) {
            PatchInstruction patchInstruction = instructions.get(i);
            if (patchInstruction.mode == Mode.ADD) {
                AbstractInsnNode newIn = patchInstruction.instruction.getCreator()
                        .create(classSet, scope, patchInstruction);
                if (position - 1 >= 0) {
                    insns.insert(insns.get(position - 1), newIn);
                } else {
                    insns.insert(newIn);
                }
                position++;
                continue;
            }

            if (patchInstruction.instruction == Instruction.ANY) {
                wildcard = true;
                if (i == instructions.size() - 1) {
                    position = methodNode.instructions.size();
                }
                continue;
            }
            while (true) {

                if (position >= insns.size()) {
                    if (!wildcard) {
                        throw new RuntimeException();
                    }
                    break;
                }
                AbstractInsnNode insn = insns.get(position);
                try {
                    patchInstruction.instruction.getChecker()
                            .check(classSet, scope, patchInstruction, insn);
                    wildcard = false;

                    if (patchInstruction.mode == Mode.REMOVE) {
                        insns.remove(insns.get(position));
                    }
                    continue check;
                } catch (PatchVerifyException e) {
                    if (!wildcard) {
                        throw new RuntimeException(e);
                    }
                }
                position++;
            }

            throw new RuntimeException();
        }
    }

    public void checkInstructions(ClassSet classSet, PatchScope scope, MethodWrapper methodWrapper) {
        int position = 0;
        MethodNode methodNode = methodWrapper.getNode();
        InsnList insns = methodNode.instructions;

        if (((methodNode.access & Opcodes.ACC_STATIC) == 0) == isStatic) {
            throw new PatchVerifyException("Expected " + (isStatic ? "static" : "non-static"));
        }

        boolean wildcard = false;
        check:
        for (int i = 0; i < instructions.size(); i++) {
            PatchInstruction patchInstruction = instructions.get(i);
            if (patchInstruction.mode == Mode.ADD) continue;

            if (patchInstruction.instruction == Instruction.ANY) {
                wildcard = true;
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

                try {
                    patchInstruction.instruction.getChecker()
                            .check(classSet, scope, patchInstruction, insn);
                    wildcard = false;
                    position++;
                    continue check;
                } catch (PatchVerifyException e) {
                    if (!wildcard) {
                        throw e;
                    }
                }
                position++;
            }
            throw new PatchVerifyException();
        }

        if (position != insns.size()) {
            throw new PatchVerifyException(position + " : " + insns.size());
        }
    }

}