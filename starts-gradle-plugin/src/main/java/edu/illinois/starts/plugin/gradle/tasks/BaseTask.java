package edu.illinois.starts.plugin.gradle.tasks;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.data.ZLCFormat;
import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.plugin.buildsystem.StartsPluginGradleGoal;
import edu.illinois.starts.util.Logger;
import org.apache.maven.plugin.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.util.DefaultScanResult;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.UnknownConfigurationException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.options.Option;
import org.gradle.internal.classloader.DefaultClassLoaderFactory;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.classpath.DefaultClassPath;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public abstract class BaseTask extends DefaultTask implements StartsPluginGradleGoal, StartsConstants {
    protected boolean filterLib = true;
    protected boolean useThirdParty = false;
    protected DependencyFormat depFormat = DependencyFormat.ZLC;
    protected ZLCFormat zlcFormat = ZLCFormat.PLAIN_TEXT;
    protected String graphCache;
    protected boolean printGraph = true;
    protected String graphFile = GRAPH;
    protected Level loggingLevel = Level.CONFIG;

    @Internal
    protected String artifactsDir;
    @Internal
    protected ClassPath testClassPathElements;
    @Internal
    Set<String> allClasses;
    @Internal
    private File classDir;
    @Internal
    private File testClassDir;

    @Input
    public boolean isFilterLib() {
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
    public boolean isUseThirdParty() {
        return this.useThirdParty;
    }

    @Option(
            option = "useThirdParty",
            description = "Set this to \"false\" to not add jdeps edges from 3rd party-libraries."
    )
    public void setUseThirdParty(String useThirdParty) {
        this.useThirdParty = !useThirdParty.equals(FALSE);
    }

    public String getArtifactsDir() throws StartsPluginException {
        if (artifactsDir == null) {
            artifactsDir = Paths.get(getProject().getRootDir().getAbsolutePath(), STARTS_DIRECTORY_PATH).toString();
            File file = new File(artifactsDir);
            if (!file.mkdirs() && !file.exists()) {
                throw new StartsPluginException("I could not create artifacts dir: " + artifactsDir);
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
    public boolean isPrintGraph() {
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

    public File getClassDir () {
        if (classDir == null) {
            classDir = StartsPluginGradleGoal.super.getClassDir();
        }
        return classDir;
    }

    @Input
    public ZLCFormat getZlcFormat() {
        return this.zlcFormat;
    }

    @Option(
            option = "zlcFormat",
            description = "Format of the ZLC dependency file deps.zlc. " +
                    "Set to \"INDEXED\" to store indices of tests. " +
                    "Set to \"PLAIN_TEXT\" to store full URLs of tests."
    )
    public void setZlcFormat(String zlcFormat) {
        this.zlcFormat = ZLCFormat.valueOf(zlcFormat);
    }

    public File getTestClassDir() {
        if (testClassDir == null) {
            testClassDir = StartsPluginGradleGoal.super.getTestClassDir();
        }
        return testClassDir;
    }

    public ClassPath getTestClassPathElements() throws GradleException {
        if (testClassPathElements == null) {
            long start = System.currentTimeMillis();
            Set<File> files;
            try {
                files = getProject().getConfigurations().getByName("testRuntimeClasspath").getFiles();
            } catch (UnknownConfigurationException uce) {
                throw new GradleException("STARTS Gradle Plugin requires the 'java' plugin.");
            }
            if (getClassDir().isDirectory()) {
                Collections.addAll(files, getClassDir().listFiles());
            }
            testClassPathElements = DefaultClassPath.of(files);
            Logger.getGlobal().log(Level.FINEST, "TEST-CLASSPATH: " + testClassPathElements);
            long end = System.currentTimeMillis();
            Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(getTestClassPathElements): "
                    + Writer.millsToSeconds(end - start));
        }
        return testClassPathElements;
    }

    @Internal
    public String getLocalRepositoryDir() {
        // unlike Maven, Gradle doesn't cache in m2Repo
        return null;
    }

    @Internal
    public String getTestClassPathElementsString() {
        return getTestClassPathElements().toString();
    }

    @Internal
    public List<String> getTestClassPathElementsPaths() {
        List<String> paths = new ArrayList<>();
        List<File> files = getTestClassPathElements().getAsFiles();
        for (File file: files) {
            paths.add(file.getPath());
        }
        return paths;
    }

    public List<String> getTestClasses(String methodName) {
        long start = System.currentTimeMillis();
        DirectoryScanner scanner = new DirectoryScanner(getTestClassDir(), TestListResolver.getEmptyTestListResolver());
        DefaultScanResult defaultScanResult = scanner.scan();
        List<String> testClasses = (List<String>) defaultScanResult.getFiles();
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] " + methodName + "(getTestClasses): "
                + Writer.millsToSeconds(end - start));
        return testClasses;
    }

    protected ClassLoader createClassLoader(ClassPath testClassPathElements) {
        long start = System.currentTimeMillis();
        ClassLoader loader = new DefaultClassLoaderFactory().createIsolatedClassLoader("MyRole", getTestClassPathElements());
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(createClassLoader): "
                + Writer.millsToSeconds(end - start));
        return loader;
    }

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
            List<File> testClassesDirs = getTestClassPathElements().getAsFiles();
            File classesDir = getProject().getBuildDir();
            for (File file : testClassesDirs) {
                scanFiles(file, allClasses);
            }
            scanFiles(classesDir, allClasses);
        }
        return allClasses;
    }
}
