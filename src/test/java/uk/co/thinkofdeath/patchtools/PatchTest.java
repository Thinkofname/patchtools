package uk.co.thinkofdeath.patchtools;

import com.google.common.io.ByteStreams;
import org.junit.Test;
import uk.co.thinkofdeath.patchtools.testcode.InterfaceTestInterface;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

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

        ClassSetLoader loader = new ClassSetLoader(classSet);
        Class<?> res = loader.loadClass("uk.co.thinkofdeath.patchtools.testcode.BasicClass");

        assertEquals("Hello jim", res.getMethod("hello").invoke(
                res.newInstance()
        ));
        assertEquals("Hello world", res.getMethod("addedMethod").invoke(
                res.newInstance()
        ));
        assertEquals("Cake", res.getMethod("create").invoke(null).toString());
    }

    @Test
    public void invoke() throws Exception {
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        classSet.add(
                getClass("uk/co/thinkofdeath/patchtools/testcode/InvokeTest")
        );

        Patcher patcher = new Patcher(classSet);

        patcher.apply(
                getClass().getResourceAsStream("/invoke.jpatch")
        );

        ClassSetLoader loader = new ClassSetLoader(classSet);
        Class<?> res = loader.loadClass("uk.co.thinkofdeath.patchtools.testcode.InvokeTest");

        assertEquals("Bye jim", res.getMethod("test").invoke(null));
    }

    @Test
    public void inherit() throws Exception {
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        classSet.add(getClass("uk/co/thinkofdeath/patchtools/testcode/InheritTestA"));
        classSet.add(getClass("uk/co/thinkofdeath/patchtools/testcode/InheritTestB"));

        Patcher patcher = new Patcher(classSet);

        patcher.apply(
                getClass().getResourceAsStream("/inherit.jpatch")
        );

        ClassSetLoader loader = new ClassSetLoader(classSet);
        Class<?> res = loader.loadClass("uk.co.thinkofdeath.patchtools.testcode.InheritTestB");

        assertEquals("abc", res.getMethod("method").invoke(res.newInstance()));
    }

    @Test
    public void interTest() throws Exception {
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        classSet.add(getClass("uk/co/thinkofdeath/patchtools/testcode/InterfaceTestClass"));

        Patcher patcher = new Patcher(classSet);

        patcher.apply(
                getClass().getResourceAsStream("/interface.jpatch")
        );

        ClassSetLoader loader = new ClassSetLoader(classSet);
        Class<?> res = loader.loadClass("uk.co.thinkofdeath.patchtools.testcode.InterfaceTestClass");

        Object o = res.newInstance();
        assertTrue("InterfaceTestClass does not implement InterfaceTestInterface", o instanceof InterfaceTestInterface);

        InterfaceTestInterface testInterface = (InterfaceTestInterface) o;

        assertEquals("Bob", testInterface.getName());
        assertEquals("Hello world", testInterface.getMessage());
    }

    @Test
    public void fieldTest() throws Exception {
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        classSet.add(getClass("uk/co/thinkofdeath/patchtools/testcode/BasicField"));

        Patcher patcher = new Patcher(classSet);

        patcher.apply(
                getClass().getResourceAsStream("/field.jpatch")
        );

        ClassSetLoader loader = new ClassSetLoader(classSet);
        Class<?> res = loader.loadClass("uk.co.thinkofdeath.patchtools.testcode.BasicField");

        try {
            res.getField("test");
            fail();
        } catch (NoSuchFieldException ignored) {

        }
        try {
            res.getField("addedField");
        } catch (NoSuchFieldException ignored) {
            fail();
        }
    }

    @Test
    public void complex() throws Exception {
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        classSet.add(getClass("uk/co/thinkofdeath/patchtools/testcode/ComplexInstruction"));

        Patcher patcher = new Patcher(classSet);

        patcher.apply(
                getClass().getResourceAsStream("/complex.jpatch")
        );

        ClassSetLoader loader = new ClassSetLoader(classSet);
        Class<?> res = loader.loadClass("uk.co.thinkofdeath.patchtools.testcode.ComplexInstruction");

        assertEquals("HelloABCTesting", res.getMethod("message").invoke(null));
    }

    public static byte[] getClass(String name) {
        try (InputStream inputStream = PatchTest.class.getResourceAsStream("/" + name + ".class")) {
            return ByteStreams.toByteArray(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
