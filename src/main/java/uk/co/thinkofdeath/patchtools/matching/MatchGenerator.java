package uk.co.thinkofdeath.patchtools.matching;

import com.google.common.math.LongMath;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.patch.*;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.FieldWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class MatchGenerator {


    private ExecutorService pool = new ThreadPoolExecutor(5, 5,
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
    private long completedTests = 0;

    private PatchScope applyParallel(Predicate<PatchScope> test) {
        result = null;
        long id = 0;
        long max = computeMax();
        System.out.println("Max: " + max);
        while (true) {
            if (result != null) {
                System.out.println();
                return result;
            }

            PatchScope newScope = new PatchScope(scope);

            final long capturedId = id;
            pool.execute(() -> {
                try {
                    if (result != null) return;
                    try {
                        cycleScope(capturedId, newScope);
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
                } finally {
                    completedTests++;
                }
            });

            if (id % 1000 == 0) {
                System.out.printf("Current: Setup:%d  Completed: %d/%d\r", id, completedTests, max);
            }

            id++;
            if (id >= max) {
                break;
            }
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
        long id = 0;
        long max = computeMax();
        System.out.println("Max: " + max);
        while (true) {
            PatchScope newScope = new PatchScope(scope);
            try {
                cycleScope(id, newScope);
            } catch (InvalidMatch e) {
                id++;
                if (id >= max) {
                    break;
                }
                continue;
            }

            if (test.test(newScope)) {
                return newScope;
            }

            id++;
            if (id >= max) {
                break;
            }
        }
        return null;
    }

    private long computeMax() {
        long max = LongMath.pow(classSet.classes(true).length, (int) tickList.stream().filter(v -> v instanceof PatchClass).count());
        long patchMethods = tickList.stream().filter(v -> v instanceof PatchMethod).count();
        long methods = Arrays.stream(classSet.classes(true))
                .map(classSet::getClassWrapper)
                .flatMap(c -> c.getMethods().stream())
                .count();
        max += LongMath.pow(methods, (int) patchMethods);
        long patchFields = tickList.stream().filter(v -> v instanceof PatchField).count();
        long fields = Arrays.stream(classSet.classes(true))
                .map(classSet::getClassWrapper)
                .flatMap(c -> c.getFields().stream())
                .count();
        max += LongMath.pow(fields, (int) patchFields);
        return max;
    }

    private void cycleScope(long id, PatchScope newScope) {
        for (Object v : tickList) {
            if (v instanceof PatchClass) {
                String[] classes = classSet.classes(true);
                if (classes.length == 0) throw new InvalidMatch();
                int index = (int) (id % classes.length);
                id /= classes.length;
                PatchClass pc = (PatchClass) v;
                if (newScope.putClass(classSet.getClassWrapper(classes[index]), pc.getIdent().getName())) {
                    throw new InvalidMatch();
                }
            } else if (v instanceof PatchMethod) {
                PatchMethod pm = (PatchMethod) v;
                ClassWrapper cls = newScope.getClass(pm.getOwner().getIdent().getName());
                MethodWrapper[] methods = cls.getMethods(true);
                if (methods.length == 0) throw new InvalidMatch();
                int index = (int) (id % methods.length);
                id /= methods.length;
                if (newScope.putMethod(methods[index], pm.getIdent().getName(), pm.getDesc().getDescriptor())) {
                    throw new InvalidMatch();
                }
            } else if (v instanceof PatchField) {
                PatchField pf = (PatchField) v;
                ClassWrapper cls = newScope.getClass(pf.getOwner().getIdent().getName());
                FieldWrapper[] fields = cls.getFields(true);
                if (fields.length == 0) throw new InvalidMatch();
                int index = (int) (id % fields.length);
                id /= fields.length;
                if (newScope.putField(fields[index], pf.getIdent().getName(), pf.getDesc().getDescriptor())) {
                    throw new InvalidMatch();
                }
            }
        }
    }

    public void close() {
        pool.shutdownNow();
    }
}
