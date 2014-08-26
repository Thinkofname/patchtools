package uk.co.thinkofdeath.patchtools.instruction.instructions;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.PatchVerifyException;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.patch.Ident;
import uk.co.thinkofdeath.patchtools.patch.PatchClass;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

public class InvokeInstruction implements InstructionHandler {

    private int opcode;

    public InvokeInstruction(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public void check(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof MethodInsnNode) || insn.getOpcode() != opcode) {
            throw new PatchVerifyException();
        }
        MethodInsnNode node = (MethodInsnNode) insn;

        if (patchInstruction.params.length != 3) {
            throw new PatchVerifyException("Incorrect number of arguments for invoke");
        }

        Ident cls = new Ident(patchInstruction.params[0]);
        String clsName = cls.getName();
        if (!clsName.equals("*")) {
            if (cls.isWeak()) {
                ClassWrapper ptcls = scope.getClass(clsName);
                if (ptcls == null) { // Assume true
                    scope.putClass(classSet.getClassWrapper(node.owner), clsName);
                    clsName = node.owner;
                } else {
                    clsName = ptcls.getNode().name;
                }
            }
            if (!clsName.equals(node.owner)) {
                throw new PatchVerifyException();
            }
        }

        Ident methodIdent = new Ident(patchInstruction.params[1]);
        String methodName = methodIdent.getName();
        if (!methodName.equals("*")) {
            if (methodIdent.isWeak()) {
                ClassWrapper owner = classSet.getClassWrapper(node.owner);
                MethodWrapper ptMethod = scope.getMethod(owner, methodName, patchInstruction.params[2]);
                if (ptMethod == null) { // Assume true
                    scope.putMethod(classSet.getClassWrapper(node.owner)
                            .getMethod(node.name, node.desc), methodName, patchInstruction.params[2]);
                    methodName = node.name;
                } else {
                    methodName = ptMethod.getName();
                }
            }
            if (!methodName.equals(node.name)) {
                throw new PatchVerifyException();
            }
        }

        Type patchDesc = Type.getMethodType(patchInstruction.params[2]);
        Type desc = Type.getMethodType(node.desc);

        for (int i = 0; i < patchDesc.getArgumentTypes().length; i++) {
            Type pt = patchDesc.getArgumentTypes()[i];
            Type t = desc.getArgumentTypes()[i];

            PatchClass.checkTypes(classSet, scope, pt, t);
        }
        PatchClass.checkTypes(classSet, scope, patchDesc.getReturnType(), desc.getReturnType());
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, MethodNode method) {
        if (patchInstruction.params.length != 3) {
            throw new PatchVerifyException("Incorrect number of arguments for invoke");
        }

        Ident ownerId = new Ident(patchInstruction.params[0]);
        String owner = ownerId.getName();
        if (ownerId.isWeak()) {
            owner = scope.getClass(owner).getNode().name;
        }
        Ident nameId = new Ident(patchInstruction.params[1]);
        String name = nameId.getName();
        if (nameId.isWeak()) {
            ClassWrapper cls = classSet.getClassWrapper(owner);
            name = scope.getMethod(cls, name, patchInstruction.params[2]).getName();
        }

        StringBuilder mappedDesc = new StringBuilder("(");
        Type desc = Type.getMethodType(patchInstruction.params[2]);
        for (Type type : desc.getArgumentTypes()) {
            PatchClass.updatedTypeString(classSet, scope, mappedDesc, type);
        }
        mappedDesc.append(")");
        PatchClass.updatedTypeString(classSet, scope, mappedDesc, desc.getReturnType());
        return new MethodInsnNode(
                opcode,
                owner,
                name,
                mappedDesc.toString(),
                false
        );
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof MethodInsnNode)) {
            return false;
        }
        MethodInsnNode methodInsnNode = (MethodInsnNode) insn;
        patch.append("invoke-");
        switch (methodInsnNode.getOpcode()) {
            case Opcodes.INVOKESTATIC:
                patch.append("static");
                break;
            case Opcodes.INVOKEVIRTUAL:
                patch.append("virtual");
                break;
            case Opcodes.INVOKESPECIAL:
                patch.append("special");
                break;
            case Opcodes.INVOKEINTERFACE:
                patch.append("interface");
                break;
            default:
                throw new IllegalArgumentException("Invoke opcode: " + methodInsnNode.getOpcode());
        }
        patch.append(' ')
                .append(methodInsnNode.owner)
                .append(' ')
                .append(methodInsnNode.name)
                .append(' ')
                .append(methodInsnNode.desc);
        return true;
    }
}
