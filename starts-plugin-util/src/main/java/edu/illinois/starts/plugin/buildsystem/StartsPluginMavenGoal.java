package edu.illinois.starts.plugin.buildsystem;

import edu.illinois.starts.helpers.PomUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.plugin.StartsPluginException;
import edu.illinois.starts.plugin.goals.StartsPluginBaseGoal;
import edu.illinois.starts.util.Logger;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.project.MavenProject;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;

/**
 * Wrapper class for STARTS plugin Maven goal
 */
public interface StartsPluginMavenGoal extends StartsPluginBaseGoal {
    ArtifactRepository getLocalRepository();
    void setIncludes(List<String> includes);
    void setExcludes(List<String> excludes);

    default String getLocalRepositoryDir() {
        return getLocalRepository().getBasedir();
    }

    default void setIncludesExcludes() throws StartsPluginException {
        long start = System.currentTimeMillis();
        try {
            Field projectField = AbstractSurefireMojo.class.getDeclaredField("project");
            projectField.setAccessible(true);
            MavenProject accessedProject = (MavenProject) projectField.get(this);
            List<String> includes = PomUtil.getFromPom("include", accessedProject);
            List<String> excludes = PomUtil.getFromPom("exclude", accessedProject);
            Logger.getGlobal().log(Level.FINEST, "@@Excludes: " + excludes);
            Logger.getGlobal().log(Level.FINEST,"@@Includes: " + includes);
            setIncludes(includes);
            setExcludes(excludes);
        } catch (NoSuchFieldException nsfe) {
            nsfe.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        } catch (MojoExecutionException mee) {
            throw new StartsPluginException(mee.getMessage(), mee.getCause());
        }
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(setIncludesExcludes): "
                + Writer.millsToSeconds(end - start));
    }
}
