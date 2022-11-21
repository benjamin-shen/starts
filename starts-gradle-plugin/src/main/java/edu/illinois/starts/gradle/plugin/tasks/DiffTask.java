package edu.illinois.starts.gradle.plugin.tasks;

import edu.illinois.starts.data.ZLCFormat;
import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.EkstaziHelper;
import edu.illinois.starts.helpers.RTSUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.helpers.ZLCHelper;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import edu.illinois.starts.util.Result;
import edu.illinois.yasgl.DirectedGraph;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Finds types that have changed since the last time they were analyzed.
 */
public class DiffTask extends BaseTask {
    public static final String NAME = "startsDiff";
    public static final String DESCRIPTION = "Finds types that have changed since the last time they were analyzed.";

    protected boolean cleanBytes = true;
    protected ZLCFormat zlcFormat = ZLCFormat.PLAIN_TEXT;
    protected boolean updateDiffChecksums = false;

    @Input
    public boolean getCleanBytes() {
        return this.cleanBytes;
    }

    @Option(
            option = "cleanBytes",
            description = "Set this to \"false\" to disable smart hashing, i.e., to *not* strip " +
                    "Bytecode files of debug info prior to computing checksums. See the \"Smart " +
                    "Checksums\" Sections in the Ekstazi paper: http://dl.acm.org/citation.cfm?id=2771784"
    )
    public void setCleanBytes(String cleanBytes) {
        this.cleanBytes = !cleanBytes.equals(FALSE);
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

    @Input
    public boolean getUpdateDiffChecksums() {
        return this.updateDiffChecksums;
    }

    @Option(
            option = "updateDiffChecksums",
            description = "Set this to \"true\" to update test dependencies on disk. The default value of \"false\" " +
                    "is useful for \"dry runs\" where one may want to see the diff without updating " +
                    "the test dependencies."
    )
    public void setUpdateDiffChecksums(String updateDiffChecksums) {
        this.updateDiffChecksums = updateDiffChecksums.equals(TRUE);
    }

    @TaskAction
    public void execute() {
        Logger.getGlobal().setLoggingLevel(loggingLevel);

        Pair<Set<String>, Set<String>> data = computeChangeData(false);
        String extraText = EMPTY;
        if (data != null) {
            nonAffectedTests = data.getKey();
            changedClasses = data.getValue();
        } else {
            extraText = " (no RTS artifacts; likely the first run)";
        }
        printResult(changedClasses, "ChangedClasses" + extraText);
        if (updateDiffChecksums) {
            updateForNextRun();
        }
    }

    protected Pair<Set<String>, Set<String>> computeChangeData(boolean writeChanged) {
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

    protected void updateForNextRun() {
        long start = System.currentTimeMillis();
        String testClassPathString = getTestClassPath().toString();
        List<String> allTests = getTestClasses("updateForNextRun");
        Set<String> affectedTests = new HashSet<>(allTests);
        affectedTests.removeAll(nonAffectedTests);
        DirectedGraph<String> graph = null;
        if (!affectedTests.isEmpty()) {
            ClassLoader loader = createClassLoader(testClassPath);
            //TODO: set this boolean to true only for static reflectionAnalyses with * (border, string, naive)?
            boolean computeUnreached = true;
            Result result = prepareForNextRun(testClassPathString, testClassPath, allTests, computeUnreached);
            Map<String, Set<String>> testDeps = result.getTestDeps();
            graph = result.getGraph();
            Set<String> unreached = computeUnreached ? result.getUnreachedDeps() : new HashSet<String>();
            if (depFormat == DependencyFormat.ZLC) {
                ZLCHelper.updateZLCFile(testDeps, loader, getArtifactsDir(), unreached, useThirdParty, zlcFormat);
            } else if (depFormat == DependencyFormat.CLZ) {
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
