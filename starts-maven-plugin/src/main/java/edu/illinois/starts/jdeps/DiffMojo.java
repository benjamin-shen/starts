/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.EkstaziHelper;
import edu.illinois.starts.helpers.RTSUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.helpers.ZLCHelper;
import edu.illinois.starts.plugin.StartsPluginDiffGoal;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.plugin.StartsPluginMavenGoal;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import edu.illinois.yasgl.DirectedGraph;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Finds types that have changed since the last time they were analyzed.
 */
@Mojo(name = "diff", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class DiffMojo extends BaseMojo implements StartsPluginMavenGoal, StartsPluginDiffGoal {
    /**
     * Set this to "false" to disable smart hashing, i.e., to *not* strip
     * Bytecode files of debug info prior to computing checksums. See the "Smart
     * Checksums" Sections in the Ekstazi paper:
     * http://dl.acm.org/citation.cfm?id=2771784
     */
    @Parameter(property = "cleanBytes", defaultValue = TRUE)
    protected boolean cleanBytes;

    /**
     * Set this to "true" to update test dependencies on disk. The default value of "false"
     * is useful for "dry runs" where one may want to see the diff without updating
     * the test dependencies.
     */
    @Parameter(property = "updateDiffChecksums", defaultValue = FALSE)
    private boolean updateDiffChecksums;

    public void execute() throws MojoExecutionException {
        try {
            Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));

            Set<String> changed = new HashSet<>();
            Set<String> nonAffected = new HashSet<>();
            Pair<Set<String>, Set<String>> data = computeChangeData(false);
            String extraText = EMPTY;
            if (data != null) {
                nonAffected = data.getKey();
                changed = data.getValue();
            } else {
                extraText = " (no RTS artifacts; likely the first run)";
            }
            printResult(changed, "ChangedClasses" + extraText);
            if (updateDiffChecksums) {
                updateForNextRun(nonAffected);
            }
        } catch (StartsPluginException spe) {
            throw new MojoExecutionException(spe.getMessage(), spe.getCause());
        }
    }

    protected Pair<Set<String>, Set<String>> computeChangeData(boolean writeChanged) throws StartsPluginException {
        long start = System.currentTimeMillis();
        Pair<Set<String>, Set<String>> data = null;
        if (depFormat == DependencyFormat.ZLC) {
            data = ZLCHelper.getChangedData(getArtifactsDir(), cleanBytes);
        } else if (depFormat == DependencyFormat.CLZ) {
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

    public void printToTerminal(List<String> testClasses, Set<String> affectedTests) {
        Logger.getGlobal().log(Level.INFO, STARTS_AFFECTED_TESTS + affectedTests.size());
        Logger.getGlobal().log(Level.INFO, "STARTS:TotalTests: " + testClasses.size());
    }

    public void save(String artifactsDir, Set<String> affectedTests, List<String> testClasses,
                     String sfPathString, DirectedGraph<String> graph) {
        int globalLogLevel = Logger.getGlobal().getLoggingLevel().intValue();
        if (globalLogLevel <= Level.FINER.intValue()) {
            Writer.writeToFile(testClasses, "all-tests", artifactsDir);
            Writer.writeToFile(affectedTests, "selected-tests", artifactsDir);
        }
        if (globalLogLevel <= Level.FINEST.intValue()) {
            RTSUtil.saveForNextRun(artifactsDir, graph, printGraph, graphFile);
            Writer.writeClassPath(sfPathString, artifactsDir);
        }
    }
}
