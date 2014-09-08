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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.logging.LoggableException;
import uk.co.thinkofdeath.patchtools.logging.StateLogger;
import uk.co.thinkofdeath.patchtools.patch.*;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.FieldWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

import java.util.*;

public class MatchGenerator {

    private final ClassSet classSet;
    private final PatchClasses patchClasses;
    private final PatchScope scope;
    private final List<MatchGroup> groups = new ArrayList<>();

    private final TObjectIntMap<Object> state = new TObjectIntHashMap<>();

    private final StateLogger logger = new StateLogger();

    public MatchGenerator(ClassSet classSet, PatchClasses patchClasses, PatchScope scope) {
        this.classSet = classSet;
        this.patchClasses = patchClasses;
        this.scope = scope;

        // To work out the links between the patch classes
        // we start with a the first class and branch out.
        // Then we move onto the next unvisited class since
        // not every class in a patch may be linked. This
        // allows us to split some patches into smaller
        // sets which are quicker to match and apply
        generateGroups();
        groups.forEach(logger::createGroup);

        // As a base every class would be matched to every
        // class in the class set, for patches with more
        // than one class this becomes a large number of
        // tests to work with. To reduce the number of
        // groups only the first class is given every
        // class in the set and then the patch is partially
        // tested (without the checking of class names just
        // types and instructions) to reduce the number of
        // classes, the references from the remaining classes
        // are used to match the others up. With patches that
        // have a good amount of information this normally
        // leaves one or two classes per a patch class
        reduceGroups();

        // Setup the initial state

        groups.stream()
            .flatMap(g -> g.getClasses().stream())
            .forEach(c -> {
                state.put(c, 0);
                c.getMethods().forEach(m -> state.put(m, 0));
                c.getFields().forEach(f -> state.put(f, 0));
            });
    }

    private void reduceGroups() {
        for (MatchGroup group : groups) {

            MatchClass first = group.getFirst();

            // Add every class as a match to the first
            // patch class in the set
            Arrays.stream(classSet.classes(true))
                .map(classSet::getClassWrapper)
                .map(ClassWrapper::getNode)
                .forEach(first::addMatch);

            logger.println("Adding all classes to " + first.getName());

            // Marks whether we made any changes in the last
            // cycle
            boolean doneSomething = true;
            while (doneSomething) {
                doneSomething = false;
                while (true) {
                    Optional<MatchClass> clazz = group.getClasses().stream()
                        .filter(MatchClass::hasUnchecked)
                        .findAny();
                    if (!clazz.isPresent()) {
                        break;
                    }
                    MatchClass cls = clazz.get();
                    doneSomething = true;
                    logger.println("Checking " + cls.getName());
                    logger.indent();

                    ClassNode[] unchecked = cls.getUncheckedClasses();
                    Arrays.stream(unchecked)
                        .forEach(node -> cls.check(logger, classSet, node));

                    logger.unindent();
                }

                while (true) {
                    Optional<MatchField> optionalField = group.getClasses().stream()
                        .flatMap(c -> c.getFields().stream())
                        .filter(MatchField::hasUnchecked)
                        .findAny();
                    if (!optionalField.isPresent()) {
                        break;
                    }
                    MatchField field = optionalField.get();
                    doneSomething = true;
                    logger.println("Checking " + field.getOwner().getName() + "." + field.getName());
                    logger.indent();

                    MatchField.FieldPair[] unchecked = field.getUncheckedMethods();
                    Arrays.stream(unchecked)
                        .forEach(pair -> field.check(logger, classSet, group, pair));

                    logger.unindent();
                }

                while (true) {
                    Optional<MatchMethod> optionalMethod = group.getClasses().stream()
                        .flatMap(c -> c.getMethods().stream())
                        .filter(MatchMethod::hasUnchecked)
                        .findAny();
                    if (!optionalMethod.isPresent()) {
                        break;
                    }
                    MatchMethod method = optionalMethod.get();
                    doneSomething = true;
                    logger.println("Checking " + method.getOwner().getName()
                        + "::" + method.getName() + method.getDesc());
                    logger.indent();

                    MatchMethod.MethodPair[] unchecked = method.getUncheckedMethods();
                    Arrays.stream(unchecked)
                        .forEach(pair -> method.check(logger, classSet, patchClasses, group, pair));

                    logger.unindent();
                }

                if (!doneSomething) {
                    String[] classes = classSet.classes(true);
                    // Check for classes without a match and as a last ditch
                    // method check against the rest of the classes
                    boolean anyUnmatched = group.getClasses().stream()
                        .filter(c -> c.getMatches().isEmpty())
                        .anyMatch(c -> !c.hasChecked(classes.length));

                    if (anyUnmatched) {
                        group.getClasses().stream()
                            .filter(c -> c.getMatches().isEmpty())
                            .filter(c -> !c.hasChecked(classes.length))
                            .forEach(c -> Arrays.stream(classes)
                                .map(classSet::getClassWrapper)
                                .map(ClassWrapper::getNode)
                                .forEach(c::addMatch));
                        doneSomething = true;
                    }
                }
            }

            // Remove incomplete classes
            for (MatchClass cls : group.getClasses()) {
                List<ClassNode> matches = new ArrayList<>(cls.getMatches());
                matches.stream()
                    .filter(clazz -> cls.getMethods().stream().anyMatch(m -> !m.usesNode(clazz))
                        || cls.getFields().stream().anyMatch(f -> !f.usesNode(clazz)))
                    .forEach(clazz -> {
                        cls.removeMatch(clazz);
                        cls.getMethods().forEach(m -> m.removeMatch(clazz));
                        cls.getFields().forEach(f -> f.removeMatch(clazz));
                    });
            }
        }
    }

