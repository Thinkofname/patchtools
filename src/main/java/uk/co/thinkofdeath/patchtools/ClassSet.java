package uk.co.thinkofdeath.patchtools;

import java.io.InputStream;
import java.util.Iterator;

import static uk.co.thinkofdeath.patchtools.temp.NYI.nyi;

public class ClassSet implements Iterable<String> {

    public ClassSet() {

    }

    public void add(byte[] clazz) {
        nyi();
    }

    public void add(InputStream clazz) {
        nyi();
    }

    public byte[] remove(String name) {
        return nyi();
    }

    public byte[] getClass(String name) {
        return nyi();
    }

    public String[] classes() {
        return nyi();
    }

    @Override
    public Iterator<String> iterator() {
        return nyi();
    }
}
