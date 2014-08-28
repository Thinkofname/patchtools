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

    public PatchScope apply(InputStream inputStream) {
        return apply(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    public PatchScope apply(Reader reader) {
        return apply(new BufferedReader(reader));
    }

    public PatchScope apply(BufferedReader reader) {
        return apply(reader, new PatchScope());
    }

    public PatchScope apply(BufferedReader reader, PatchScope patchScope) {
        PatchClasses patchClasses;
        try (BufferedReader ignored = reader) {
            patchClasses = new PatchClasses(reader);
            return apply(patchClasses, patchScope);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public PatchScope apply(PatchClasses patchClasses, PatchScope patchScope) {
        return apply(patchClasses, patchScope, true);
    }

    public PatchScope apply(PatchClasses patchClasses, PatchScope patchScope, boolean parallel) {
        MatchGenerator generator = new MatchGenerator(classSet, patchClasses, patchScope);
        PatchScope foundScope = generator.apply(scope -> {
            try {
                patchClasses.getClasses().forEach(c -> c.checkAttributes(scope, classSet));
                patchClasses.getClasses().forEach(c -> c.checkFields(scope, classSet));
                patchClasses.getClasses().forEach(c -> c.checkMethods(scope, classSet));
                patchClasses.getClasses().forEach(c -> c.checkMethodsInstructions(scope, classSet));
                return true;
            } catch (PatchVerifyException e) {
                return false;
            }
        }, parallel);
        generator.close();
        patchClasses.getClasses().forEach(c -> c.apply(foundScope, classSet));
        return foundScope;
    }

    public ClassSet getClasses() {
        return classSet;
    }
}
