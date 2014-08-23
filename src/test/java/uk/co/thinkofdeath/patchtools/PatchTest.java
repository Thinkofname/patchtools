package uk.co.thinkofdeath.patchtools;

import org.junit.Test;
import sun.invoke.anon.AnonymousClassLoader;
import sun.misc.Unsafe;
import uk.co.thinkofdeath.patchtools.testcode.Basic2Class;
import uk.co.thinkofdeath.patchtools.testcode.BasicClass;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PatchTest {

    @Test
    public void classSetBasic() throws IOException {
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        classSet.add(
                AnonymousClassLoader.readClassFile(BasicClass.class)
        );
        classSet.add(
                AnonymousClassLoader.readClassFile(Basic2Class.class)
        );

        for (String clazz : classSet) {
            byte[] data = classSet.getClass(clazz);
            if (data == null) {
                fail();
            }
        }
    }

    @Test
    public void basicPatch() throws Exception {
        AnonymousClassLoader cl = AnonymousClassLoader.make(
                getUnsafe(),
                getClass()
        );
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        classSet.add(
                AnonymousClassLoader.readClassFile(BasicClass.class)
        );
        classSet.add(
                AnonymousClassLoader.readClassFile(Basic2Class.class)
        );

        Patcher patcher = new Patcher(classSet);

        patcher.apply(
                getClass().getResourceAsStream("/basic.jpatch")
        );

        byte[] clz = classSet.getClass("uk/co/thinkofdeath/patchtools/testcode/BasicClass");
        Class<?> res = cl.loadClass(clz);

        String result = (String) res.getMethod("hello").invoke(
                res.newInstance()
        );

        assertEquals("Hello jim", result);
    }


    private static Unsafe unsafe;

    public static Unsafe getUnsafe() {
        if (unsafe == null) {
            try {
                Constructor<Unsafe> unsafeConstructor = null;
                unsafeConstructor = Unsafe.class.getDeclaredConstructor();
                unsafeConstructor.setAccessible(true);
                unsafe = unsafeConstructor.newInstance();
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return unsafe;
    }
}
