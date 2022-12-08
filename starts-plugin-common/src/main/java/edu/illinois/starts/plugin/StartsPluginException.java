package edu.illinois.starts.plugin;

public class StartsPluginException extends Exception {
    public StartsPluginException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public StartsPluginException(String errorMessage) {
        super(errorMessage);
    }
}
