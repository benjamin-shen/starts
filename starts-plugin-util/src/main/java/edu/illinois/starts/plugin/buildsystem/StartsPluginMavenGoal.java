package edu.illinois.starts.plugin.buildsystem;

import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.Cache;
import edu.illinois.starts.helpers.Loadables;
import edu.illinois.starts.helpers.PomUtil;
import edu.illinois.starts.helpers.RTSUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.helpers.ZLCHelper;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.plugin.goals.StartsPluginBaseGoal;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Result;
import edu.illinois.yasgl.DirectedGraph;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.SurefireExecutionException;
import org.apache.maven.surefire.util.DefaultScanResult;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import static edu.illinois.starts.constants.StartsConstants.PROFILE_UPDATE_FOR_NEXT_RUN_TOTAL;

/**
 * Wrapper class for STARTS plugin Maven goal
 */
public interface StartsPluginMavenGoal extends StartsPluginBaseGoal {
    void setIncludes(List<String> includes);
    void setExcludes(List<String> excludes);
    ArtifactRepository getLocalRepository();
    File getTestClassesDirectory();
    File getClassesDirectory();

    default String getLocalRepositoryDir() {
        return getLocalRepository().getBasedir();
    };

    default void setIncludesExcludes() throws StartsPluginException {
        long start = System.currentTimeMillis();
        try {
            Field projectField = AbstractSurefireMojo.class.getDeclaredField("project");
            projectField.setAccessible(true);
            MavenProject accessedProject = (MavenProject) projectField.get(this);
            List<String> includes = PomUtil.getFromPom("include", accessedProject);
            List<String> excludes = PomUtil.getFromPom("exclude", accessedProject);
            Logger.getGlobal().log(Level.FINEST, "@@Excludes: " + excludes);
            Logger.getGlobal().log(Level.FINEST,"@@Includes: " + includes);
            setIncludes(includes);
            setExcludes(excludes);
        } catch (NoSuchFieldException nsfe) {
            nsfe.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        } catch (MojoExecutionException mee) {
            throw new StartsPluginException(mee.getMessage(), mee.getCause());
        }
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(setIncludesExcludes): "
                + Writer.millsToSeconds(end - start));
    }

    default List<String> getTestClasses(String methodName) {
        long start = System.currentTimeMillis();
        DefaultScanResult defaultScanResult = null;
        try {
            Method scanMethod = AbstractSurefireMojo.class.getDeclaredMethod("scanForTestClasses", null);
            scanMethod.setAccessible(true);
            defaultScanResult = (DefaultScanResult) scanMethod.invoke(this, null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
        }
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] " + methodName + "(getTestClasses): "
                + Writer.millsToSeconds(end - start));
        return (List<String>) defaultScanResult.getFiles(); // TODO @bshen
    }

    default ClassLoader createClassLoader(Classpath sfClassPath) {
        long start = System.currentTimeMillis();
        ClassLoader loader = null;
        try {
            loader = sfClassPath.createClassLoader(false, false, "MyRole");
        } catch (SurefireExecutionException see) {
            see.printStackTrace();
        }
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(createClassLoader): "
                + Writer.millsToSeconds(end - start));
        return loader;
    }

    default Result prepareForNextRun(String sfPathString, Classpath sfClassPath, List<String> classesToAnalyze,
                                     Set<String> nonAffected, boolean computeUnreached) throws StartsPluginException {
        // TODO @bshen refactor
        long start = System.currentTimeMillis();
        String m2Repo = getLocalRepositoryDir();
        File jdepsCache = new File(getGraphCache());
        // We store the jdk-graphs at the root of "jdepsCache" directory, with
        // jdk.graph being the file that merges all the graphs for all standard
        // library jars.
        File libraryFile = new File(jdepsCache, "jdk.graph");
        // Create the Loadables object early so we can use its helpers
        Loadables loadables = new Loadables(classesToAnalyze, getArtifactsDir(), sfPathString,
                isUseThirdParty(), isFilterLib(), jdepsCache);
        List<String> paths = sfClassPath != null ? sfClassPath.getClassPath() : null;
        loadables.setTestClassPaths(paths);

        long loadMoreEdges = System.currentTimeMillis();
        Cache cache = new Cache(jdepsCache, m2Repo);
        // 1. Load non-reflection edges from third-party libraries in the classpath
        List<String> moreEdges = new ArrayList<>();
        if (isUseThirdParty()) {
            moreEdges = cache.loadM2EdgesFromCache(sfPathString);
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
        Classpath sfClassPath = getSureFireClassPath();
        String sfPathString = Writer.pathToString(sfClassPath.getClassPath());
        setIncludesExcludes();
        List<String> allTests = getTestClasses("updateForNextRun");
        Set<String> affectedTests = new HashSet<>(allTests);
        affectedTests.removeAll(nonAffected);
        DirectedGraph<String> graph = null;
        if (!affectedTests.isEmpty()) {
            ClassLoader loader = createClassLoader(sfClassPath);
            //TODO: set this boolean to true only for static reflectionAnalyses with * (border, string, naive)?
            boolean computeUnreached = true;
            Result result = prepareForNextRun(sfPathString, sfClassPath, allTests, nonAffected, computeUnreached);
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
                    throw new StartsPluginException(ioe.getMessage(), ioe);
                }
            }
        }
        save(getArtifactsDir(), affectedTests, allTests, sfPathString, graph);
        printToTerminal(allTests, affectedTests);
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, PROFILE_UPDATE_FOR_NEXT_RUN_TOTAL + Writer.millsToSeconds(end - start));
    }

    // TODO @bshen remove after refactor
    default void save(String artifactsDir, Set<String> affectedTests, List<String> testClasses,
                      String sfPathString, DirectedGraph<String> graph) {
    }

    Classpath getSureFireClassPath();
}
