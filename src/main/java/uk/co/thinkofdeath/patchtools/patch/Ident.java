package uk.co.thinkofdeath.patchtools.patch;

public class Ident {

    private boolean weak;
    private String name;

    public Ident(String val) {
        if (val.charAt(0) == '~') {
            weak = true;
            val = val.substring(1);
        }
        name = val;
    }
}
