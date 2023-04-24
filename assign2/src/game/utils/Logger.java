package game.utils;

import java.util.logging.Level;

public class Logger {

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(Logger.class.getName());

    private static final String LOG_FORMAT = "[%s:%d] %s.%s(): %s";

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_ORANGE = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";

    public static void info(String message) {
        log(Level.INFO, ANSI_BLUE, message);
    }

    public static void warning(String message) {
        log(Level.WARNING, ANSI_ORANGE, message);
    }

    public static void error(String message) {
        log(Level.SEVERE, ANSI_RED, message);
    }

    private static void log(Level level, String color, String message) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String className = stackTrace[3].getClassName();
        String methodName = stackTrace[3].getMethodName();
        int lineNumber = stackTrace[3].getLineNumber();
        String logMessage = String.format(LOG_FORMAT, className, lineNumber, className, methodName, message);
        String colorMessage = color + logMessage + ANSI_RESET;
        LOGGER.log(level, colorMessage);
    }
}