    private void generateGroups() {
        Map<MatchClass, MatchGroup> visited = new HashMap<>();
        patchClasses.getClasses().stream()
            .filter(p -> p.getMode() != Mode.ADD)
            .forEach(clazz -> {
                MatchClass cls = new MatchClass(clazz.getIdent().getName());
                if (visited.containsKey(cls)) {
                    return;
                }

                MatchGroup group = new MatchGroup(classSet);

                visited.put(cls, group);
                groups.add(group);
                Stack<MatchClass> visitList = new Stack<>();
                visitList.add(cls);
                while (!visitList.isEmpty()) {
                    MatchClass mc = group.getClass(visitList.pop());
                    PatchClass pc = patchClasses.getClass(mc.getName());
                    if (pc == null || pc.getMode() == Mode.ADD) continue;
                    group.add(mc);

                    pc.getExtends().stream()
                        .filter(c -> c.getMode() != Mode.ADD)
                        .forEach(c -> {
                            MatchClass matchClass = group.getClass(new MatchClass(c.getIdent().getName()));
                            mc.setSuperClass(matchClass);
                            addToVisited(visited, visitList, matchClass, group);
                        });
                    pc.getInterfaces().stream()
                        .filter(c -> c.getMode() != Mode.ADD)
                        .forEach(c -> {
                            MatchClass matchClass = group.getClass(new MatchClass(c.getIdent().getName()));
                            mc.addInterface(matchClass);
                            addToVisited(visited, visitList, matchClass, group);
                        });

                    pc.getFields().stream()
                        .filter(f -> f.getMode() != Mode.ADD)
                        .forEach(f -> {
                            Type type = f.getDesc();

                            MatchField field = new MatchField(mc, f.getIdent().getName(), f.getDescRaw());
                            field = mc.addField(field);

                            Type rt = getRootType(type);
                            if (rt.getSort() == Type.OBJECT) {
                                MatchClass argCls = new MatchClass(
                                    new Ident(rt.getInternalName()).getName()
                                );
                                addToVisited(visited, visitList, group.getClass(argCls), group);
                            }
                            field.setType(type);
                        });

                    pc.getMethods().stream()
                        .filter(m -> m.getMode() != Mode.ADD)
                        .forEach(m -> {
                            Type desc = m.getDesc();

                            MatchMethod mTemp = new MatchMethod(mc, m.getIdent().getName(), m.getDescRaw());
                            MatchMethod method = mc.addMethod(mTemp);

                            if (mTemp == method) {
                                for (Type type : desc.getArgumentTypes()) {
                                    Type rt = getRootType(type);
                                    if (rt.getSort() == Type.OBJECT) {
                                        MatchClass argCls = new MatchClass(
                                            new Ident(rt.getInternalName()).getName()
                                        );
                                        addToVisited(visited, visitList, group.getClass(argCls), group);
                                    }
                                    method.addArgument(type);
                                }
                                Type type = desc.getReturnType();
                                Type rt = getRootType(type);
                                if (rt.getSort() == Type.OBJECT) {
                                    MatchClass argCls = new MatchClass(
                                        new Ident(rt.getInternalName()).getName()
                                    );
                                    addToVisited(visited, visitList, group.getClass(argCls), group);
                                }
                                method.setReturn(type);
                            }

                            for (PatchInstruction instruction : m.getInstructions()) {
                                Instruction in = instruction.instruction;
                                if (in.getHandler() == null || instruction.mode == Mode.ADD) continue;
                                in.getHandler().getReferencedClasses(instruction).stream()
                                    .forEach(c -> addToVisited(visited, visitList, c, group));
                                in.getHandler().getReferencedMethods(instruction)
                                    .forEach(me -> {
                                        MatchClass owner = group.getClass(me.getOwner());
                                        owner.addMethod(me);
                                    });
                                in.getHandler().getReferencedFields(instruction)
                                    .forEach(fe -> {
                                        MatchClass owner = group.getClass(fe.getOwner());
                                        owner.addField(fe);
                                    });
                            }
                        });
                }
            });
    }

