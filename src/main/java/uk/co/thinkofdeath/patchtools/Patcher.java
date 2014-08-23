package uk.co.thinkofdeath.patchtools;

import uk.co.thinkofdeath.patchtools.matching.MatchGenerator;
import uk.co.thinkofdeath.patchtools.patch.PatchClasses;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Patcher {

    private final ClassSet classSet;

    public Patcher(ClassSet classSet) {
        this.classSet = classSet;
    }

    public void apply(InputStream inputStream) {
        apply(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    public void apply(Reader reader) {
        apply(new BufferedReader(reader));
    }

    public void apply(BufferedReader reader) {
        apply(reader, new PatchScope());
    }

    public void apply(BufferedReader reader, PatchScope patchScope) {
        PatchClasses patchClasses;
        try (BufferedReader ignored = reader) {
            patchClasses = new PatchClasses(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        MatchGenerator generator = new MatchGenerator(classSet, patchClasses, patchScope);
        PatchScope foundScope = generator.apply(scope -> {
            System.out.println("Trying: " + scope);
            try {
                patchClasses.getClasses().forEach(c -> c.check(scope, classSet));
                return true;
            } catch (PatchVerifyException e) {
                e.printStackTrace();
                return false;
            }
        });
        System.out.println("Found: " + foundScope);
        patchClasses.getClasses().forEach(c -> c.apply(foundScope, classSet));
    }

    public ClassSet getClasses() {
        return classSet;
    }
}
