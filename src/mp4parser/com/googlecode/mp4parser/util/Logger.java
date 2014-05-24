package com.googlecode.mp4parser.util;

public abstract class Logger {

    public static Logger getLogger(Class clz) {
        return new JuliLogger(clz.getSimpleName());
    }

    public abstract void logDebug(String message);

    public abstract void logWarn(String message);

    public abstract void logError(String message);
}
