package uk.co.thinkofdeath.patchtools.instruction.instructions;

public class Utils {
    public static Object parseConstant(String cst) {
        if (cst.startsWith("\"") && cst.endsWith("\"")) {
            return cst.substring(1, cst.length() - 1);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static void printConstant(StringBuilder patch, Object value) {
        if (value instanceof String) {
            patch.append('"').append(value).append('"');
        } else {
            //throw new UnsupportedOperationException("Unsupported " + value.getClass());
            patch.append("unsupported: ").append(value);
        }
    }
}
