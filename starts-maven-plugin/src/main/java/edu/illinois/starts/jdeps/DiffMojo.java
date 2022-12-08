/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.plugin.goals.StartsPluginDiffGoal;
import edu.illinois.starts.util.Logger;
import lombok.Getter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Finds types that have changed since the last time they were analyzed.
 */
@Mojo(name = "diff", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class DiffMojo extends BaseMojo implements StartsPluginDiffGoal {
    /**
     * Set this to "false" to disable smart hashing, i.e., to *not* strip
     * Bytecode files of debug info prior to computing checksums. See the "Smart
     * Checksums" Sections in the Ekstazi paper:
     * http://dl.acm.org/citation.cfm?id=2771784
     */
    @Parameter(property = "cleanBytes", defaultValue = TRUE)
    @Getter
    protected boolean cleanBytes;

    /**
     * Set this to "true" to update test dependencies on disk. The default value of "false"
     * is useful for "dry runs" where one may want to see the diff without updating
     * the test dependencies.
     */
    @Parameter(property = "updateDiffChecksums", defaultValue = FALSE)
    @Getter
    private boolean updateDiffChecksums;

    public void execute() throws MojoExecutionException {
        try {
            Logger.getGlobal().setLoggingLevel(loggingLevel);
            computeDiff();
        } catch (StartsPluginException spe) {
            throw new MojoExecutionException(spe.getMessage(), spe.getCause());
        }
    }

    public ClassLoader getClassLoader() {
        return createClassLoader(sureFireClassPath);
    }

    @Override
    public void setIncludesExcludes() throws StartsPluginException {
        super.setIncludesExcludes();
    }
}

