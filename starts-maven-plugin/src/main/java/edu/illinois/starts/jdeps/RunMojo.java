/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.maven.AgentLoader;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.plugin.goals.StartsPluginRunGoal;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Prepares for test runs by writing non-affected tests in the excludesFile.
 */
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.TEST)
public class RunMojo extends DiffMojo implements StartsPluginRunGoal {
    private static final String TARGET = "target";
    /**
     * Set this to "false" to prevent checksums from being persisted to disk. This
     * is useful for "dry runs" where one may want to see the non-affected tests that
     * STARTS writes to the Surefire excludesFile, without updating test dependencies.
     */
    @Parameter(property = "updateRunChecksums", defaultValue = TRUE)
    @Getter
    protected boolean updateRunChecksums;

    /**
     * Set this option to "true" to run all tests, not just the affected ones. This option is useful
     * in cases where one is interested to measure the time to run all tests, while at the
     * same time measuring the times for analyzing what tests to select and reporting the number of
     * tests it would select.
     * Note: Run with "-DstartsLogging=FINER" or "-DstartsLogging=FINEST" so that the "selected-tests"
     * file, which contains the list of tests that would be run if this option is set to false, will
     * be written to disk.
     */
    @Parameter(property = "retestAll", defaultValue = FALSE)
    @Getter
    protected boolean retestAll;

    /**
     * Set this to "true" to save nonAffectedTests to a file on disk. This improves the time for
     * updating test dependencies in offline mode by not running computeChangeData() twice.
     * Note: Running with "-DstartsLogging=FINEST" also saves nonAffectedTests to a file on disk.
     */
    @Parameter(property = "writeNonAffected", defaultValue = FALSE)
    protected boolean writeNonAffected;

    /**
     * Set this to "true" to save changedClasses to a file on disk.
     * Note: Running with "-DstartsLogging=FINEST" also saves changedClasses to a file on disk.
     */
    @Parameter(property = "writeChangedClasses", defaultValue = FALSE)
    protected boolean writeChangedClasses;

    @Getter
    protected Set<String> nonAffectedTests;
    protected Set<String> changedClasses;

    @Setter @Getter
    protected List<Pair<String, String>> jarCheckSums = null;

    private Logger logger;

    public void execute() throws MojoExecutionException {
        try {
            Logger.getGlobal().setLoggingLevel(loggingLevel);
            logger = Logger.getGlobal();
            long start = System.currentTimeMillis();
            setIncludesExcludes();
            run();
            Set<String> allTests = new HashSet<>(getTestClasses(CHECK_IF_ALL_AFFECTED));
            if (writeNonAffected || logger.getLoggingLevel().intValue() <= Level.FINEST.intValue()) {
                Writer.writeToFile(nonAffectedTests, "non-affected-tests", getArtifactsDir());
            }
            if (allTests.equals(nonAffectedTests)) {
                logger.log(Level.INFO, STARS_RUN_STARS);
                logger.log(Level.INFO, NO_TESTS_ARE_SELECTED_TO_RUN);
            }
            long end = System.currentTimeMillis();
            System.setProperty(PROFILE_END_OF_RUN_MOJO, Long.toString(end));
            logger.log(Level.FINE, PROFILE_RUN_MOJO_TOTAL + Writer.millsToSeconds(end - start));
        } catch (StartsPluginException spe) {
            throw new MojoExecutionException(spe.getMessage(), spe.getCause());
        }
    }

    @Override
    public void dynamicallyUpdateExcludes(List<String> excludePaths) throws StartsPluginException {
        if (AgentLoader.loadDynamicAgent()) {
            logger.log(Level.FINEST, "AGENT LOADED!!!");
            System.setProperty(STARTS_EXCLUDE_PROPERTY, Arrays.toString(excludePaths.toArray(new String[0])));
        } else {
            throw new StartsPluginException("I COULD NOT ATTACH THE AGENT");
        }
    }

    public String getClassesPath() {
        return File.separator + TARGET +  File.separator + CLASSES;
    }

    public String getTestClassesPath() {
        return File.separator + TARGET +  File.separator + TEST_CLASSES;
    }

    public void setChangedAndNonaffected() throws StartsPluginException {
        nonAffectedTests = new HashSet<>();
        changedClasses = new HashSet<>();
        Pair<Set<String>, Set<String>> data = computeChangeData(writeChangedClasses);
        nonAffectedTests = data == null ? new HashSet<>() : data.getKey();
        changedClasses  = data == null ? new HashSet<>() : data.getValue();
    }
}
