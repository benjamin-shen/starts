package edu.illinois.starts.util;

import java.util.Map;
import java.util.Set;

import edu.illinois.yasgl.DirectedGraph;

public class Result {
    private final Map<String, Set<String>> testDeps;
    private final DirectedGraph<String> graph;
    private final Set<String> affectedTests;
    private final Set<String> unreachedDeps;

    public Result(Map<String, Set<String>> testDeps, DirectedGraph<String> graph,
                  Set<String> affectedTests, Set<String> unreached) {
        this.testDeps = testDeps;
        this.graph = graph;
        this.affectedTests = affectedTests;
        this.unreachedDeps = unreached;
    }

    public Map<String, Set<String>> getTestDeps() {
        return testDeps;
    }

    public DirectedGraph<String> getGraph() {
        return graph;
    }

    public Set<String> getAffectedTests() {
        return affectedTests;
    }

    public Set<String> getUnreachedDeps() {
        return unreachedDeps;
    }
}
