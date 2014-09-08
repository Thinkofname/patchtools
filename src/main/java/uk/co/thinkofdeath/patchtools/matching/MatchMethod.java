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

package uk.co.thinkofdeath.patchtools.matching;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.patch.*;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.FieldWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

import java.util.*;

public class MatchMethod {

    private final MatchClass owner;
    private final String name;
    private final String desc;
    private final List<Type> arguments = new ArrayList<>();
    private Type returnType;

    private List<MethodPair> matchedMethods = new ArrayList<>();
    private Set<MethodPair> checkedMethods = new HashSet<>();

    public MatchMethod(MatchClass owner, String name, String desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public void addArgument(Type type) {
        arguments.add(type);
    }

    public void setReturn(Type type) {
        returnType = type;
    }

    public List<Type> getArguments() {
        return arguments;
    }

    public Type getReturnType() {
        return returnType;
    }

    public MatchClass getOwner() {
        return owner;
    }

    public String getDesc() {
        return desc;
    }

    public void addMatch(@NotNull ClassNode owner, @NotNull MethodNode methodNode) {
        if (!checkedMethods.contains(new MethodPair(owner, methodNode))) {
            matchedMethods.add(new MethodPair(owner, methodNode));
        }
    }

    public void removeMatch(@NotNull ClassNode owner, @NotNull MethodNode methodNode) {
        matchedMethods.remove(new MethodPair(owner, methodNode));
    }

    public void removeMatch(ClassNode clazz) {
        ListIterator<MethodPair> it = matchedMethods.listIterator();
        while (it.hasNext()) {
            MethodPair pair = it.next();
            if (pair.owner == clazz) {
                it.remove();
            }
        }
    }

    public void addChecked(@NotNull ClassNode owner, @NotNull MethodNode methodNode) {
        checkedMethods.add(new MethodPair(owner, methodNode));
    }

    public boolean hasUnchecked() {
        return matchedMethods.stream().anyMatch(methodPair -> !checkedMethods.contains(methodPair));
    }

    public MethodPair[] getUncheckedMethods() {
        return matchedMethods.stream()
            .filter(c -> !checkedMethods.contains(c))
            .toArray(MethodPair[]::new);
    }

    public List<MethodNode> getMatches() {
        return Arrays.asList(matchedMethods.stream().map(MethodPair::getNode).toArray(MethodNode[]::new));
    }

    public List<MethodNode> getMatches(ClassNode owner) {
        return Arrays.asList(matchedMethods.stream()
            .filter(m -> m.getOwner() == owner)
            .map(MethodPair::getNode).toArray(MethodNode[]::new));
    }

    public boolean usesNode(ClassNode clazz) {
        return matchedMethods.stream()
            .anyMatch(m -> m.owner == clazz);
    }

    public void check(ClassSet classSet, PatchClasses patchClasses,
                      MatchGroup group, MethodPair pair) {
        MethodNode node = pair.getNode();
        addChecked(pair.getOwner(), pair.getNode());

        List<MatchPair> matchPairs = new ArrayList<>();

        Type type = Type.getMethodType(node.desc);

        if (type.getArgumentTypes().length != getArguments().size()) {
            removeMatch(pair.getOwner(), node);
            return;
        }

        Type ret = type.getReturnType();
        if (ret.getSort() != getReturnType().getSort()) {
            removeMatch(pair.getOwner(), node);
            return;
        } else if (ret.getSort() == Type.OBJECT) {
            MatchClass retCls = group.getClass(new MatchClass(new Ident(getReturnType().getInternalName()).getName()));
            ClassWrapper wrapper = classSet.getClassWrapper(ret.getInternalName());
            if (wrapper != null && !wrapper.isHidden()) {
                matchPairs.add(new MatchPair.ClassMatch(retCls, wrapper.getNode()));
            }
        }

        Type[] argumentTypes = type.getArgumentTypes();
        for (int i = 0; i < argumentTypes.length; i++) {
            Type arg = argumentTypes[i];
            if (arg.getSort() != getArguments().get(i).getSort()) {
                removeMatch(pair.getOwner(), node);
                return;
            } else if (arg.getSort() == Type.OBJECT) {
                MatchClass argCls = group.getClass(new MatchClass(new Ident(getArguments().get(i).getInternalName()).getName()));
                ClassWrapper wrapper = classSet.getClassWrapper(arg.getInternalName());
                if (wrapper != null && !wrapper.isHidden()) {
                    matchPairs.add(new MatchPair.ClassMatch(argCls, wrapper.getNode()));
                }
            }
        }

        PatchClass pc = patchClasses.getClass(getOwner().getName());
        if (pc != null) {
            PatchMethod pm = pc.getMethods().stream().filter(
                m -> m.getIdent().getName().equals(getName())
                    && m.getDescRaw().equals(getDesc())
            ).findFirst().orElse(null);

            if (pm != null) {
                if (!pm.check(classSet, null, node)) {
                    removeMatch(pair.getOwner(), node);
                    return;
                }

                ListIterator<AbstractInsnNode> it = node.instructions.iterator();
                Set<ClassNode> referencedClasses = new HashSet<>();
                Set<MatchMethod.MethodPair> referencedMethods = new HashSet<>();
                Set<MatchField.FieldPair> referencedFields = new HashSet<>();
                while (it.hasNext()) {
                    AbstractInsnNode insn = it.next();

                    if (insn instanceof MethodInsnNode) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) insn;

                        ClassWrapper cls = classSet.getClassWrapper(methodInsnNode.owner);
                        if (cls == null || cls.isHidden()) continue;

                        referencedClasses.add(cls.getNode());

                        MethodWrapper wrap = cls.getMethod(methodInsnNode.name, methodInsnNode.desc);
                        if (wrap != null) {
                            referencedMethods.add(new MatchMethod.MethodPair(
                                cls.getNode(),
                                cls.getMethodNode(wrap)
                            ));
                        }
                    } else if (insn instanceof FieldInsnNode) {
                        FieldInsnNode fieldInsnNode = (FieldInsnNode) insn;

                        ClassWrapper cls = classSet.getClassWrapper(fieldInsnNode.owner);
                        if (cls == null || cls.isHidden()) continue;

                        referencedClasses.add(cls.getNode());

                        FieldWrapper wrap = cls.getField(fieldInsnNode.name, fieldInsnNode.desc);
                        if (wrap != null) {
                            referencedFields.add(new MatchField.FieldPair(
                                cls.getNode(),
                                cls.getFieldNode(wrap)
                            ));
                        }
                    } else if (insn instanceof LdcInsnNode) {
                        LdcInsnNode ldc = (LdcInsnNode) insn;
                        if (ldc.cst instanceof Type) {
                            ClassWrapper cls = classSet.getClassWrapper(((Type) ldc.cst).getInternalName());
                            if (cls == null || cls.isHidden()) continue;

                            referencedClasses.add(cls.getNode());
                        }
                    } else if (insn instanceof TypeInsnNode) {
                        TypeInsnNode tNode = (TypeInsnNode) insn;
                        String desc = tNode.desc;
                        ClassWrapper cls = classSet.getClassWrapper(MatchGenerator.getRootType(Type.getObjectType(desc)).getInternalName());
                        if (cls == null || cls.isHidden()) continue;

                        referencedClasses.add(cls.getNode());
                    } else if (insn instanceof MultiANewArrayInsnNode) {
                        MultiANewArrayInsnNode tNode = (MultiANewArrayInsnNode) insn;
                        String desc = tNode.desc;
                        ClassWrapper cls = classSet.getClassWrapper(MatchGenerator.getRootType(Type.getObjectType(desc)).getInternalName());
                        if (cls == null || cls.isHidden()) continue;

                        referencedClasses.add(cls.getNode());
                    }
                }

                for (PatchInstruction instruction : pm.getInstructions()) {
                    Instruction in = instruction.instruction;
                    if (in.getHandler() == null || instruction.mode == Mode.ADD) continue;
                    in.getHandler().getReferencedClasses(instruction)
                        .forEach(c -> referencedClasses.forEach(rc -> {
                                ClassWrapper wrapper = classSet.getClassWrapper(rc.name);
                                if (!wrapper.isHidden()) {
                                    matchPairs.add(new MatchPair.ClassMatch(group.getClass(c), rc));
                                }
                            })
                        );
                    in.getHandler().getReferencedMethods(instruction)
                        .forEach(me -> {
                            MatchClass matchClass = group.getClass(me.getOwner());
                            final MatchMethod fme = matchClass.addMethod(me);
                            referencedMethods.forEach(rm -> matchPairs.add(new MatchPair.MethodMatch(fme, rm.getOwner(), rm.getNode())));
                        });
                    in.getHandler().getReferencedFields(instruction)
                        .forEach(fe -> {
                            MatchClass matchClass = group.getClass(fe.getOwner());
                            final MatchField ffe = matchClass.addField(fe);
                            referencedFields.forEach(rf -> matchPairs.add(new MatchPair.FieldMatch(ffe, rf.getOwner(), rf.getNode())));
                        });
                }
            }
        }

        matchPairs.forEach(MatchPair::apply);
    }

    public static class MethodPair {
        private ClassNode owner;
        private MethodNode node;

        public MethodPair(ClassNode owner, MethodNode node) {
            this.owner = owner;
            this.node = node;
        }

        public ClassNode getOwner() {
            return owner;
        }

        public MethodNode getNode() {
            return node;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MethodPair that = (MethodPair) o;

            return node.equals(that.node) && owner.equals(that.owner);

        }

        @Override
        public int hashCode() {
            int result = owner.hashCode();
            result = 31 * result + node.hashCode();
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MatchMethod that = (MatchMethod) o;

        return desc.equals(that.desc)
            && name.equals(that.name)
            && owner.equals(that.owner);

    }

    @Override
    public int hashCode() {
        int result = owner.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + desc.hashCode();
        return result;
    }
}
