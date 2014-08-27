package uk.co.thinkofdeath.patchtools.bench;

import com.google.caliper.Benchmark;
import com.google.common.io.ByteStreams;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.Patcher;
import uk.co.thinkofdeath.patchtools.patch.PatchClasses;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class MatchingTest {


    private ClassSet classSet = new ClassSet(new ClassPathWrapper());
    private PatchClasses patchClasses;
    private Patcher patcher = new Patcher(classSet);

    public MatchingTest() {
        classSet.add(getClass("uk/co/thinkofdeath/patchtools/bench/BasicField"));

        try {
            patchClasses = new PatchClasses(
                    new BufferedReader(
                            new InputStreamReader(
                                    getClass().getResourceAsStream("/field.jpatch"), StandardCharsets.UTF_8
                            )
                    )
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Benchmark
    public void parallel() throws Exception {
        patcher.apply(patchClasses, new PatchScope(), true);
    }

    @Benchmark
    public void single() throws Exception {
        patcher.apply(patchClasses, new PatchScope(), false);
    }

    public static byte[] getClass(String name) {
        try (InputStream inputStream = MatchingTest.class.getResourceAsStream("/" + name + ".class")) {
            return ByteStreams.toByteArray(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
