/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.helpers.FileUtil;
import edu.illinois.starts.plugin.StartsPluginException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;

/**
 * Removes STARTS plugin artifacts.
 */
@Mojo(name = "clean", requiresDirectInvocation = true)
public class CleanMojo extends BaseMojo {
    public void execute() throws MojoExecutionException {
        try {
            File directory = new File(getArtifactsDir());
            if (directory.exists()) {
                FileUtil.delete(directory);
            }
        } catch (StartsPluginException spe) {
            throw new MojoExecutionException(spe.getMessage(), spe.getCause());
        }
    }
}
