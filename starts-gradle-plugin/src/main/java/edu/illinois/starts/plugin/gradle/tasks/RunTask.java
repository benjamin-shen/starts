package edu.illinois.starts.plugin.gradle.tasks;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.options.Option;
import org.gradle.api.tasks.testing.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Prepares for test runs by writing non-affected tests in the excludesFile.
 */
public class RunTask extends DiffTask {
    public static final String NAME = "startsRun";
    public static final String DESCRIPTION = "Prepares for test runs by writing non-affected tests in the excludesFile.";

    protected boolean updateRunChecksums = true;
    protected boolean retestAll = false;
    protected boolean writeNonAffected = false;
    protected boolean writeChangedClasses = false;
    protected List<Pair> jarCheckSums = null;
    protected List<String> excludePaths = new ArrayList<>();

    @Input
    public boolean getUpdateRunChecksums() {
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
    public boolean getRetestAll() {
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

    @TaskAction
    public void execute() {
        Logger logger = Logger.getGlobal();
        logger.setLoggingLevel(loggingLevel);

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
    }

    protected void run() {
        String cpString = getTestClassPath().toString();
        List<String> testDependencyElements = getCleanClassPath(cpString);
        if (!isSameClassPath(testDependencyElements) || !hasSameJarChecksum(testDependencyElements)) {
            // Force retestAll because classpath changed since last run
            // don't compute changed and non-affected classes
            dynamicallyUpdateExcludes(null);
            // Make nonAffected empty so dependencies can be updated
            nonAffectedTests.clear();
            Writer.writeClassPath(cpString, artifactsDir);
            Writer.writeJarChecksums(testDependencyElements, artifactsDir, jarCheckSums);
        } else if (retestAll) {
            // Force retestAll but compute changes and affected tests
            setChangedAndNonaffected();
            dynamicallyUpdateExcludes(null);
        } else {
            setChangedAndNonaffected();
            excludePaths = Writer.fqnsToExcludePath(nonAffectedTests);
            dynamicallyUpdateExcludes(excludePaths);
        }
        long startUpdateTime = System.currentTimeMillis();
        if (updateRunChecksums) {
            updateForNextRun();
        }
        long endUpdateTime = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, PROFILE_STARTS_MOJO_UPDATE_TIME
                + Writer.millsToSeconds(endUpdateTime - startUpdateTime));
    }

    private List<String> getCleanClassPath(String cp) {
        // TODO refactor
        List<String> cpPaths = new ArrayList<>();
        String[] paths = cp.split(File.pathSeparator);
        String classes = getClassDir().toString();
        for (int i = 0; i < paths.length; i++) {
            // TODO: should we also exclude SNAPSHOTS from same project?
            if (paths[i].contains(classes)) {
                continue;
            }
            cpPaths.add(paths[i]);
        }
        return cpPaths;
    }

    private boolean isSameClassPath(List<String> sfPathString) throws GradleException {
        // TODO refactor
        if (sfPathString.isEmpty()) {
            return true;
        }
        String oldSfPathFileName = Paths.get(getArtifactsDir(), SF_CLASSPATH).toString();
        if (!new File(oldSfPathFileName).exists()) {
            return false;
        }
        try {
            List<String> oldClassPathLines = Files.readAllLines(Paths.get(oldSfPathFileName));
            if (oldClassPathLines.size() != 1) {
                throw new GradleException(SF_CLASSPATH + " is corrupt! Expected only 1 line.");
            }
            List<String> oldClassPathelements = getCleanClassPath(oldClassPathLines.get(0));
            // comparing lists and not sets in case order changes
            if (sfPathString.equals(oldClassPathelements)) {
                return true;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return false;
    }

    private boolean hasSameJarChecksum(List<String> cleanSfClassPath) {
        if (cleanSfClassPath.isEmpty()) {
            return true;
        }
        String oldChecksumPathFileName = Paths.get(getArtifactsDir(), JAR_CHECKSUMS).toString();
        if (!new File(oldChecksumPathFileName).exists()) {
            return false;
        }
        boolean noException = true;
        try {
            List<String> lines = Files.readAllLines(Paths.get(oldChecksumPathFileName));
            Map<String, String> checksumMap = new HashMap<>();
            for (String line : lines) {
                String[] elems = line.split(COMMA);
                checksumMap.put(elems[0], elems[1]);
            }
            jarCheckSums = new ArrayList<>();
            for (String path : cleanSfClassPath) {
                Pair<String, String> pair = Writer.getJarToChecksumMapping(path);
                jarCheckSums.add(pair);
                String oldCS = checksumMap.get(pair.getKey());
                noException &= pair.getValue().equals(oldCS);
            }
        } catch (IOException ioe) {
            noException = false;
            // reset to null because we don't know what/when exception happened
            jarCheckSums = null;
            ioe.printStackTrace();
        }
        return noException;
    }

    private void dynamicallyUpdateExcludes(List<String> excludePaths) {
        if (excludePaths == null) return;
        TaskContainer allTasks = getProject().getTasks();
        for (Task task : allTasks) {
            if (task instanceof Test) {
                ((Test) task).setExcludes(excludePaths);
            }
        }
    }

    protected void setChangedAndNonaffected() {
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
