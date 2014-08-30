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

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.jetbrains.annotations.Contract;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.patch.*;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

import java.util.*;
import java.util.function.BiPredicate;

// Find links between patch classes
// Split into groups if every class isn't linked
// Test one group at a time
// Find links between normal classes

public class MatchGenerator {

    private final ClassSet classSet;
    private final PatchClasses patchClasses;
    private final PatchScope scope;
    private final List<MatchGroup> groups = new ArrayList<>();

    private final TObjectIntMap<Object> state = new TObjectIntHashMap<>();

    public MatchGenerator(ClassSet classSet, PatchClasses patchClasses, PatchScope scope) {
        this.classSet = classSet;
        this.patchClasses = patchClasses;
        this.scope = scope;

        // To work out the links between the patch classes
        // we start with a the first class and branch out.
        // Then we move onto the next unvisited class since
        // not every class in a patch may be linked
        Map<MatchClass, MatchGroup> visited = new HashMap<>();
        patchClasses.getClasses().stream()
                .filter(p -> p.getMode() != Mode.ADD)
                .forEach(clazz -> {
                    MatchClass cls = new MatchClass(clazz.getIdent().getName());
                    if (visited.containsKey(cls)) {
                        return;
                    }

                    MatchGroup group = new MatchGroup();
                    visited.put(cls, group);
                    groups.add(group);
                    Stack<MatchClass> visitList = new Stack<>();
                    visitList.add(cls);
                    while (!visitList.isEmpty()) {
                        MatchClass mc = visitList.pop();
                        PatchClass pc = patchClasses.getClass(mc.getName());
                        if (pc == null || pc.getMode() == Mode.ADD) continue;
                        group.add(mc);

                        pc.getExtends().stream()
                                .filter(c -> c.getMode() != Mode.ADD)
                                .forEach(c -> {
                                    MatchClass matchClass = new MatchClass(c.getIdent().getName());
                                    mc.setSuperClass(matchClass);
                                    addToVisited(visited, visitList, matchClass, group);
                                });
                        pc.getInterfaces().stream()
                                .filter(c -> c.getMode() != Mode.ADD)
                                .forEach(c -> {
                                    MatchClass matchClass = new MatchClass(c.getIdent().getName());
                                    mc.addInterface(matchClass);
                                    addToVisited(visited, visitList, matchClass, group);
                                });

                        // TODO: Fields

                        pc.getMethods().stream()
                                .filter(m -> m.getMode() != Mode.ADD)
                                .forEach(m -> {
                                    Type desc = m.getDesc();

                                    MatchMethod method = new MatchMethod(mc, m.getIdent().getName(), m.getDescRaw());
                                    mc.addMethod(method);

                                    for (Type type : desc.getArgumentTypes()) {
                                        Type rt = getRootType(type);
                                        if (rt.getSort() == Type.OBJECT) {
                                            MatchClass argCls = new MatchClass(
                                                    new Ident(rt.getInternalName()).getName()
                                            );
                                            addToVisited(visited, visitList, argCls, group);
                                        }
                                        method.addArgument(type);
                                    }
                                    Type type = desc.getReturnType();
                                    Type rt = getRootType(type);
                                    if (rt.getSort() == Type.OBJECT) {
                                        MatchClass argCls = new MatchClass(
                                                new Ident(rt.getInternalName()).getName()
                                        );
                                        addToVisited(visited, visitList, argCls, group);
                                    }
                                    method.setReturn(type);

                                    for (PatchInstruction instruction : m.getInstructions()) {
                                        Instruction in = instruction.instruction;
                                        if (in.getHandler() == null) continue;
                                        in.getHandler().getReferencedClasses(instruction).stream()
                                                .forEach(c -> addToVisited(visited, visitList, c, group));
                                        in.getHandler().getReferencedMethods(instruction)
                                                .forEach(me -> {
                                                    MatchClass owner = group.getClass(me.getOwner());
                                                    owner.addMethod(me);
                                                });
                                    }
                                });
                    }
                });
        System.out.println("Groups: " + groups.size());
        groups.forEach(g -> {
            System.out.println("=Group");
            g.getClasses().forEach(c -> System.out.println("  " + c.getName()));
        });
        System.out.println("Reducing");

        for (MatchGroup group : groups) {

            MatchClass first = group.getFirst();

            Arrays.stream(classSet.classes(true))
                    .map(classSet::getClassWrapper)
                    .map(ClassWrapper::getNode)
                    .forEach(first::addMatch);

            boolean doneSomething = true;
            while (doneSomething) {
                doneSomething = false;
                while (true) {
                    MatchClass cls = group.getClasses().stream()
                            .filter(MatchClass::hasUnchecked).findAny().orElse(null);
                    if (cls == null) {
                        break;
                    }
                    doneSomething = true;

                    ClassNode[] unchecked = cls.getUncheckedClasses();
                    for (ClassNode node : unchecked) {
                        cls.addChecked(node);

                        if (cls.getSuperClass() != null) {
                            ClassWrapper su = classSet.getClassWrapper(node.superName);
                            if (!su.isHidden()) {
                                cls.getSuperClass().addMatch(su.getNode());
                            }
                        }

                        for (String inter : node.interfaces) {
                            ClassWrapper su = classSet.getClassWrapper(inter);
                            if (!su.isHidden()) {
                                cls.getInterfaces().forEach(i -> i.addMatch(su.getNode()));
                            }
                        }

                        // TODO: fields

                        cls.getMethods().forEach(m -> node.methods.forEach(n -> m.addMatch(node, n)));
                    }
                }

                // TODO: fields

                while (true) {
                    MatchMethod method = group.getClasses().stream()
                            .flatMap(c -> c.getMethods().stream())
                            .filter(MatchMethod::hasUnchecked)
                            .findAny().orElse(null);
                    if (method == null) {
                        break;
                    }
                    doneSomething = true;

                    MatchMethod.MethodPair[] unchecked = method.getUncheckedMethods();
                    methodCheck:
                    for (MatchMethod.MethodPair pair : unchecked) {
                        MethodNode node = pair.getNode();
                        method.addChecked(node);

                        List<MatchPair> matchPairs = new ArrayList<>();

                        Type type = Type.getMethodType(node.desc);
                        Type ret = type.getReturnType();
                        if (ret.getSort() != method.getReturnType().getSort()) {
                            method.removeMatch(pair.getOwner(), node);
                            continue;
                        } else if (ret.getSort() == Type.OBJECT) {
                            MatchClass retCls = group.getClass(new MatchClass(new Ident(method.getReturnType().getInternalName()).getName()));
                            ClassWrapper wrapper = classSet.getClassWrapper(ret.getInternalName());
                            if (!wrapper.isHidden()) {
                                matchPairs.add(new MatchPair.ClassMatch(retCls, wrapper.getNode()));
                            }
                        }

                        if (type.getArgumentTypes().length != method.getArguments().size()) {
                            method.removeMatch(pair.getOwner(), node);
                            continue;
                        }

                        Type[] argumentTypes = type.getArgumentTypes();
                        for (int i = 0; i < argumentTypes.length; i++) {
                            Type arg = argumentTypes[i];
                            if (arg.getSort() != method.getArguments().get(i).getSort()) {
                                method.removeMatch(pair.getOwner(), node);
                                continue methodCheck;
                            } else if (arg.getSort() == Type.OBJECT) {
                                MatchClass argCls = group.getClass(new MatchClass(new Ident(method.getArguments().get(i).getInternalName()).getName()));
                                ClassWrapper wrapper = classSet.getClassWrapper(arg.getInternalName());
                                if (!wrapper.isHidden()) {
                                    matchPairs.add(new MatchPair.ClassMatch(argCls, wrapper.getNode()));
                                }
                            }
                        }

                        PatchClass pc = patchClasses.getClass(method.getOwner().getName());
                        if (pc != null) {
                            PatchMethod pm = pc.getMethods().stream().filter(
                                    m -> m.getIdent().getName().equals(method.getName())
                                            && m.getDescRaw().equals(method.getDesc())
                            ).findFirst().orElse(null);

                            if (pm != null) {
                                if (!pm.checkInstructions(classSet, null, node)) {
                                    method.removeMatch(pair.getOwner(), node);
                                    continue methodCheck;
                                }

                                ListIterator<AbstractInsnNode> it = node.instructions.iterator();
                                Set<ClassNode> referencedClasses = new HashSet<>();
                                Set<MatchMethod.MethodPair> referencedMethods = new HashSet<>();
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
                                    }
                                }

                                for (PatchInstruction instruction : pm.getInstructions()) {
                                    Instruction in = instruction.instruction;
                                    if (in.getHandler() == null) continue;
                                    in.getHandler().getReferencedClasses(instruction)
                                            .forEach(c -> referencedClasses.forEach(rc -> {
                                                        ClassWrapper wrapper = classSet.getClassWrapper(rc.name);
                                                        if (!wrapper.isHidden()) {
                                                            matchPairs.add(new MatchPair.ClassMatch(c, rc));
                                                        }
                                                    })
                                            );
                                    in.getHandler().getReferencedMethods(instruction)
                                            .forEach(me ->
                                                            referencedMethods.forEach(rm -> matchPairs.add(new MatchPair.MethodMatch(me, rm.getOwner(), rm.getNode())))
                                            );
                                }
                            }
                        }

                        matchPairs.forEach(MatchPair::apply);
                    }
                }

                if (!doneSomething) {
                    String clss[] = classSet.classes(true);
                    if (group.getClasses().stream()
                            .filter(c -> c.getMatches().size() == 0 && !c.hasChecked(clss.length))
                            .count() != 0) {
                        group.getClasses().stream().filter(c -> c.getMatches().size() == 0 && !c.hasChecked(clss.length))
                                .forEach(c -> {
                                    Arrays.stream(clss)
                                            .map(classSet::getClassWrapper)
                                            .map(ClassWrapper::getNode)
                                            .forEach(c::addMatch);
                                });
                        doneSomething = true;
                    }
                }
            }

