package uk.co.thinkofdeath.patchtools.testcode;

public class LoopTestClass {

    public static void testMethod() {
        LoopTestClass test = new LoopTestClass();
        int[] hello = new int[5];
    }

    public static int testTwo(int val) {
        if (val == 0) {
            val++;
            val *= 5;
            return val;
        } else if (val > 5) {
            return val / 5;
        }
        return val;
    }

    public static int testThree(int val) {
        for (int i = 0; i < val; i++) {
            val *= 2;
        }
        return val;
    }

    public static int switchTest(int val) {
        switch (val) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            default:
                return -1;
        }
    }
}
