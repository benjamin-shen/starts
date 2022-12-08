package edu.illinois.starts.plugin.goals;

import static edu.illinois.starts.constants.StartsConstants.COMMA;
import static edu.illinois.starts.constants.StartsConstants.JAR_CHECKSUMS;
import static edu.illinois.starts.constants.StartsConstants.PROFILE_STARTS_MOJO_UPDATE_TIME;
import static edu.illinois.starts.constants.StartsConstants.SF_CLASSPATH;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.gradle.api.tasks.Internal;

public interface StartsPluginRunGoal extends StartsPluginDiffGoal {
    boolean isUpdateRunChecksums();

    boolean isRetestAll();

    Set<String> getNonAffectedTests();

    void setChangedAndNonaffected() throws StartsPluginException;

    List<Pair<String, String>> getJarCheckSums();

    void setJarCheckSums(List<Pair<String, String>> jarCheckSums);

    void dynamicallyUpdateExcludes(List<String> excludePaths) throws StartsPluginException;

    @Internal
    String getClassesPath();

    @Internal
    String getTestClassesPath();

    default void run() throws StartsPluginException {
        String cpString = getTestClassPathElementsString();
        List<String> testDependencyElements = getCleanClassPath(cpString);
        if (!isSameClassPath(testDependencyElements) || !hasSameJarChecksum(testDependencyElements)) {
            // Force retestAll because classpath changed since last run
            // don't compute changed and non-affected classes
            dynamicallyUpdateExcludes(new ArrayList<>());
            // Make nonAffected empty so dependencies can be updated
            getNonAffectedTests().clear();
            Writer.writeClassPath(cpString, getArtifactsDir());
            Writer.writeJarChecksums(testDependencyElements, getArtifactsDir(), getJarCheckSums());
        } else if (isRetestAll()) {
            // Force retestAll but compute changes and affected tests
            setChangedAndNonaffected();
            dynamicallyUpdateExcludes(new ArrayList<>());
        } else {
            setChangedAndNonaffected();
            List<String> excludePaths = Writer.fqnsToExcludePath(getNonAffectedTests());
            dynamicallyUpdateExcludes(excludePaths);
        }
        long startUpdateTime = System.currentTimeMillis();
        if (isUpdateRunChecksums()) {
            updateForNextRun(getNonAffectedTests());
        }
        long endUpdateTime = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, PROFILE_STARTS_MOJO_UPDATE_TIME
                + Writer.millsToSeconds(endUpdateTime - startUpdateTime));
    }

    default boolean isSameClassPath(List<String> sfPathString) throws StartsPluginException {
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
                throw new StartsPluginException(SF_CLASSPATH + " is corrupt! Expected only 1 line.");
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

    default boolean hasSameJarChecksum(List<String> cleanSfClassPath) throws StartsPluginException {
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
            List<Pair<String, String>> jarCheckSums = new ArrayList<>();
            for (String path : cleanSfClassPath) {
                Pair<String, String> pair = Writer.getJarToChecksumMapping(path);
                jarCheckSums.add(pair);
                String oldCS = checksumMap.get(pair.getKey());
                noException &= pair.getValue().equals(oldCS);
            }
            setJarCheckSums(jarCheckSums);
        } catch (IOException ioe) {
            noException = false;
            // reset to null because we don't know what/when exception happened
            setJarCheckSums(null);
            ioe.printStackTrace();
        }
        return noException;
    }

    default List<String> getCleanClassPath(String cp) {
        List<String> cpPaths = new ArrayList<>();
        String[] paths = cp.split(File.pathSeparator);
        String classes = getClassesPath();
        String testClasses = getTestClassesPath();
        for (int i = 0; i < paths.length; i++) {
            // TODO: should we also exclude SNAPSHOTS from same project?
            if (paths[i].contains(classes) || paths[i].contains(testClasses)) {
                continue;
            }
            cpPaths.add(paths[i]);
        }
        return cpPaths;
    }
}
