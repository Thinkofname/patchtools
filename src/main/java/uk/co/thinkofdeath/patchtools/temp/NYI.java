package uk.co.thinkofdeath.patchtools.temp;

public class NYI {

    @Deprecated
    public static <T> T nyi() throws Error {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StackTraceElement caller = stack[2];
        throw new NotYetImplemented(caller);
    }


    private static class NotYetImplemented extends Error {

        private NotYetImplemented(StackTraceElement caller) {
            super(toMessage(caller));
            StackTraceElement[] stack = getStackTrace();
            StackTraceElement[] cleanedStack = new StackTraceElement[stack.length - 1];
            System.arraycopy(stack, 1, cleanedStack, 0, cleanedStack.length);
            setStackTrace(cleanedStack);
        }

        private static String toMessage(StackTraceElement caller) {
            return "Method "
                    + caller.getMethodName()
                    + " is not implemented in class "
                    + caller.getClassName();
        }

        @Override
        public String toString() {
            return "NotYetImplemented: " + getMessage();
        }
    }
}
