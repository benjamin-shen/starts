/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package edu.illinois.starts.gradle.plugin;

import edu.illinois.starts.gradle.plugin.tasks.CleanTask;
import edu.illinois.starts.gradle.plugin.tasks.DiffTask;
import edu.illinois.starts.gradle.plugin.tasks.HelpTask;
import edu.illinois.starts.gradle.plugin.tasks.RunTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

/**
 * STARTS Gradle Plugin
 */
public class StartsPlugin implements Plugin<Project> {
    private final String STARTS_GROUP = "starts";

    public void apply(Project project) {
        Task helpTask = project.getTasks().create(HelpTask.NAME, HelpTask.class);
        helpTask.setDescription(HelpTask.DESCRIPTION);
        helpTask.setGroup(STARTS_GROUP);

        Task cleanTask = project.getTasks().create(CleanTask.NAME, CleanTask.class);
        cleanTask.setDescription(CleanTask.DESCRIPTION);
        cleanTask.setGroup(STARTS_GROUP);

        Task diffTask = project.getTasks().create(DiffTask.NAME, DiffTask.class);
        diffTask.setDescription(DiffTask.DESCRIPTION);
        diffTask.setGroup(STARTS_GROUP);
        diffTask.dependsOn(project.getTasksByName("testClasses", false));

        Task runTask = project.getTasks().create(RunTask.NAME, RunTask.class);
        runTask.setDescription(RunTask.DESCRIPTION);
        runTask.setGroup(STARTS_GROUP);

    }
}
