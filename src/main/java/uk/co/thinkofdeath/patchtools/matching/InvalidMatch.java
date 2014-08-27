package uk.co.thinkofdeath.patchtools.matching;

public class InvalidMatch extends RuntimeException {

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
