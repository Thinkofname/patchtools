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

import com.google.common.collect.ImmutableList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.matching.MatchClass;
import uk.co.thinkofdeath.patchtools.matching.MatchGenerator;
import uk.co.thinkofdeath.patchtools.matching.MatchMethod;
import uk.co.thinkofdeath.patchtools.patch.Ident;
import uk.co.thinkofdeath.patchtools.patch.PatchClass;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InvokeInstruction implements InstructionHandler {

    private int opcode;

    public InvokeInstruction(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof MethodInsnNode) || insn.getOpcode() != opcode) {
            return false;
        }
        MethodInsnNode node = (MethodInsnNode) insn;

        if (patchInstruction.params.length != 3) {
            return false;
        }

        Ident cls = new Ident(patchInstruction.params[0]);
        String clsName = cls.getName();
        if (!clsName.equals("*")) {
            if (scope != null || !cls.isWeak()) {
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
                    return false;
                }
            }
        }

        Ident methodIdent = new Ident(patchInstruction.params[1]);
        String methodName = methodIdent.getName();
        if (!methodName.equals("*")) {
            if (scope != null || !methodIdent.isWeak()) {
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
                    return false;
                }
            }
        }

        Type patchDesc = Type.getMethodType(patchInstruction.params[2]);
        Type desc = Type.getMethodType(node.desc);

        if (desc.getArgumentTypes().length != patchDesc.getArgumentTypes().length) {
            return false;
        }

        for (int i = 0; i < patchDesc.getArgumentTypes().length; i++) {
            Type pt = patchDesc.getArgumentTypes()[i];
            Type t = desc.getArgumentTypes()[i];

            if (!PatchClass.checkTypes(classSet, scope, pt, t)) {
                return false;
            }
        }
        return PatchClass.checkTypes(classSet, scope, patchDesc.getReturnType(), desc.getReturnType());
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, MethodNode method) {
        if (patchInstruction.params.length != 3) {
            throw new RuntimeException("Incorrect number of arguments for invoke");
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
            opcode == Opcodes.INVOKEINTERFACE
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

    @Override
    public List<MatchClass> getReferencedClasses(PatchInstruction instruction) {
        if (instruction.params.length != 3) {
            throw new RuntimeException("Incorrect number of arguments for invoke");
        }
        ArrayList<MatchClass> classes = new ArrayList<>();
        Ident owner = new Ident(instruction.params[0]);

        if (!owner.getName().equals("*")) {
            MatchClass omc = new MatchClass(owner.getName());
            classes.add(omc);
        }
        Type desc = Type.getMethodType(instruction.params[2]);

        for (Type type : desc.getArgumentTypes()) {
            Type rt = MatchGenerator.getRootType(type);
            if (rt.getSort() == Type.OBJECT) {
                MatchClass argCls = new MatchClass(
                    new Ident(rt.getInternalName()).getName()
                );
                if (!argCls.getName().equals("*")) {
                    classes.add(argCls);
                }
            }
        }
        Type type = desc.getReturnType();
        Type rt = MatchGenerator.getRootType(type);
        if (rt.getSort() == Type.OBJECT) {
            MatchClass argCls = new MatchClass(
                new Ident(rt.getInternalName()).getName()
            );
            if (!argCls.getName().equals("*")) {
                classes.add(argCls);
            }
        }
        return classes;
    }

    @Override
    public List<MatchMethod> getReferencedMethods(PatchInstruction instruction) {
        if (instruction.params.length != 3) {
            throw new RuntimeException("Incorrect number of arguments for invoke");
        }
        Ident owner = new Ident(instruction.params[0]);
        Ident method = new Ident(instruction.params[1]);


        if (!owner.getName().equals("*") && !method.getName().equals("*")) {
            MatchClass omc = new MatchClass(owner.getName());
            MatchMethod mmc = new MatchMethod(omc, method.getName(), instruction.params[2]);

            Type desc = Type.getMethodType(instruction.params[2]);

            for (Type type : desc.getArgumentTypes()) {
                mmc.addArgument(type);
            }
            Type type = desc.getReturnType();
            mmc.setReturn(type);
            return Arrays.asList(mmc);
        }
        return ImmutableList.of();
    }
}
