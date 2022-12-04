package edu.illinois.starts.plugin.gradle.tasks;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.plugin.goals.StartsPluginStartsGoal;
import edu.illinois.starts.util.Logger;
import org.gradle.api.tasks.TaskAction;

import java.util.logging.Level;

/**
 * Invoked after running selected tests.
 */
public class StartsTask extends RunTask implements StartsPluginStartsGoal {
    public static final String NAME = "starts";
    public static final String DESCRIPTION = "Invoked after running selected tests.";

    @TaskAction
    public void execute() {
        long endOfRunMojo = Long.parseLong(System.getProperty(PROFILE_END_OF_RUN_MOJO));
        Logger logger = Logger.getGlobal();
        logger.setLoggingLevel(loggingLevel);
        logger = Logger.getGlobal();
        long end = System.currentTimeMillis();
        logger.log(Level.FINE, PROFILE_TEST_RUNNING_TIME + Writer.millsToSeconds(end - endOfRunMojo));
        logger.log(Level.FINE, "[PROFILE] STARTS-MOJO-TOTAL: " + Writer.millsToSeconds(end - endOfRunMojo));
    }
}