    public PatchScope apply() {
        List<PatchScope> scopes = new ArrayList<>();
        groupCheck:
        for (MatchGroup group : groups) {

            List<Object> tickList = generateTickList(group);

            long tick = 0;

            do {
                tick++;

                PatchScope testScope = generateScope(group, new PatchScope(scope));
                if (testScope == null) continue;

                if (test(group, testScope)) {
                    scopes.add(testScope);
                    continue groupCheck;
                }
            } while (tick(tickList));
            logger.failedTicks(tick);
            throw new LoggableException(logger);
        }
        PatchScope finalScope = new PatchScope(scope);
        scopes.forEach(finalScope::merge);
        return finalScope;
    }

    private boolean test(MatchGroup group, PatchScope scope) {
        PatchClass[] classes = group.getClasses().stream()
            .map(c -> patchClasses.getClass(c.getName()))
            .filter(c -> c != null)
            .toArray(PatchClass[]::new);
        // Slightly faster to do it this way since the instruction checking is the heaviest
        return Arrays.stream(classes).allMatch(c -> c.checkAttributes(logger, scope, classSet))
            && Arrays.stream(classes).allMatch(c -> c.checkFields(logger, scope, classSet))
            && Arrays.stream(classes).allMatch(c -> c.checkMethods(logger, scope, classSet))
            && Arrays.stream(classes).allMatch(c -> c.checkMethodsInstructions(logger, scope, classSet));
    }

    private List<Object> generateTickList(MatchGroup group) {
        ArrayList<Object> tickList = new ArrayList<>();
        group.getClasses().forEach(c -> {
            tickList.add(c);

            c.getFields().forEach(tickList::add);
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
            } else if (o instanceof MatchField) {
                MatchClass matchClass = nearestClass(tickList, i);
                ClassNode cls = matchClass.getMatches().get(state.get(matchClass));
                MatchField mc = (MatchField) o;
                if (index >= mc.getMatches(cls).size()) {
                    index = 0;
                    state.put(o, index);
                    continue;
                }
                state.put(o, index);
                return true;
            }
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

            if (c.getMatches().isEmpty()) {
                throw new LoggableException(logger);
            }

            ClassWrapper cls = classSet.getClassWrapper(c.getMatches().get(state.get(c)).name);
            if (scope.putClass(cls, c.getName())) {
                return null;
            }

            for (MatchField f : c.getFields()) {
                List<FieldNode> matches = f.getMatches(cls.getNode());
                if (matches.isEmpty()) {
                    throw new LoggableException(logger);
                }
                FieldNode node = matches.get(state.get(f));
                FieldWrapper met = cls.getField(node.name, node.desc);
                if (scope.putField(met, f.getName(), f.getDesc())) {
                    return null;
                }
            }

            for (MatchMethod m : c.getMethods()) {
                List<MethodNode> matches = m.getMatches(cls.getNode());
                if (matches.isEmpty()) {
                    throw new LoggableException(logger);
                }
                MethodNode node = matches.get(state.get(m));
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