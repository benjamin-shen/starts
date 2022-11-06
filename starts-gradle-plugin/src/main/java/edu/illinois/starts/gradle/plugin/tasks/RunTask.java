package edu.illinois.starts.gradle.plugin.tasks;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

/**
 * Prepares for test runs by writing non-affected tests in the excludesFile.
 */
public class RunTask extends BaseTask {
    public static final String NAME = "startsRun";
    public static final String DESCRIPTION = "Prepares for test runs by writing non-affected tests in the excludesFile.";

    protected boolean updateRunChecksums = false;

    @Input
    public boolean getUpdateRunChecksums() {
        return this.updateRunChecksums;
    }

    @Option(
            option = "updateRunChecksums",
            description = "Set this to \"true\" to update test dependencies on disk. The default value of \"false\" " +
                    "is useful for \"dry runs\" where one may want to see the non-affected tests that " +
                    "STARTS writes to the Surefire excludesFile, without updating test dependencies."
    )
    public void setUpdateRunChecksums(String updateRunChecksums) {
        this.updateRunChecksums = updateRunChecksums.equals(TRUE);
    }

    @TaskAction
    public void execute() {
    }
}
