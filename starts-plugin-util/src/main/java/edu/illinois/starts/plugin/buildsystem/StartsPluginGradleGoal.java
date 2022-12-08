package edu.illinois.starts.plugin.buildsystem;

import edu.illinois.starts.plugin.goals.StartsPluginBaseGoal;
import org.gradle.api.Project;
import org.gradle.api.tasks.Internal;

import java.io.File;
import java.nio.file.Paths;

/**
 * Wrapper class for STARTS plugin Gradle goal
 */
public interface StartsPluginGradleGoal extends StartsPluginBaseGoal {
    @Internal
    Project getProject();
    default File getClassDir() {
        return Paths.get(getProject().getBuildDir().toString(), "classes", "java").toFile();
    }
    default File getTestClassDir() {
        return Paths.get(getClassDir().toString(), "test").toFile();
    }
}
