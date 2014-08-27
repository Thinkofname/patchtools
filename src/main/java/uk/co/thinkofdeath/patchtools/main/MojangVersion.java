package uk.co.thinkofdeath.patchtools.main;

public class MojangVersion {
    public String id;
    public String assets;
    public int minimumLauncherVersion;

    public Library[] libraries;

    public static class Library {
        public String name;
        public Natives natives;
    }

    private static class Natives {
    }
}
