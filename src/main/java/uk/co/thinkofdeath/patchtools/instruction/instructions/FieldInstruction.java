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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.InstructionHandler;
import uk.co.thinkofdeath.patchtools.matching.MatchClass;
import uk.co.thinkofdeath.patchtools.matching.MatchField;
import uk.co.thinkofdeath.patchtools.matching.MatchGenerator;
import uk.co.thinkofdeath.patchtools.patch.Ident;
import uk.co.thinkofdeath.patchtools.patch.PatchClass;
import uk.co.thinkofdeath.patchtools.patch.PatchInstruction;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.FieldWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FieldInstruction implements InstructionHandler {

    private int opcode;

    public FieldInstruction(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public boolean check(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof FieldInsnNode) || insn.getOpcode() != opcode) {
            return false;
        }
        FieldInsnNode node = (FieldInsnNode) insn;

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

        Ident fieldIdent = new Ident(patchInstruction.params[1]);
        String fieldName = fieldIdent.getName();
        if (!fieldName.equals("*")) {
            if (scope != null || !fieldIdent.isWeak()) {
                if (fieldIdent.isWeak()) {
                    ClassWrapper owner = classSet.getClassWrapper(node.owner);
                    FieldWrapper ptField = scope.getField(owner, fieldName, patchInstruction.params[2]);
                    if (ptField == null) { // Assume true
                        scope.putField(classSet.getClassWrapper(node.owner)
                            .getField(node.name, node.desc), fieldName, patchInstruction.params[2]);
                        fieldName = node.name;
                    } else {
                        fieldName = ptField.getName();
                    }
                }
                if (!fieldName.equals(node.name)) {
                    return false;
                }
            }
        }

        Type patchDesc = Type.getType(patchInstruction.params[2]);
        Type desc = Type.getType(node.desc);

        return PatchClass.checkTypes(classSet, scope, patchDesc, desc);
    }

    @Override
    public AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction patchInstruction, MethodNode method) {
        if (patchInstruction.params.length != 3) {
            throw new RuntimeException("Incorrect number of arguments for field");
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
            name = scope.getField(cls, name, patchInstruction.params[2]).getName();
        }

        StringBuilder mappedDesc = new StringBuilder();
        Type desc = Type.getType(patchInstruction.params[2]);
        PatchClass.updatedTypeString(classSet, scope, mappedDesc, desc);
        return new FieldInsnNode(
            opcode,
            owner,
            name,
            mappedDesc.toString()
        );
    }

    @Override
    public boolean print(Instruction instruction, StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        if (!(insn instanceof FieldInsnNode)) {
            return false;
        }
        FieldInsnNode fieldInsnNode = (FieldInsnNode) insn;
        switch (fieldInsnNode.getOpcode()) {
            case Opcodes.GETFIELD:
                patch.append("get-field");
                break;
            case Opcodes.GETSTATIC:
                patch.append("get-static");
                break;
            case Opcodes.PUTFIELD:
                patch.append("put-field");
                break;
            case Opcodes.PUTSTATIC:
                patch.append("put-static");
                break;
            default:
                throw new IllegalArgumentException("Invoke opcode: " + fieldInsnNode.getOpcode());
        }
        patch.append(' ')
            .append(fieldInsnNode.owner)
            .append(' ')
            .append(fieldInsnNode.name)
            .append(' ')
            .append(fieldInsnNode.desc);
        return true;
    }

    @Override
    public List<MatchClass> getReferencedClasses(PatchInstruction instruction) {
        if (instruction.params.length != 3) {
            throw new RuntimeException("Incorrect number of arguments for field");
        }
        ArrayList<MatchClass> classes = new ArrayList<>();
        Ident owner = new Ident(instruction.params[0]);
        MatchClass omc = new MatchClass(owner.getName());
        classes.add(omc);
        Type desc = Type.getType(instruction.params[2]);

        Type rt = MatchGenerator.getRootType(desc);
        if (rt.getSort() == Type.OBJECT) {
            MatchClass argCls = new MatchClass(
                new Ident(rt.getInternalName()).getName()
            );
            classes.add(argCls);
        }
        return classes;
    }

    @Override
    public List<MatchField> getReferencedFields(PatchInstruction instruction) {
        if (instruction.params.length != 3) {
            throw new RuntimeException("Incorrect number of arguments for field");
        }
        Ident owner = new Ident(instruction.params[0]);
        Ident field = new Ident(instruction.params[1]);
        MatchClass omc = new MatchClass(owner.getName());
        MatchField mmc = new MatchField(omc, field.getName(), instruction.params[2]);

        Type desc = Type.getType(instruction.params[2]);
        mmc.setType(desc);
        return Arrays.asList(mmc);
    }
}
