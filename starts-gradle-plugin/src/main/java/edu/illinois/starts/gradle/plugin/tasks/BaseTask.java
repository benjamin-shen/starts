package edu.illinois.starts.gradle.plugin.tasks;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.enums.DependencyFormat;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.options.Option;

import java.io.File;
import java.nio.file.Paths;
import java.util.logging.Level;

public class BaseTask extends DefaultTask implements StartsConstants {
    private boolean filterLib = true;
    private boolean useThirdParty = false;
    private DependencyFormat depFormat = DependencyFormat.ZLC;
    private String graphCache;
    private boolean printGraph = true;
    private String graphFile = GRAPH;
    private Level loggingLevel = Level.CONFIG;
    /**
     * The directory in which to store STARTS artifacts that are needed between runs.
     */
    @Internal
    private String artifactsDir;

    @Input
    public boolean getFilterLib() {
        return this.filterLib;
    }

    @Option(
        option = "filterLib",
        description = "Set this to \"false\" to not filter out \"sun.*\" and \"java.*\" classes from jdeps parsing."
    )
    public void setFilterLib(String filterLib) {
        this.filterLib = !filterLib.equals(FALSE);
    }

    @Input
    public boolean getUseThirdParty() {
        return this.useThirdParty;
    }

    @Option(
            option = "useThirdParty",
            description = "Set this to \"false\" to not add jdeps edges from 3rd party-libraries."
    )
    public void setUseThirdParty(String useThirdParty) {
        this.useThirdParty = !useThirdParty.equals(FALSE);
    }

    public String getArtifactsDir() throws GradleException {
        if (artifactsDir == null) {
            artifactsDir = Paths.get(getProject().getRootDir().getAbsolutePath(), STARTS_DIRECTORY_PATH).toString();
            File file = new File(artifactsDir);
            if (!file.mkdirs() && !file.exists()) {
                throw new GradleException("I could not create artifacts dir: " + artifactsDir);
            }
        }
        return artifactsDir;
    }

    @Input
    public DependencyFormat getDepFormat() {
        return this.depFormat;
    }

    @Option(
            option = "depFormat",
            description = "Allows to switch the format in which we want to store the test dependencies. " +
                    "A full list of what we currently support can be found in edu.illinois.starts.enums.DependencyFormat"
    )
    public void setDepFormat(String depFormat) {
        this.depFormat = DependencyFormat.valueOf(depFormat);
    }

    @Input
    public String getGraphCache() {
        if (this.graphCache == null) {
            this.graphCache = Paths.get(getProject().getRootDir().getAbsolutePath(), JDEPS_CACHE).toString();
        }
        return this.graphCache;
    }

    @Option(
            option = "gCache",
            description = "Path to directory that contains the result of running jdeps on third-party " +
                    "and standard library jars that an application may need, e.g., those in M2_REPO."
    )
    public void setGraphCache(String graphCache) {
        this.graphCache = graphCache;
    }

    @Input
    public boolean getPrintGraph() {
        return this.printGraph;
    }

    @Option(
            option = "printGraph",
            description = "Set this to \"false\" to not print the graph obtained from jdeps parsing. " +
                    "When \"true\" the graph is written to file after the run."
    )
    public void setPrintGraph(String printGraph) {
        this.printGraph = !printGraph.equals(FALSE);
    }

    @Input
    public String getGraphFile() {
        return this.graphFile;
    }

    @Option(
            option = "graphFile",
            description = "Output filename for the graph, if printGraph == true."
    )
    public void setGraphFile(String graphFile) {
        this.graphFile = graphFile;
    }

    @Input
    public Level getLoggingLevel() {
        return this.loggingLevel;
    }

    @Option(
            option = "startsLogging",
            description = "Log levels as defined in java.util.logging.Level."
    )
    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = Level.parse(loggingLevel);
    }
}
