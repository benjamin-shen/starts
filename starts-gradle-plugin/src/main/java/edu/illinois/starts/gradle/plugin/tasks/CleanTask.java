package edu.illinois.starts.gradle.plugin.tasks;

import edu.illinois.starts.helpers.FileUtil;
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
        File directory = new File(getArtifactsDir());
        if (directory.exists()) {
            FileUtil.delete(directory);
        }
    }
}
