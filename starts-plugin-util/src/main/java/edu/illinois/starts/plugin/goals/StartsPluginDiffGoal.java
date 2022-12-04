package edu.illinois.starts.plugin.goals;

import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.EkstaziHelper;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.helpers.ZLCHelper;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import static edu.illinois.starts.constants.StartsConstants.CHANGED_CLASSES;
import static edu.illinois.starts.constants.StartsConstants.EMPTY;

public interface StartsPluginDiffGoal extends StartsPluginBaseGoal {
    boolean isCleanBytes();
    boolean isUpdateDiffChecksums();

    default void computeDiff() throws StartsPluginException {
        Pair<Set<String>, Set<String>> data = computeChangeData(false);
        String extraText = EMPTY;
        Set<String> nonAffected = new HashSet<>();
        Set<String> changedClasses = new HashSet<>();
        if (data != null) {
            nonAffected = data.getKey();
            changedClasses = data.getValue();
        } else {
            extraText = " (no RTS artifacts; likely the first run)";
        }
        printResult(changedClasses, "ChangedClasses" + extraText);
        if (isUpdateDiffChecksums()) {
            updateForNextRun(nonAffected);
        }
    }

    void updateForNextRun(Set<String> nonAffectedTests) throws StartsPluginException;

    default Pair<Set<String>, Set<String>> computeChangeData(boolean writeChanged) throws StartsPluginException {
        long start = System.currentTimeMillis();
        Pair<Set<String>, Set<String>> data = null;
        if (getDepFormat() == DependencyFormat.ZLC) {
            data = ZLCHelper.getChangedData(getArtifactsDir(), isCleanBytes());
        } else if (getDepFormat() == DependencyFormat.CLZ) {
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
}
