package edu.illinois.starts.gradle.plugin.tasks;

import edu.illinois.starts.data.ZLCFormat;
import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import java.util.HashSet;
import java.util.Set;

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

        Set<String> changed = new HashSet<>();
        Set<String> nonAffected = new HashSet<>();
        Pair<Set<String>, Set<String>> data = null; //computeChangeData(false);
        String extraText = EMPTY;
        if (data != null) {
            nonAffected = data.getKey();
            changed = data.getValue();
        } else {
            extraText = " (no RTS artifacts; likely the first run)";
        }
        printResult(changed, "ChangedClasses" + extraText);
//        if (updateDiffChecksums) {
//            updateForNextRun(nonAffected);
//        }
    }
}
