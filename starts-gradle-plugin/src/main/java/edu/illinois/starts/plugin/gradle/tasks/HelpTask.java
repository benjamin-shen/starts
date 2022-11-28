package edu.illinois.starts.plugin.gradle.tasks;

import org.codehaus.groovy.reflection.ReflectionUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.util.Scanner;

/**
 * Displays help message for STARTS plugin.
 */
public class HelpTask extends DefaultTask {
    public static final String NAME = "startsHelp";
    public static final String DESCRIPTION = "Displays help message for STARTS plugin.";

    @TaskAction
    void printHelp() {
        String helpMessage = new Scanner(ReflectionUtils.getCallingClass(0).getResourceAsStream("/startsHelp.txt"), "UTF-8").useDelimiter("\\A").next();
        System.out.println(helpMessage);
    }
}
