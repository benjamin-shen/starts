package edu.illinois.starts.plugin.goals;

import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.EkstaziHelper;
import edu.illinois.starts.helpers.RTSUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.helpers.ZLCHelper;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import edu.illinois.starts.util.Result;
import edu.illinois.yasgl.DirectedGraph;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import static edu.illinois.starts.constants.StartsConstants.CHANGED_CLASSES;
import static edu.illinois.starts.constants.StartsConstants.EMPTY;
import static edu.illinois.starts.constants.StartsConstants.PROFILE_UPDATE_FOR_NEXT_RUN_TOTAL;
import static edu.illinois.starts.constants.StartsConstants.STARTS_AFFECTED_TESTS;

public interface StartsPluginDiffGoal extends StartsPluginBaseGoal {
    boolean isCleanBytes();
    boolean isUpdateDiffChecksums();

    ClassLoader getClassLoader();

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

    default Pair<Set<String>, Set<String>> computeChangeData(boolean writeChanged) throws StartsPluginException {
        long start = System.currentTimeMillis();
        Pair<Set<String>, Set<String>> data = null;
        if (getDepFormat() == DependencyFormat.ZLC) {
            data = ZLCHelper.getChangedData(getArtifactsDir(), isCleanBytes());
        } else if (getDepFormat() == DependencyFormat.CLZ) {
            data = EkstaziHelper.getNonAffectedTests(getArtifactsDir());
        }
        Set<String> changed = data == null ? new HashSet<>() : data.getValue();
        if (writeChanged || Logger.getGlobal().getLoggingLevel().intValue() <= Level.FINEST.intValue()) {
            Writer.writeToFile(changed, CHANGED_CLASSES, getArtifactsDir());
        }
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] COMPUTING CHANGES: " + Writer.millsToSeconds(end - start));
        return data;
    }

    default void computeDiff() throws StartsPluginException {
        Pair<Set<String>, Set<String>> data = computeChangeData(false);
        String extraText = EMPTY;
        Set<String> nonAffected = new HashSet<>();
        Set<String> changedClasses = new HashSet<>();
        if (data != null) {
            nonAffected = data.getKey();
            changedClasses = data.getValue();
        } else {
            extraText = " (no RTS artifacts; likely the first run)";
        }
        printResult(changedClasses, "ChangedClasses" + extraText);
        if (isUpdateDiffChecksums()) {
            updateForNextRun(nonAffected);
        }
    }

    List<String> getTestClassPathElementsPaths();

    String getTestClassPathElementsString();

    default void updateForNextRun(Set<String> nonAffected) throws StartsPluginException {
        long start = System.currentTimeMillis();
        setIncludesExcludes();
        String testClassPathElementsString = getTestClassPathElementsString();
        List<String> testClassPathElementsPaths = getTestClassPathElementsPaths();
        List<String> allTests = getTestClasses("updateForNextRun");
        Set<String> affectedTests = new HashSet<>(allTests);
        affectedTests.removeAll(nonAffected);
        DirectedGraph<String> graph = null;
        if (!affectedTests.isEmpty()) {
            ClassLoader loader = getClassLoader();
            //TODO: set this boolean to true only for static reflectionAnalyses with * (border, string, naive)?
            boolean computeUnreached = true;
            Result result = prepareForNextRun(testClassPathElementsString, testClassPathElementsPaths, allTests, nonAffected, computeUnreached);
            Map<String, Set<String>> testDeps = result.getTestDeps();
            graph = result.getGraph();
            Set<String> unreached = computeUnreached ? result.getUnreachedDeps() : new HashSet<>();
            if (getDepFormat() == DependencyFormat.ZLC) {
                ZLCHelper.updateZLCFile(testDeps, loader, getArtifactsDir(), unreached, isUseThirdParty(), getZlcFormat());
            } else if (getDepFormat() == DependencyFormat.CLZ) {
                // The next line is not needed with ZLC because '*' is explicitly tracked in ZLC
                affectedTests = result.getAffectedTests();
                if (affectedTests == null) {
                    throw new StartsPluginException("Affected tests should not be null with CLZ format!");
                }
                try {
                    RTSUtil.computeAndSaveNewCheckSums(getArtifactsDir(), affectedTests, testDeps, loader);
                } catch (IOException ioe) {
                    throw new StartsPluginException(ioe.getMessage(), ioe.getCause());
                }
            }
        }
        save(getArtifactsDir(), affectedTests, allTests, testClassPathElementsString, graph);
        printToTerminal(allTests, affectedTests);
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, PROFILE_UPDATE_FOR_NEXT_RUN_TOTAL + Writer.millsToSeconds(end - start));
    }

    List<String> getTestClasses(String updateForNextRun);

    default void setIncludesExcludes() throws StartsPluginException {};
}
