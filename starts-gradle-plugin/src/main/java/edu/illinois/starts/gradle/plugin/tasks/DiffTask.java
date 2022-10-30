package edu.illinois.starts.gradle.plugin.tasks;

import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.util.HashSet;
import java.util.Set;

/**
 * Finds types that have changed since the last time they were analyzed.
 */
public class DiffTask extends BaseTask {
    public static final String NAME = "startsDiff";
    public static final String DESCRIPTION = "Finds types that have changed since the last time they were analyzed.";


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
