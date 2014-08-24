package uk.co.thinkofdeath.patchtools;

import com.google.common.io.ByteStreams;
import org.junit.Test;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.io.IOException;
import java.io.InputStream;

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

    public static byte[] getClass(String name) {
        try (InputStream inputStream = PatchTest.class.getResourceAsStream("/" + name + ".class")) {
            return ByteStreams.toByteArray(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
