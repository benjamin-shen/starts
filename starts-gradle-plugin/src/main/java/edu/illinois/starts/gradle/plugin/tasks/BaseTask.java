package edu.illinois.starts.gradle.plugin.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.options.Option;

import edu.illinois.starts.constants.StartsConstants;

abstract class BaseTask extends DefaultTask implements StartsConstants {

    private String filterLib = TRUE;

    /**
     * Set this to "false" to not filter out "sun.*" and "java.*" classes from jdeps parsing.
     */
    @Option(option = "filterLib", description = "hello world")
    public void setFilterLib(String filterLib) {
        this.filterLib = filterLib;
    }

    /**
     * Blah
     */
    @Input
    public String getFilterLib() {
        return this.filterLib;
    }
}
