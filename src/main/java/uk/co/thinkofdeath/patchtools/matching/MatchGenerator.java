package uk.co.thinkofdeath.patchtools.matching;

import uk.co.thinkofdeath.patchtools.ClassSet;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.patch.Mode;
import uk.co.thinkofdeath.patchtools.patch.PatchClass;
import uk.co.thinkofdeath.patchtools.patch.PatchClasses;
import uk.co.thinkofdeath.patchtools.patch.PatchMethod;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class MatchGenerator {

    private final ClassSet classSet;
    private final PatchClasses patchClasses;
    private final PatchScope scope;

    private final Map<Object, Integer> state = new HashMap<>();

    private final List<Object> tickList = new ArrayList<>();

    public MatchGenerator(ClassSet classSet, PatchClasses patchClasses, PatchScope scope) {
        this.classSet = classSet;
        this.patchClasses = patchClasses;
        this.scope = scope;

        patchClasses.getClasses()
                .stream()
                .filter(c -> c.getMode() != Mode.ADD)
                .forEach(c -> {
                    tickList.add(c);

                    c.getMethods().stream()
                            .filter(m -> m.getMode() != Mode.ADD)
                            .forEach(tickList::add);
                });
    }

    public PatchScope apply(Predicate<PatchScope> test) {
        main:
        while (true) {
            PatchScope newScope = scope.duplicate();
            try {
                tickList.forEach(v -> {
                    int index = getState(v);
                    if (v instanceof PatchClass) {
                        PatchClass pc = (PatchClass) v;
                        String[] classes = classSet.classes();
                        if (classes.length <= index) {
                            throw new IllegalStateException();
                        }
                        if (newScope.hasClass(classSet.getClassWrapper(classes[index]))) {
                            throw new IllegalStateException();
                        }
                        newScope.putClass(classSet.getClassWrapper(classes[index]), pc.getIdent().getName());
                    } else if (v instanceof PatchMethod) {
                        PatchMethod pm = (PatchMethod) v;
                        ClassWrapper cls = newScope.getClass(pm.getOwner().getIdent().getName());
                        List<MethodWrapper> methods = cls.getMethods();
                        if (methods.size() <= index) {
                            throw new IllegalStateException();
                        }
                        if (newScope.hasMethod(methods.get(index))) {
                            throw new IllegalStateException();
                        }
                        newScope.putMethod(methods.get(index), pm.getIdent().getName());
                    }
                });
            } catch (IllegalStateException e) {
                tick();
                continue;
            }

            if (test.test(newScope)) {
                return newScope;
            }

            if (tick()) {
                continue main;
            }
            return null;
        }
    }

    private boolean tick() {
        for (int i = tickList.size() - 1; i >= 0; i--) {
            Object val = tickList.get(i);
            int index = state.get(val);
            index++;
            if (val instanceof PatchClass) {
                if (index >= classSet.classes().length) {
                    index = 0;
                    state.put(val, index);
                    continue;
                }
            } else if (val instanceof PatchMethod) {
                PatchMethod pm = (PatchMethod) val;
                PatchClass owner = nearestClass(i);
                ClassWrapper cls = classSet.getClassWrapper(classSet.classes()[state.get(owner)]);
                if (index >= cls.getMethods().size()) {
                    index = 0;
                    state.put(val, index);
                    continue;
                }
            }
            state.put(val, index);
            return true;
        }
        return false;
    }

    private PatchClass nearestClass(int i) {
        for (; i >= 0; i--) {
            Object val = tickList.get(i);
            if (val instanceof PatchClass) {
                return (PatchClass) val;
            }
        }
        return null;
    }

    private int getState(Object o) {
        if (!state.containsKey(o)) {
            state.put(o, 0);
        }
        return state.get(o);
    }
}
