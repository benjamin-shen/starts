package edu.illinois.starts.plugin.buildsystem;

import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.Cache;
import edu.illinois.starts.helpers.Loadables;
import edu.illinois.starts.helpers.RTSUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.helpers.ZLCHelper;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.plugin.goals.StartsPluginBaseGoal;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Result;
import edu.illinois.yasgl.DirectedGraph;
import org.apache.maven.plugin.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.util.DefaultScanResult;
import org.gradle.api.GradleException;
import org.gradle.internal.classloader.DefaultClassLoaderFactory;
import org.gradle.internal.classpath.ClassPath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import static edu.illinois.starts.constants.StartsConstants.PROFILE_UPDATE_FOR_NEXT_RUN_TOTAL;

/**
 * Wrapper class for STARTS plugin Gradle goal
 */
public interface StartsPluginGradleGoal extends StartsPluginBaseGoal {
    File getTestClassDir();

    default ClassLoader createClassLoader(ClassPath testClassPath) {
        long start = System.currentTimeMillis();
        ClassLoader loader = new DefaultClassLoaderFactory().createIsolatedClassLoader(testClassPath);
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(createClassLoader): "
                + Writer.millsToSeconds(end - start));
        return loader;
    }

    default List<String> getTestClasses(String methodName) {
        long start = System.currentTimeMillis();
        DirectoryScanner scanner = new DirectoryScanner(getTestClassDir(), TestListResolver.getEmptyTestListResolver());
        DefaultScanResult defaultScanResult = scanner.scan();
        List<String> testClasses = (List<String>) defaultScanResult.getFiles();
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] " + methodName + "(getTestClasses): "
                + Writer.millsToSeconds(end - start));
        return testClasses;
    }

    default Result prepareForNextRun(String testClassPathString, ClassPath testClassPath, List<String> classesToAnalyze,
                                    Set<String> nonAffected, boolean computeUnreached) throws StartsPluginException {
        // TODO @bshen refactor
        long start = System.currentTimeMillis();
        File jdepsCache = new File(getGraphCache());

        // Create the Loadables object early so we can use its helpers
        Loadables loadables = new Loadables(classesToAnalyze, getArtifactsDir(), testClassPathString,
                isUseThirdParty(), isFilterLib(), jdepsCache);
        List<String> paths = new ArrayList<>();
        for (File file: testClassPath.getAsFiles()) {
            paths.add(file.getPath());
        }
        loadables.setTestClassPaths(paths);

        long loadMoreEdges = System.currentTimeMillis();
        Cache cache = new Cache(jdepsCache, null);
        // 1. Load non-reflection edges from third-party libraries in the classpath
        List<String> moreEdges = new ArrayList<>();
        if (isUseThirdParty()) {
            moreEdges = cache.loadM2EdgesFromCache(testClassPathString);
        }
        long loadM2EdgesFromCache = System.currentTimeMillis();
        // 2. Get non-reflection edges from CUT and SDK; use (1) to build graph
        loadables.create(new ArrayList<>(moreEdges), paths, computeUnreached);

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

    default void updateForNextRun(Set<String> nonAffected) throws StartsPluginException {
        // TODO @bshen refactor
        long start = System.currentTimeMillis();
        String testClassPathString = getTestClassPath().toString();
        List<String> allTests = getTestClasses("updateForNextRun");
        Set<String> affectedTests = new HashSet<>(allTests);
        affectedTests.removeAll(nonAffected);
        DirectedGraph<String> graph = null;
        if (!affectedTests.isEmpty()) {
            ClassLoader loader = createClassLoader(getTestClassPath());
            //TODO: set this boolean to true only for static reflectionAnalyses with * (border, string, naive)?
            boolean computeUnreached = true;
            Result result = prepareForNextRun(testClassPathString, getTestClassPath(), allTests, nonAffected, computeUnreached);
            Map<String, Set<String>> testDeps = result.getTestDeps();
            graph = result.getGraph();
            Set<String> unreached = computeUnreached ? result.getUnreachedDeps() : new HashSet<>();
            if (getDepFormat() == DependencyFormat.ZLC) {
                ZLCHelper.updateZLCFile(testDeps, loader, getArtifactsDir(), unreached, isUseThirdParty(), getZlcFormat());
            } else if (getDepFormat() == DependencyFormat.CLZ) {
                // The next line is not needed with ZLC because '*' is explicitly tracked in ZLC
                affectedTests = result.getAffectedTests();
                if (affectedTests == null) {
                    throw new GradleException("Affected tests should not be null with CLZ format!");
                }
                try {
                    RTSUtil.computeAndSaveNewCheckSums(getArtifactsDir(), affectedTests, testDeps, loader);
                } catch (IOException ioe) {
                    throw new GradleException(ioe.getMessage());
                }
            }
        }
        save(getArtifactsDir(), affectedTests, allTests, testClassPathString, graph);
        printToTerminal(allTests, affectedTests);
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, PROFILE_UPDATE_FOR_NEXT_RUN_TOTAL + Writer.millsToSeconds(end - start));
    }

    // TODO @bshen remove after refactor
    default void save(String artifactsDir, Set<String> affectedTests, List<String> allTests,
              String testClassPathString, DirectedGraph<String> graph) {
    }

    ClassPath getTestClassPath();
}
