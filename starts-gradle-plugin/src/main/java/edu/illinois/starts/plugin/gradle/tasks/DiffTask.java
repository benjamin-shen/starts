package edu.illinois.starts.plugin.gradle.tasks;

import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.plugin.goals.StartsPluginDiffGoal;
import edu.illinois.starts.util.Logger;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

/**
 * Finds types that have changed since the last time they were analyzed.
 */
public class DiffTask extends BaseTask implements StartsPluginDiffGoal {
    public static final String NAME = "startsDiff";
    public static final String DESCRIPTION = "Finds types that have changed since the last time they were analyzed.";

    protected boolean cleanBytes = true;

    protected boolean updateDiffChecksums = false;

    @Input
    public boolean isCleanBytes() {
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
    public boolean isUpdateDiffChecksums() {
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

    public ClassLoader getClassLoader() {
        return createClassLoader(testClassPathElements);
    }

    @TaskAction
    public void execute() {
        try {
            Logger.getGlobal().setLoggingLevel(loggingLevel);
            computeDiff();
        } catch (StartsPluginException spe) {
            throw new GradleException(spe.getMessage());
        }
    }
}
