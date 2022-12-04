package edu.illinois.starts.plugin.goals;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.illinois.starts.constants.StartsConstants.COMMA;
import static edu.illinois.starts.constants.StartsConstants.JAR_CHECKSUMS;
import static edu.illinois.starts.constants.StartsConstants.SF_CLASSPATH;

public interface StartsPluginRunGoal extends StartsPluginDiffGoal {
    List<String> getCleanClassPath(String cp);

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
        // TODO @bshen refactor
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

    void setJarCheckSums(List<Pair<String, String>> jarCheckSums);
}
