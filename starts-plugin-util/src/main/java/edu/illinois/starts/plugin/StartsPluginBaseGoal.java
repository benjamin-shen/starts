package edu.illinois.starts.plugin;

import edu.illinois.starts.data.ZLCFormat;
import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.RTSUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.util.Logger;
import edu.illinois.yasgl.DirectedGraph;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static edu.illinois.starts.constants.StartsConstants.STARTS_AFFECTED_TESTS;

/**
 * Interface with default methods for STARTS plugin goals (MOJOs or Gradle Tasks).
 */
public interface StartsPluginBaseGoal {
    String getArtifactsDir() throws StartsPluginException;
    String getGraphCache();
    boolean isUseThirdParty();
    boolean isFilterLib();
    DependencyFormat getDepFormat();
    boolean isPrintGraph();
    String getGraphFile();
    ZLCFormat getZlcFormat();

    default String getLocalRepositoryDir() {
        return null;
    };

    default void printResult(Set<String> set, String title) {
        Writer.writeToLog(set, title, Logger.getGlobal());
    }

    default void printToTerminal(List<String> testClasses, Set<String> affectedTests) {
        Logger.getGlobal().log(Level.INFO, STARTS_AFFECTED_TESTS + affectedTests.size());
        Logger.getGlobal().log(Level.INFO, "STARTS:TotalTests: " + testClasses.size());
    }

    default void save(String artifactsDir, Set<String> affectedTests, List<String> testClasses,
                      String sfPathString, DirectedGraph<String> graph) {
        int globalLogLevel = Logger.getGlobal().getLoggingLevel().intValue();
        if (globalLogLevel <= Level.FINER.intValue()) {
            Writer.writeToFile(testClasses, "all-tests", artifactsDir);
            Writer.writeToFile(affectedTests, "selected-tests", artifactsDir);
        }
        if (globalLogLevel <= Level.FINEST.intValue()) {
            RTSUtil.saveForNextRun(artifactsDir, graph, isPrintGraph(), getGraphFile());
            Writer.writeClassPath(sfPathString, artifactsDir);
        }
    }
}
