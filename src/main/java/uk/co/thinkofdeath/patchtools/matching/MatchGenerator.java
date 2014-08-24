package uk.co.thinkofdeath.patchtools.matching;

import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.patch.Mode;
import uk.co.thinkofdeath.patchtools.patch.PatchClass;
import uk.co.thinkofdeath.patchtools.patch.PatchClasses;
import uk.co.thinkofdeath.patchtools.patch.PatchMethod;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
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
        while (true) {
            PatchScope newScope = scope.duplicate();
            try {
                tickList.forEach(v -> {
                    int index = getState(v);
                    if (v instanceof PatchClass) {
                        PatchClass pc = (PatchClass) v;
                        String[] classes = classSet.classes(true);
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
                        MethodWrapper[] methods = cls.getMethods(true);
                        if (methods.length <= index) {
                            throw new IllegalStateException();
                        }
                        if (newScope.hasMethod(methods[index])) {
                            throw new IllegalStateException();
                        }
                        newScope.putMethod(methods[index], pm.getIdent().getName());
                    }
                });
            } catch (IllegalStateException e) {
                if (tick()) {
                    continue;
                }
                return null;
            }

            if (test.test(newScope)) {
                return newScope;
            }

            if (tick()) {
                continue;
            }
            return null;
        }
    }

    private boolean tick() {
        for (int i = tickList.size() - 1; i >= 0; i--) {
            Object val = tickList.get(i);
            int index = getState(val);
            index++;
            if (val instanceof PatchClass) {
                if (index >= classSet.classes(true).length) {
                    index = 0;
                    state.put(val, index);
                    if (i == 0) {
                        break;
                    }
                    continue;
                }
            } else if (val instanceof PatchMethod) {
                PatchClass owner = nearestClass(i);
                ClassWrapper cls = classSet.getClassWrapper(classSet.classes(true)[getState(owner)]);
                if (index >= cls.getMethods(true).length) {
                    index = 0;
                    state.put(val, index);
                    if (i == 0) {
                        break;
                    }
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
