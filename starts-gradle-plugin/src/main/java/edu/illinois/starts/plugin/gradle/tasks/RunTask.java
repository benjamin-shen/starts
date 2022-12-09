package edu.illinois.starts.plugin.gradle.tasks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.plugin.goals.StartsPluginRunGoal;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.options.Option;
import org.gradle.api.tasks.testing.Test;

/**
 * Prepares for test runs by writing non-affected tests in the excludesFile.
 */
public class RunTask extends DiffTask implements StartsPluginRunGoal {
    public static final String NAME = "startsRun";
    public static final String DESCRIPTION = "Prepares for test runs by writing non-affected tests in the excludesFile.";

    protected boolean updateRunChecksums = true;
    protected boolean retestAll = false;
    protected boolean writeNonAffected = false;
    protected boolean writeChangedClasses = false;
    @Internal
    protected Set<String> nonAffectedTests = new HashSet<>();
    @Internal
    protected List<Pair<String, String>> jarCheckSums = null;
    protected Set<String> changedClasses = new HashSet<>();

    @Input
    public boolean isUpdateRunChecksums() {
        return this.updateRunChecksums;
    }

    @Option(
            option = "updateRunChecksums",
            description = "Set this to \"false\" to prevent checksums from being persisted to disk. This " +
                    "is useful for \"dry runs\" where one may want to see the non-affected tests that " +
                    "STARTS writes to the Surefire excludesFile, without updating test dependencies."
    )
    public void setUpdateRunChecksums(String updateRunChecksums) {
        this.updateRunChecksums = !updateRunChecksums.equals(FALSE);
    }

    @Input
    public boolean isRetestAll() {
        return this.retestAll;
    }

    @Option(
            option = "retestAll",
            description = "Set this option to \"true\" to run all tests, not just the affected ones. This option is useful " +
                    "in cases where one is interested to measure the time to run all tests, while at the " +
                    "same time measuring the times for analyzing what tests to select and reporting the number of " +
                    "tests it would select.\n" +
                    "Note: Run with \"--startsLogging=FINER\" or \"--startsLogging=FINEST\" so that the \"selected-tests\" " +
                    "file, which contains the list of tests that would be run if this option is set to false, will " +
                    "be written to disk."
    )
    public void setRetestAll(String retestAll) {
        this.retestAll = retestAll.equals(TRUE);
    }

    @Input
    public boolean getWriteNonAffected() {
        return this.writeNonAffected;
    }

    @Option(
            option = "writeNonAffected",
            description = "Set this to \"true\" to save nonAffectedTests to a file on disk. This improves the time for " +
                    "updating test dependencies in offline mode by not running computeChangeData() twice. " +
                    "Note: Running with \"--startsLogging=FINEST\" also saves nonAffectedTests to a file on disk."
    )
    public void setWriteNonAffected(String writeNonAffected) {
        this.writeNonAffected = writeNonAffected.equals(TRUE);
    }

    @Input
    public boolean getWriteChangedClasses() {
        return this.writeChangedClasses;
    }

    @Option(
            option = "writeChangedClasses",
            description = "Set this to \"true\" to save changedClasses to a file on disk. " +
                    "Note: Running with \"-DstartsLogging=FINEST\" also saves changedClasses to a file on disk."
    )
    public void setWriteChangedClasses(String writeChangedClasses) {
        this.writeChangedClasses = writeChangedClasses.equals(TRUE);
    }

    public Set<String> getNonAffectedTests() {
        return nonAffectedTests;
    }

    public List<Pair<String, String>> getJarCheckSums() {
        return jarCheckSums;
    }

    public void setJarCheckSums(List<Pair<String, String>> jarCheckSums) {
        this.jarCheckSums = jarCheckSums;
    }

    @TaskAction
    public void execute() {
        try {
            Logger logger = Logger.getGlobal();
            Logger.getGlobal().setLoggingLevel(loggingLevel);
            long start = System.currentTimeMillis();
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
            throw new GradleException(spe.getMessage(), spe.getCause());
        }
    }

    @Internal
    public String getClassesPath() {
        return getClassDir().toString();
    }

    @Internal
    public String getTestClassesPath() {
        return getTestClassDir().toString();
    }

    @Override
    public void dynamicallyUpdateExcludes(List<String> excludePaths) {
        if (excludePaths.isEmpty()) return;
        TaskContainer allTasks = getProject().getTasks();
        for (Task task : allTasks) {
            if (task instanceof Test) {
                Test test = ((Test) task);
                Set<String> excludes = test.getExcludes();
                excludes.addAll(excludePaths);
                test.setExcludes(excludes);
            }
        }
    }

    public void setChangedAndNonaffected() throws StartsPluginException {
        Pair<Set<String>, Set<String>> data = computeChangeData(writeChangedClasses);
        if (data != null) {
            nonAffectedTests = data.getKey();
            changedClasses  = data.getValue();
        } else {
            nonAffectedTests.clear();
            changedClasses.clear();
        }
    }
}
