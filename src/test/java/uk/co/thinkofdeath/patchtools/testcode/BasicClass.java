package uk.co.thinkofdeath.patchtools.testcode;

public class BasicClass {

    public String str;

    public BasicClass() {

    }

    public BasicClass(String str) {
        this.str = str;
    }

    public String hello() {
        return "Hello bob";
    }

    public static BasicClass create() {
        return new BasicClass("Testing");
    }

    @Override
    public String toString() {
        return str;
    }
}
