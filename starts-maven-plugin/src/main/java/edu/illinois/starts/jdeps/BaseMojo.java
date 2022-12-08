/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.data.ZLCFormat;
import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.plugin.buildsystem.StartsPluginMavenGoal;
import edu.illinois.starts.util.Logger;
import lombok.Getter;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.plugin.surefire.SurefirePlugin;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.SurefireExecutionException;
import org.apache.maven.surefire.util.DefaultScanResult;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;

/**
 * Base MOJO for the JDeps-Based STARTS.
 */
public abstract class BaseMojo extends SurefirePlugin implements StartsPluginMavenGoal, StartsConstants {
    /**
     * Set this to "false" to not filter out "sun.*" and "java.*" classes from jdeps parsing.
     */
    @Parameter(property = "filterLib", defaultValue = TRUE)
    @Getter
    protected boolean filterLib;

    /**
     * Set this to "false" to not add jdeps edges from 3rd party-libraries.
     */
    @Parameter(property = "useThirdParty", defaultValue = FALSE)
    @Getter
    protected boolean useThirdParty;

    /**
     * The directory in which to store STARTS artifacts that are needed between runs.
     */
    protected String artifactsDir;

    /**
     * Allows to switch the format in which we want to store the test dependencies.
     * A full list of what we currently support can be found in
     * @see edu.illinois.starts.enums.DependencyFormat
     */
    @Parameter(property = "depFormat", defaultValue = "ZLC")
    @Getter
    protected DependencyFormat depFormat;

    /**
     * Path to directory that contains the result of running jdeps on third-party
     * and standard library jars that an application may need, e.g., those in M2_REPO.
     */
    @Parameter(property = "gCache", defaultValue = "${basedir}${file.separator}jdeps-cache")
    @Getter
    protected String graphCache;

    /**
     * Set this to "false" to not print the graph obtained from jdeps parsing.
     * When "true" the graph is written to file after the run.
     */
    @Parameter(property = "printGraph", defaultValue = TRUE)
    @Getter
    protected boolean printGraph;

    /**
     * Output filename for the graph, if printGraph == true.
     */
    @Parameter(defaultValue = "graph", readonly = true, required = true)
    @Getter
    protected String graphFile;

    /**
     * Log levels as defined in java.util.logging.Level.
     */
    @Parameter(property = "startsLogging", defaultValue = "CONFIG")
    protected Level loggingLevel;

    /**
     * Format of the ZLC dependency file deps.zlc
     * Set to "INDEXED" to store indices of tests
     * Set to "PLAIN_TEXT" to store full URLs of tests
     */
    @Parameter(property = "zlcFormat", defaultValue = "PLAIN_TEXT")
    @Getter
    protected ZLCFormat zlcFormat;

    protected Classpath sureFireClassPath;

    public String getArtifactsDir() throws StartsPluginException {
        if (artifactsDir == null) {
            artifactsDir = basedir.getAbsolutePath() + File.separator + STARTS_DIRECTORY_PATH;
            File file = new File(artifactsDir);
            if (!file.mkdirs() && !file.exists()) {
                throw new StartsPluginException("I could not create artifacts dir: " + artifactsDir);
            }
        }
        return artifactsDir;
    }

    public Classpath getSureFireClassPath() {
        long start = System.currentTimeMillis();
        if (sureFireClassPath == null) {
            try {
                sureFireClassPath = new Classpath(getProject().getTestClasspathElements());
            } catch (DependencyResolutionRequiredException drre) {
                drre.printStackTrace();
            }
        }
        Logger.getGlobal().log(Level.FINEST, "SF-CLASSPATH: " + sureFireClassPath.getClassPath());
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(getSureFireClassPath): "
                + Writer.millsToSeconds(end - start));
        return sureFireClassPath;
    }

    public List<String> getTestClasses(String methodName) {
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
        return (List<String>) defaultScanResult.getFiles();
    }

    protected ClassLoader createClassLoader(Classpath sfClassPath) {
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

    public String getTestClassPathElementsString() {
        return Writer.pathToString(getTestClassPathElementsPaths());
    }

    public List<String> getTestClassPathElementsPaths() {
        Classpath sfClassPath = sureFireClassPath;
        return sfClassPath != null ? sfClassPath.getClassPath() : null;
    }
}
