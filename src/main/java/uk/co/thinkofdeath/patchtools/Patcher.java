package uk.co.thinkofdeath.patchtools;

import uk.co.thinkofdeath.patchtools.matching.MatchGenerator;
import uk.co.thinkofdeath.patchtools.patch.PatchClasses;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Patcher {

    private final ClassSet classSet;

    public Patcher(ClassSet classSet) {
        this.classSet = classSet;
        classSet.simplify();
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
            apply(patchClasses, patchScope);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void apply(PatchClasses patchClasses, PatchScope patchScope) {
        apply(patchClasses, patchScope, true);
    }

    public void apply(PatchClasses patchClasses, PatchScope patchScope, boolean parallel) {
        MatchGenerator generator = new MatchGenerator(classSet, patchClasses, patchScope);
        PatchScope foundScope = generator.apply(scope -> {
            try {
                patchClasses.getClasses().forEach(c -> c.check(scope, classSet));
                return true;
            } catch (PatchVerifyException e) {
                return false;
            }
        }, parallel);
        patchClasses.getClasses().forEach(c -> c.apply(foundScope, classSet));
    }

    public ClassSet getClasses() {
        return classSet;
    }
}
