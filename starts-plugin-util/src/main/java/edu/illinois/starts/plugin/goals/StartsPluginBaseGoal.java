package edu.illinois.starts.plugin.goals;

import edu.illinois.starts.data.ZLCFormat;
import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.Cache;
import edu.illinois.starts.helpers.Loadables;
import edu.illinois.starts.helpers.RTSUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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

    String getLocalRepositoryDir();

    default void printResult(Set<String> set, String title) {
        Writer.writeToLog(set, title, Logger.getGlobal());
    }

    List<String> getTestClasses(String methodName);

    List<String> getTestClassPathElementsPaths();

    String getTestClassPathElementsString();

    default Result prepareForNextRun(String testClassPathElementsString, List<String> testClassPathElementsPaths,
                                     List<String> classesToAnalyze, Set<String> nonAffected, boolean computeUnreached) throws StartsPluginException {
        long start = System.currentTimeMillis();
        String m2Repo = getLocalRepositoryDir();
        File jdepsCache = new File(getGraphCache());
        // We store the jdk-graphs at the root of "jdepsCache" directory, with
        // jdk.graph being the file that merges all the graphs for all standard
        // library jars.
        File libraryFile = new File(jdepsCache, "jdk.graph");
        // Create the Loadables object early so we can use its helpers
        Loadables loadables = new Loadables(classesToAnalyze, getArtifactsDir(), testClassPathElementsString,
                isUseThirdParty(), isFilterLib(), jdepsCache);
        loadables.setTestClassPathElements(testClassPathElementsPaths);

        long loadMoreEdges = System.currentTimeMillis();
        Cache cache = new Cache(jdepsCache, m2Repo);
        // 1. Load non-reflection edges from third-party libraries in the classpath
        List<String> moreEdges = new ArrayList<>();
        if (isUseThirdParty()) {
            moreEdges = cache.loadM2EdgesFromCache(testClassPathElementsString);
        }
        long loadM2EdgesFromCache = System.currentTimeMillis();
        // 2. Get non-reflection edges from CUT and SDK; use (1) to build graph
        loadables.create(new ArrayList<>(moreEdges), testClassPathElementsPaths, computeUnreached);

        Map<String, Set<String>> transitiveClosure = loadables.getTransitiveClosure();
        long createLoadables = System.currentTimeMillis();

        // We don't need to compute affected tests this way with ZLC format.
        // In RTSUtil.computeAffectedTests(), we find affected tests by (a) removing nonAffected tests from the set of
        // all tests and then (b) adding all tests that reach to * as affected if there has been a change. This is only
        // for CLZ which does not encode information about *. ZLC already encodes and reasons about * when it finds
        // nonAffected tests.
        Set<String> affected = getDepFormat() == DependencyFormat.ZLC ? null
                : RTSUtil.computeAffectedTests(new HashSet<>(classesToAnalyze),
                nonAffected, transitiveClosure);
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] prepareForNextRun(loadMoreEdges): "
                + Writer.millsToSeconds(loadMoreEdges - start));
        Logger.getGlobal().log(Level.FINE, "[PROFILE] prepareForNextRun(loadM2EdgesFromCache): "
                + Writer.millsToSeconds(loadM2EdgesFromCache - loadMoreEdges));
        Logger.getGlobal().log(Level.FINE, "[PROFILE] prepareForNextRun(createLoadable): "
                + Writer.millsToSeconds(createLoadables - loadM2EdgesFromCache));
        Logger.getGlobal().log(Level.FINE, "[PROFILE] prepareForNextRun(computeAffectedTests): "
                + Writer.millsToSeconds(end - createLoadables));
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(prepareForNextRun(TOTAL)): "
                + Writer.millsToSeconds(end - start));
        return new Result(transitiveClosure, loadables.getGraph(), affected, loadables.getUnreached());
    }
}
