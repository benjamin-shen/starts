package edu.illinois.starts.gradle.plugin.tasks;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.util.Logger;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.options.Option;
import org.gradle.internal.classloader.DefaultClassLoaderFactory;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.classpath.DefaultClassPath;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class BaseTask extends DefaultTask implements StartsConstants {
    protected boolean filterLib = true;
    protected boolean useThirdParty = false;
    protected DependencyFormat depFormat = DependencyFormat.ZLC;
    protected String graphCache;
    protected boolean printGraph = true;
    protected String graphFile = GRAPH;
    protected Level loggingLevel = Level.CONFIG;
    /**
     * The directory in which to store STARTS artifacts that are needed between runs.
     */
    @Internal
    protected String artifactsDir;
    @Internal
    protected ClassPath testClassPath;
    @Internal
    Set<String> allClasses;

    @Input
    public boolean getFilterLib() {
        return this.filterLib;
    }

    @Option(
        option = "filterLib",
        description = "Set this to \"false\" to not filter out \"sun.*\" and \"java.*\" classes from jdeps parsing."
    )
    public void setFilterLib(String filterLib) {
        this.filterLib = !filterLib.equals(FALSE);
    }

    @Input
    public boolean getUseThirdParty() {
        return this.useThirdParty;
    }

    @Option(
            option = "useThirdParty",
            description = "Set this to \"false\" to not add jdeps edges from 3rd party-libraries."
    )
    public void setUseThirdParty(String useThirdParty) {
        this.useThirdParty = !useThirdParty.equals(FALSE);
    }

    public String getArtifactsDir() throws GradleException {
        if (artifactsDir == null) {
            artifactsDir = Paths.get(getProject().getRootDir().getAbsolutePath(), STARTS_DIRECTORY_PATH).toString();
            File file = new File(artifactsDir);
            if (!file.mkdirs() && !file.exists()) {
                throw new GradleException("I could not create artifacts dir: " + artifactsDir);
            }
        }
        return artifactsDir;
    }

    @Input
    public DependencyFormat getDepFormat() {
        return this.depFormat;
    }

    @Option(
            option = "depFormat",
            description = "Allows to switch the format in which we want to store the test dependencies. " +
                    "A full list of what we currently support can be found in edu.illinois.starts.enums.DependencyFormat"
    )
    public void setDepFormat(String depFormat) {
        this.depFormat = DependencyFormat.valueOf(depFormat);
    }

    @Input
    public String getGraphCache() {
        if (this.graphCache == null) {
            this.graphCache = Paths.get(getProject().getRootDir().getAbsolutePath(), JDEPS_CACHE).toString();
        }
        return this.graphCache;
    }

    @Option(
            option = "gCache",
            description = "Path to directory that contains the result of running jdeps on third-party " +
                    "and standard library jars that an application may need, e.g., those in M2_REPO."
    )
    public void setGraphCache(String graphCache) {
        this.graphCache = graphCache;
    }

    @Input
    public boolean getPrintGraph() {
        return this.printGraph;
    }

    @Option(
            option = "printGraph",
            description = "Set this to \"false\" to not print the graph obtained from jdeps parsing. " +
                    "When \"true\" the graph is written to file after the run."
    )
    public void setPrintGraph(String printGraph) {
        this.printGraph = !printGraph.equals(FALSE);
    }

    @Input
    public String getGraphFile() {
        return this.graphFile;
    }

    @Option(
            option = "graph",
            description = "Output filename for the graph, if printGraph == true."
    )
    public void setGraphFile(String graphFile) {
        this.graphFile = graphFile;
    }

    @Input
    public Level getLoggingLevel() {
        return this.loggingLevel;
    }

    @Option(
            option = "startsLogging",
            description = "Log levels as defined in java.util.logging.Level."
    )
    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = Level.parse(loggingLevel);
    }

    protected void printResult(Set<String> set, String title) {
        Writer.writeToLog(set, title, Logger.getGlobal());
    }

    public void setIncludesExcludes() {
        long start = System.currentTimeMillis();
        // TODO not implemented
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(setIncludesExcludes): "
                + Writer.millsToSeconds(end - start));
    }

    public List<String> getTestClasses(String methodName) {
        long start = System.currentTimeMillis();
        List<File> files = getTestClassPath().getAsFiles();
        List<String> testClasses = new ArrayList<>();
        for (File file : files) {
            testClasses.add(file.getPath());
        }
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] " + methodName + "(getTestClasses): "
                + Writer.millsToSeconds(end - start));
        return testClasses;
    }

    public ClassLoader createClassLoader(ClassPath testClassPath) {
        long start = System.currentTimeMillis();
        ClassLoader loader = new DefaultClassLoaderFactory().createIsolatedClassLoader("MyRole", testClassPath);
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(createClassLoader): "
                + Writer.millsToSeconds(end - start));
        return loader;
    }

    public ClassPath getTestClassPath() {
        long start = System.currentTimeMillis();
        if (testClassPath == null) {
            Set<File> files = getProject().getConfigurations().getByName("testRuntimeClasspath").getFiles();
            files.add(getProject().getBuildDir()); // the `build` directory, by default
            testClassPath = DefaultClassPath.of(files);
        }
        Logger.getGlobal().log(Level.FINEST, "TEST-CLASSPATH: " + testClassPath.getAsURLs());
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(getTestClassPath): "
                + Writer.millsToSeconds(end - start));
        return testClassPath;
    }

