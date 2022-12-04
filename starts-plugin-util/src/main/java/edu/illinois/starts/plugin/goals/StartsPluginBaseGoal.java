package edu.illinois.starts.plugin.goals;

import edu.illinois.starts.data.ZLCFormat;
import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.util.Logger;

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

    default void printResult(Set<String> set, String title) {
        Writer.writeToLog(set, title, Logger.getGlobal());
    }

    default void printToTerminal(List<String> testClasses, Set<String> affectedTests) {
        Logger.getGlobal().log(Level.INFO, STARTS_AFFECTED_TESTS + affectedTests.size());
        Logger.getGlobal().log(Level.INFO, "STARTS:TotalTests: " + testClasses.size());
    }
}
