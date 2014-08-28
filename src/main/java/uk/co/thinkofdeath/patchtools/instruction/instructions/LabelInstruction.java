package uk.co.thinkofdeath.patchtools.instruction.instructions;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.Ident;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class LabelInstruction implements InstructionHandler {
    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof LabelNode)) {
            return false;
        }
        if (instruction.params.length != 1) {
            return false;
        }

        Ident ident = new Ident(instruction.params[0]);
        if (!ident.isWeak()) {
            if (!ident.getName().equals("*")) {
                return false;
            }
        } else {
            scope.putLabel(method, ((LabelNode) insn).getLabel(), ident.getName());
        }
        return true;
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction, MethodNode method) {
        if (instruction.params.length != 1) {
            throw new RuntimeException("Incorrect number of arguments for label");
        }

        Ident ident = new Ident(instruction.params[0]);
        if (!ident.isWeak()) {
            throw new UnsupportedOperationException("Weak labels");
        }

        Label label = scope.getLabel(method, ident.getName());
        if (label == null) {
            label = new Label();
            scope.putLabel(method, label, ident.getName());
        }
        return new LabelNode(label);
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof LabelNode)) {
            return false;
        }
        patch.append("label ")
                .append(((LabelNode) insn).getLabel());
        return true;
    }
}
