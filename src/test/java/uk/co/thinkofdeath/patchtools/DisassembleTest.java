package uk.co.thinkofdeath.patchtools;

import com.google.common.io.ByteStreams;
import org.junit.Test;
import uk.co.thinkofdeath.patchtools.disassemble.Disassembler;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.io.IOException;
import java.io.InputStream;

public class DisassembleTest {

    @Test
    public void test1() {
        ClassSet classSet = new ClassSet(new ClassPathWrapper());
        classSet.add(
                getClass("uk/co/thinkofdeath/patchtools/testcode/DisassembleClass")
        );

        Disassembler disassembler = new Disassembler(classSet);

        String patch = disassembler.disassemble("uk/co/thinkofdeath/patchtools/testcode/DisassembleClass");
        System.out.println("Done:");
        System.out.println(patch);
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
