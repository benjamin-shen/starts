package edu.illinois.starts.plugin.gradle.tasks;

import edu.illinois.starts.helpers.FileUtil;
import edu.illinois.starts.plugin.StartsPluginException;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

/**
 * Removes STARTS plugin artifacts.
 */
public class CleanTask extends BaseTask {
    public static final String NAME = "startsClean";
    public static final String DESCRIPTION = "Removes STARTS plugin artifacts.";

    @TaskAction
    public void cleanArtifacts() {
        try {
            File directory = new File(getArtifactsDir());
            if (directory.exists()) {
                FileUtil.delete(directory);
            }
        } catch (StartsPluginException spe) {
            throw new GradleException(spe.getMessage(), spe.getCause());
        }
    }
}
