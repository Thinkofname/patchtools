package uk.co.thinkofdeath.patchtools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static uk.co.thinkofdeath.patchtools.temp.NYI.nyi;

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
        nyi();
    }

    public ClassSet getClasses() {
        return classSet;
    }
}
