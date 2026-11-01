package com.tradewind.bootstrap;

/** Fatal condition during startup; the message is printed to the console verbatim. */
public final class PluginStartupException extends Exception {

    private static final long serialVersionUID = 1L;

    public PluginStartupException(String message) {
        super(message);
    }

    public PluginStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}

