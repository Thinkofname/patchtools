package uk.co.thinkofdeath.patchtools.testcode;

public class DisassembleClass {

    public static void test() {
        String test = "Hello world";
        int i = 0;
        i = i + 5;
        i += 3;
        i += 50;
        i += 12000;
        i += 0xFFFFF;
        System.out.println(i);
    }
}
