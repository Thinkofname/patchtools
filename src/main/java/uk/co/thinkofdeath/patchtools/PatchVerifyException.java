package uk.co.thinkofdeath.patchtools;

public class PatchVerifyException extends RuntimeException {

    public PatchVerifyException() {
    }

    public PatchVerifyException(String message) {
        super(message);
    }

    public PatchVerifyException(String message, Throwable cause) {
        super(message, cause);
    }

    public PatchVerifyException(Throwable cause) {
        super(cause);
    }

    public PatchVerifyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
