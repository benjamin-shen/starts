/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.plugin.buildsystem.StartsPluginMavenGoal;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Finds tests affected by a change but does not run them.
 */
@Mojo(name = "select", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class SelectMojo extends DiffMojo implements StartsPluginMavenGoal {
    /**
     * Set this to "true" to update test dependencies on disk. The default value of
     * "false" is useful for "dry runs" where one may want to see the affected
     * tests, without updating test dependencies.
     */
    @Parameter(property = "updateSelectChecksums", defaultValue = FALSE)
    private boolean updateSelectChecksums;

    private Logger logger;

    public void execute() throws MojoExecutionException {
        try {
            Logger.getGlobal().setLoggingLevel(loggingLevel);
            logger = Logger.getGlobal();
            long start = System.currentTimeMillis();
            Set<String> affectedTests = computeAffectedTests();
            printResult(affectedTests, "AffectedTests");
            long end = System.currentTimeMillis();
            logger.log(Level.FINE, PROFILE_RUN_MOJO_TOTAL + Writer.millsToSeconds(end - start));
            logger.log(Level.FINE, PROFILE_TEST_RUNNING_TIME + 0.0);
        } catch (StartsPluginException spe) {
            throw new MojoExecutionException(spe.getMessage(), spe.getCause());
        }
    }

    private Set<String> computeAffectedTests() throws StartsPluginException {
        setIncludesExcludes();
        Set<String> allTests = new HashSet<>(getTestClasses(CHECK_IF_ALL_AFFECTED));
        Set<String> affectedTests = new HashSet<>(allTests);
        Pair<Set<String>, Set<String>> data = computeChangeData(false);
        Set<String> nonAffectedTests = data == null ? new HashSet<String>() : data.getKey();
        affectedTests.removeAll(nonAffectedTests);
        if (allTests.equals(nonAffectedTests)) {
            logger.log(Level.INFO, STARS_RUN_STARS);
            logger.log(Level.INFO, NO_TESTS_ARE_SELECTED_TO_RUN);
        }
        long startUpdate = System.currentTimeMillis();
        if (updateSelectChecksums) {
            updateForNextRun(nonAffectedTests);
        }
        long endUpdate = System.currentTimeMillis();
        logger.log(Level.FINE, PROFILE_STARTS_MOJO_UPDATE_TIME + Writer.millsToSeconds(endUpdate - startUpdate));
        return affectedTests;
    }
}
