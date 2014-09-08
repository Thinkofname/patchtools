package uk.co.thinkofdeath.patchtools.logging;

import uk.co.thinkofdeath.patchtools.matching.MatchGroup;

import java.util.LinkedHashMap;

public class StateLogger {

    private LinkedHashMap<MatchGroup, LoggedGroup> groups = new LinkedHashMap<>();

    public StateLogger() {

    }

    public void createGroup(MatchGroup group) {
        groups.put(group, new LoggedGroup(group));
    }
}