//    public Result prepareForNextRun(String testClassPathString, ClassPath testClassPath, List<String> classesToAnalyze,
//                                    Set<String> nonAffected, boolean computeUnreached) {
//        long start = System.currentTimeMillis();
//        File jdepsCache = new File(graphCache);
//
//        // Create the Loadables object early so we can use its helpers
//        Loadables loadables = new Loadables(classesToAnalyze, artifactsDir, testClassPathString,
//                useThirdParty, filterLib, jdepsCache);
//        List<String> paths = new ArrayList<>();
//        for (File file: testClassPath.getAsFiles()) {
//            paths.add(file.getPath());
//        }
//        loadables.setTestClassPaths(paths);
//
//        long loadMoreEdges = System.currentTimeMillis();
//        Cache cache = new Cache(jdepsCache, null);
//        // 1. Load non-reflection edges from third-party libraries in the classpath
//        List<String> moreEdges = new ArrayList<>();
//        if (useThirdParty) {
//            moreEdges = cache.loadM2EdgesFromCache(testClassPathString);
//        }
//        long loadM2EdgesFromCache = System.currentTimeMillis();
//        // 2. Get non-reflection edges from CUT and SDK; use (1) to build graph
//        loadables.create(new ArrayList<>(moreEdges), paths, computeUnreached);
//
//        Map<String, Set<String>> transitiveClosure = loadables.getTransitiveClosure();
//        long createLoadables = System.currentTimeMillis();
//
//        // We don't need to compute affected tests this way with ZLC format.
//        // In RTSUtil.computeAffectedTests(), we find affected tests by (a) removing nonAffected tests from the set of
//        // all tests and then (b) adding all tests that reach to * as affected if there has been a change. This is only
//        // for CLZ which does not encode information about *. ZLC already encodes and reasons about * when it finds
//        // nonAffected tests.
//        Set<String> affected = depFormat == DependencyFormat.ZLC ? null
//                : RTSUtil.computeAffectedTests(new HashSet<>(classesToAnalyze),
//                nonAffected, transitiveClosure);
//        long end = System.currentTimeMillis();
//        Logger.getGlobal().log(Level.FINE, "[PROFILE] prepareForNextRun(loadMoreEdges): "
//                + Writer.millsToSeconds(loadMoreEdges - start));
//        Logger.getGlobal().log(Level.FINE, "[PROFILE] prepareForNextRun(loadM2EdgesFromCache): "
//                + Writer.millsToSeconds(loadM2EdgesFromCache - loadMoreEdges));
//        Logger.getGlobal().log(Level.FINE, "[PROFILE] prepareForNextRun(createLoadable): "
//                + Writer.millsToSeconds(createLoadables - loadM2EdgesFromCache));
//        Logger.getGlobal().log(Level.FINE, "[PROFILE] prepareForNextRun(computeAffectedTests): "
//                + Writer.millsToSeconds(end - createLoadables));
//        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(prepareForNextRun(TOTAL)): "
//                + Writer.millsToSeconds(end - start));
//        return new Result(transitiveClosure, loadables.getGraph(), affected, loadables.getUnreached());
//    }

    private void scanFiles(File file, Set<String> acc) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                scanFiles(child, acc);
            }
        } else {
            acc.add(file.getPath());
        }
    }

    protected Set<String> getAllClasses() {
        if (allClasses == null) {
            allClasses = new HashSet<>();
            List<File> testClassesDirs = getTestClassPath().getAsFiles();
            File classesDir = getProject().getBuildDir();
            for (File file : testClassesDirs) {
                scanFiles(file, allClasses);
            }
            scanFiles(classesDir, allClasses);
        }
        return allClasses;
    }
}
