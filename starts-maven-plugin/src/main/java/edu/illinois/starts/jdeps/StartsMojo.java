/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.plugin.goals.StartsPluginStartsGoal;
import edu.illinois.starts.util.Logger;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.logging.Level;

/**
 * Invoked after running selected tests (see lifecycle.xml for details).
 */
@Mojo(name = "starts", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST, lifecycle = "starts")
public class StartsMojo extends RunMojo implements StartsPluginStartsGoal {
    public void execute() {
        long endOfRunMojo = Long.parseLong(System.getProperty(PROFILE_END_OF_RUN_MOJO));
        Logger logger = Logger.getGlobal();
        Logger.getGlobal().setLoggingLevel(loggingLevel);
        long end = System.currentTimeMillis();
        logger.log(Level.FINE, PROFILE_TEST_RUNNING_TIME + Writer.millsToSeconds(end - endOfRunMojo));
        logger.log(Level.FINE, "[PROFILE] STARTS-MOJO-TOTAL: " + Writer.millsToSeconds(end - endOfRunMojo));
    }
}
