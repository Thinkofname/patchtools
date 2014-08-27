package uk.co.thinkofdeath.patchtools;

import org.junit.Assert;
import org.junit.Test;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

public class AssembleTest {

    @Test
    public void assemble() throws Exception {
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        Patcher patcher = new Patcher(classSet);
        patcher.apply(getClass().getResourceAsStream("/writing.jpatch"));

        ClassSetLoader loader = new ClassSetLoader(classSet);
        Class<?> res = loader.loadClass("think.TestClass");

        int val = (int) res.getMethod("doMagic").invoke(null);
        Assert.assertEquals(5, val);
    }
}
