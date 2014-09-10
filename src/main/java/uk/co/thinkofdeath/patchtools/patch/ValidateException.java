package uk.co.thinkofdeath.patchtools.patch;

public class ValidateException extends RuntimeException {

    private int lineNumber = -1;

    public ValidateException() {
    }

    public ValidateException(String message) {
        super(message);
    }

    public ValidateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidateException(Throwable cause) {
        super(cause);
    }

    public ValidateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ValidateException setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
        return this;
    }

    @Override
    public String getMessage() {
        return (lineNumber == -1 ? "??" : lineNumber) + ":" + super.getMessage();
    }
}
