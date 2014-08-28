package uk.co.thinkofdeath.patchtools.matching;

import com.google.common.collect.Maps;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.patch.*;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.FieldWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class MatchGenerator {


    private ExecutorService pool = new ThreadPoolExecutor(8, 8,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(128) {
                @Override
                public boolean offer(Runnable o) {
                    try {
                        put(o);
                        return true;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            });

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

                    c.getFields().stream()
                            .filter(f -> f.getMode() != Mode.ADD)
                            .forEach(tickList::add);
                });
    }

    public PatchScope apply(Predicate<PatchScope> test, boolean parallel) {
        if (parallel) {
            return applyParallel(test);
        }
        return applySingle(test);
    }

    private PatchScope result;
    private final Object resultLock = new Object();

    private PatchScope applyParallel(Predicate<PatchScope> test) {
        result = null;
        while (true) {
            if (result != null) {
                System.out.println();
                return result;
            }

            PatchScope newScope = scope.duplicate();
            HashMap<Object, Integer> capturedState = Maps.newHashMap(state);

            pool.execute(() -> {
                if (result != null) return;
                try {
                    cycleScope(capturedState, newScope);
                } catch (InvalidMatch e) {
                    return;
                }
                if (test.test(newScope)) {
                    synchronized (resultLock) {
                        if (result == null) {
                            result = newScope;
                            resultLock.notifyAll();
                        }
                    }
                }
            });

            if (tick()) {
                continue;
            }
            break;
        }
        System.out.println();
        System.out.println("Waiting");
        synchronized (resultLock) {
            if (result != null) {
                return result;
            }
            try {
                resultLock.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            System.out.println("Result: " + result);
            return result;
        }
    }

    private PatchScope applySingle(Predicate<PatchScope> test) {
        while (true) {
            PatchScope newScope = scope.duplicate();
            try {
                cycleScope(state, newScope);
            } catch (InvalidMatch e) {
                if (tick()) {
                    continue;
                }
                break;
            }

            if (test.test(newScope)) {
                return newScope;
            }

            if (tick()) {
                continue;
            }
            break;
        }
        return null;
    }

    private void cycleScope(Map<Object, Integer> state, PatchScope newScope) {
        tickList.forEach(v -> {
            int index = getState(state, v);
            if (v instanceof PatchClass) {
                PatchClass pc = (PatchClass) v;
                String[] classes = classSet.classes(true);
                if (classes.length <= index) {
                    throw new InvalidMatch();
                }
                if (newScope.hasClass(classSet.getClassWrapper(classes[index]))) {
                    throw new InvalidMatch();
                }
                newScope.putClass(classSet.getClassWrapper(classes[index]), pc.getIdent().getName());
            } else if (v instanceof PatchMethod) {
                PatchMethod pm = (PatchMethod) v;
                ClassWrapper cls = newScope.getClass(pm.getOwner().getIdent().getName());
                MethodWrapper[] methods = cls.getMethods(true);
                if (methods.length <= index) {
                    throw new InvalidMatch();
                }
                if (newScope.hasMethod(methods[index])) {
                    throw new InvalidMatch();
                }
                newScope.putMethod(methods[index], pm.getIdent().getName(), pm.getDesc().getDescriptor());
            } else if (v instanceof PatchField) {
                PatchField pf = (PatchField) v;
                ClassWrapper cls = newScope.getClass(pf.getOwner().getIdent().getName());
                FieldWrapper[] fields = cls.getFields(true);
                if (fields.length <= index) {
                    throw new InvalidMatch();
                }
                if (newScope.hasField(fields[index])) {
                    throw new InvalidMatch();
                }
                newScope.putField(fields[index], pf.getIdent().getName(), pf.getDesc().getDescriptor());
            }
        });
    }

    private boolean tick() {
        for (int i = tickList.size() - 1; i >= 0; i--) {
            Object val = tickList.get(i);
            int index = getState(state, val);
            index++;
            if (val instanceof PatchClass) {
                if (i == 0) {
                    System.out.print(i + " : " + index + "/" + classSet.classes(true).length + "\r");
                }
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
                ClassWrapper cls = classSet.getClassWrapper(classSet.classes(true)[getState(state, owner)]);
                if (index >= cls.getMethods(true).length) {
                    index = 0;
                    state.put(val, index);
                    if (i == 0) {
                        break;
                    }
                    continue;
                }
            } else if (val instanceof PatchField) {
                PatchClass owner = nearestClass(i);
                ClassWrapper cls = classSet.getClassWrapper(classSet.classes(true)[getState(state, owner)]);
                if (index >= cls.getFields(true).length) {
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

    private int getState(Map<Object, Integer> state, Object o) {
        if (!state.containsKey(o)) {
            state.put(o, 0);
        }
        return state.get(o);
    }

    public void close() {
        pool.shutdownNow();
    }
}
