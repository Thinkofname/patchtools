package uk.co.thinkofdeath.patchtools;

import com.google.common.io.ByteStreams;
import org.junit.Test;
import sun.misc.Unsafe;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PatchTest {

    @Test
    public void classSetBasic() throws IOException {
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        classSet.add(
                getClass("uk/co/thinkofdeath/patchtools/testcode/BasicClass")
        );
        classSet.add(
                getClass("uk/co/thinkofdeath/patchtools/testcode/Basic2Class")
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
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        classSet.add(
                getClass("uk/co/thinkofdeath/patchtools/testcode/BasicClass")
        );
        classSet.add(
                getClass("uk/co/thinkofdeath/patchtools/testcode/Basic2Class")
        );

        Patcher patcher = new Patcher(classSet);

        patcher.apply(
                getClass().getResourceAsStream("/basic.jpatch")
        );

        byte[] clz = classSet.getClass("uk/co/thinkofdeath/patchtools/testcode/BasicClass");
        Class<?> res = getUnsafe().defineClass("uk/co/thinkofdeath/patchtools/testcode/BasicClass", clz, 0, clz.length, getClass().getClassLoader(), null);

        assertEquals("Hello jim", res.getMethod("hello").invoke(
                res.newInstance()
        ));
        assertEquals("Hello world", res.getMethod("addedMethod").invoke(
                res.newInstance()
        ));
        assertEquals("Cake", res.getMethod("create").invoke(
                null
        ).toString());
    }

    public static byte[] getClass(String name) {
        try (InputStream inputStream = PatchTest.class.getResourceAsStream("/" + name + ".class")) {
            return ByteStreams.toByteArray(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static Unsafe unsafe;

    public static Unsafe getUnsafe() {
        if (unsafe == null) {
            try {
                Constructor<Unsafe> unsafeConstructor;
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