            for (MatchClass cls : group.getClasses()) {
                List<ClassNode> matches = new ArrayList<>(cls.getMatches());
                for (ClassNode clazz : matches) {
                    if (!cls.getMethods().stream()
                            .allMatch(m -> m.usesNode(clazz))) {
                        cls.removeMatch(clazz);
                        cls.getMethods().forEach(m -> m.removeMatch(clazz));
                    }
                }
            }
        }

        System.out.println("Groups: " + groups.size());
        groups.forEach(g -> {
            System.out.println("=Group");
            g.getClasses().forEach(c -> {
                System.out.printf("  %s with %d matches\n", c.getName(), c.getMatches().size());
                c.getMethods().forEach(m -> {
                    System.out.printf("    %s with %d matches\n", m.getName(), m.getMatches().size());
                });
            });
        });

        groups.forEach(g -> {
            g.getClasses().forEach(c -> {
                state.put(c, 0);
                c.getMethods().forEach(m -> state.put(m, 0));
            });
        });
    }

    @Contract("null -> fail")
    public PatchScope apply(BiPredicate<MatchGroup, PatchScope> test) {

        List<PatchScope> scopes = new ArrayList<>();
        groupCheck:
        for (MatchGroup group : groups) {

            List<Object> tickList = generateTickList(group);

            long tick = 0;

            do {
                if (tick % 1000 == 0) {
                    System.out.printf("%d ticks\r", tick);
                }
                tick++;

                PatchScope testScope = generateScope(group, new PatchScope(scope));
                if (testScope == null) continue;

                if (test.test(group, testScope)) {
                    System.out.println("Found");
                    scopes.add(testScope);
                    continue groupCheck;
                }
            } while (tick(tickList));
            throw new RuntimeException("Unable to find match");
        }
        System.out.println();
        PatchScope finalScope = new PatchScope(scope);
        scopes.forEach(finalScope::merge);
        return finalScope;
    }

    private List<Object> generateTickList(MatchGroup group) {
        ArrayList<Object> tickList = new ArrayList<>();
        group.getClasses().forEach(c -> {
            tickList.add(c);

            // TODO: fields

            c.getMethods().forEach(tickList::add);
        });
        return tickList;
    }

    private boolean tick(List<Object> tickList) {
        for (int i = tickList.size() - 1; i >= 0; i--) {
            Object o = tickList.get(i);
            int index = state.get(o) + 1;
            if (o instanceof MatchClass) {
                MatchClass mc = (MatchClass) o;
                if (index >= mc.getMatches().size()) {
                    index = 0;
                    state.put(o, index);
                    continue;
                }
                state.put(o, index);
                return true;
            } else if (o instanceof MatchMethod) {
                MatchClass matchClass = nearestClass(tickList, i);
                ClassNode cls = matchClass.getMatches().get(state.get(matchClass));
                MatchMethod mc = (MatchMethod) o;
                if (index >= mc.getMatches(cls).size()) {
                    index = 0;
                    state.put(o, index);
                    continue;
                }
                state.put(o, index);
                return true;
            }
            // TODO: fields
        }
        return false;
    }

    private MatchClass nearestClass(List<Object> tickList, int index) {
        for (int i = index; i >= 0; i--) {
            if (tickList.get(i) instanceof MatchClass) {
                return (MatchClass) tickList.get(i);
            }
        }
        return null;
    }

    private PatchScope generateScope(MatchGroup group, PatchScope scope) {

        for (MatchClass c : group.getClasses()) {
            ClassWrapper cls = classSet.getClassWrapper(c.getMatches().get(state.get(c)).name);
            if (scope.putClass(cls, c.getName())) {
                return null;
            }

            // TODO: fields

            for (MatchMethod m : c.getMethods()) {
                MethodNode node = m.getMatches(cls.getNode()).get(state.get(m));
                MethodWrapper met = cls.getMethod(node.name, node.desc);
                if (scope.putMethod(met, m.getName(), m.getDesc())) {
                    return null;
                }
            }
        }
        return scope;
    }

    private void addToVisited(Map<MatchClass, MatchGroup> visited, Stack<MatchClass> visitList,
                              MatchClass matchClass, MatchGroup group) {
        if (patchClasses.getClass(matchClass.getName()) == null) return;
        if (!visited.containsKey(matchClass)) {
            visited.put(matchClass, group);
            visitList.push(matchClass);
        } else if (visited.get(matchClass) != group) {
            MatchGroup other = visited.get(matchClass);
            group.merge(other);
            other.getClasses().forEach(c -> visited.put(c, group));
            groups.remove(other);
        }
    }

    public static Type getRootType(Type type) {
        if (type.getSort() == Type.ARRAY) {
            return getRootType(type.getElementType());
        }
        return type;
    }
}