package edu.illinois.starts.gradle.plugin.tasks;

import org.codehaus.groovy.reflection.ReflectionUtils;
import org.gradle.api.tasks.TaskAction;

/**
 * Removes STARTS plugin artifacts.
 */
public class CleanTask extends BaseTask {
    public static final String NAME = "startsClean";

    @TaskAction
    void printHelloWorld() {
        System.out.println("hello world");
    }
}
