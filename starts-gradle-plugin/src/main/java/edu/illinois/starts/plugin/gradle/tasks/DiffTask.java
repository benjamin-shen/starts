package edu.illinois.starts.plugin.gradle.tasks;

import edu.illinois.starts.data.ZLCFormat;
import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.EkstaziHelper;
import edu.illinois.starts.helpers.RTSUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.helpers.ZLCHelper;
import edu.illinois.starts.plugin.StartsPluginDiffGoal;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.plugin.StartsPluginGradleGoal;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import edu.illinois.yasgl.DirectedGraph;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Finds types that have changed since the last time they were analyzed.
 */
public class DiffTask extends BaseTask implements StartsPluginGradleGoal, StartsPluginDiffGoal {
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
        try {
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
                updateForNextRun(nonAffectedTests);
            }
        } catch (StartsPluginException spe) {
            throw new GradleException(spe.getMessage());
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
