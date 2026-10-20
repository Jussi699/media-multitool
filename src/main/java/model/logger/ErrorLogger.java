package model.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorLogger {
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    /**
     * Logs a formatted message with an error code and throwable at the specified level.
     * <p>
     * The caller class is determined at runtime by walking the call stack,
     * which introduces significant performance overhead.
     *
     * @param errorCode the error code to format into the message
     * @param level the logging level (DEBUG, INFO, WARN, ERROR)
     * @param message the message to log
     * @param t the throwable associated with the log event, or null
     * @deprecated This method incurs a performance penalty due to stack walking.
     *             Use {@link #log(Class, int, Level, String, Throwable)} instead.
     */
    @Deprecated
    public static void log(int errorCode, Level level, String message, Throwable t) {
        String logMessage = String.format("[%d] %s", errorCode, message);
        Logger logger = getCallerLogger();

        switch (level) {
            case DEBUG -> logger.debug(logMessage, t);
            case INFO -> logger.info(logMessage, t);
            case WARN -> logger.warn(logMessage, t);
            case ERROR -> logger.error(logMessage, t);
        }
    }

    /**
     * Logs a formatted message with an error code and throwable at the specified level for the given class.
     *
     * @param sourceClass the class from which the log event originates
     * @param errorCode the error code to format into the message
     * @param level the logging level (DEBUG, INFO, WARN, ERROR)
     * @param message the message to log
     * @param t the throwable associated with the log event, or null
     */
    public static void log(Class<?> sourceClass, int errorCode, Level level, String message, Throwable t) {
        String logMessage = String.format("[%d] %s", errorCode, message);
        Logger logger = LoggerFactory.getLogger(sourceClass);

        switch (level) {
            case DEBUG -> logger.debug(logMessage, t);
            case INFO -> logger.info(logMessage, t);
            case WARN -> logger.warn(logMessage, t);
            case ERROR -> logger.error(logMessage, t);
        }
    }

    /**
     * Logs an informational message at the INFO level.
     * <p>
     * The caller class is determined at runtime by walking the call stack,
     * which introduces significant performance overhead.
     *
     * @param message the message to log
     * @deprecated This method incurs a performance penalty due to stack walking.
     *             Use {@link #info(Class, String)} instead, or create a logger
     *             via {@link org.slf4j.LoggerFactory#getLogger(Class)}.
     */
    @Deprecated
    public static void info(String message) {
        getCallerLogger().info(message);
    }

    /**
     * Logs an informational message at the INFO level for the specified class.
     *
     * @param sourceClass the class from which the log event originates
     * @param message the message to log
     */
    public static void info(Class<?> sourceClass, String message) {
        LoggerFactory.getLogger(sourceClass).info(message);
    }

    /**
     * Logs a warning message at the WARN level.
     * <p>
     * The caller class is determined at runtime by walking the call stack,
     * which introduces significant performance overhead.
     *
     * @param message the message to log
     * @deprecated This method incurs a performance penalty due to stack walking.
     *             Use {@link #warn(Class, String)} instead, or create a logger
     *             via {@link org.slf4j.LoggerFactory#getLogger(Class)}.
     */
    @Deprecated
    public static void warn(String message) {
        getCallerLogger().warn(message);
    }

    /**
     * Logs a warning message at the WARN level for the specified class.
     *
     * @param sourceClass the class from which the log event originates
     * @param message the message to log
     */
    public static void warn(Class<?> sourceClass, String message) {
        LoggerFactory.getLogger(sourceClass).warn(message);
    }

    /**
     * Logs an error message at the ERROR level.
     * <p>
     * The caller class is determined at runtime by walking the call stack,
     * which introduces significant performance overhead.
     *
     * @param message the message to log
     * @deprecated This method incurs a performance penalty due to stack walking.
     *             Use {@link #error(Class, String)} instead, or create a logger
     *             via {@link org.slf4j.LoggerFactory#getLogger(Class)}.
     */
    @Deprecated
    public static void error(String message) {
        getCallerLogger().error(message);
    }

    /**
     * Logs an error message at the ERROR level for the specified class.
     *
     * @param sourceClass the class from which the log event originates
     * @param message the message to log
     */
    public static void error(Class<?> sourceClass, String message) {
        LoggerFactory.getLogger(sourceClass).error(message);
    }

    /**
     * Logs an error message and associated throwable at the ERROR level.
     * <p>
     * The caller class is determined at runtime by walking the call stack,
     * which introduces significant performance overhead.
     *
     * @param message the message to log
     * @param throwable the throwable associated with the error
     * @deprecated This method incurs a performance penalty due to stack walking.
     *             Use {@link #error(Class, String, Throwable)} instead, or create a logger
     *             via {@link org.slf4j.LoggerFactory#getLogger(Class)}.
     */
    @Deprecated
    public static void error(String message, Throwable throwable) {
        getCallerLogger().error(message, throwable);
    }

    /**
     * Logs an error message and associated throwable at the ERROR level for the specified class.
     *
     * @param sourceClass the class from which the log event originates
     * @param message the message to log
     * @param throwable the throwable associated with the error
     */
    public static void error(Class<?> sourceClass, String message, Throwable throwable) {
        LoggerFactory.getLogger(sourceClass).error(message, throwable);
    }

    private static Logger getCallerLogger() {
        String callerClassName = STACK_WALKER.walk(frames -> frames
                .map(frame -> frame.getDeclaringClass().getName())
                .filter(className -> !className.equals(ErrorLogger.class.getName()))
                .findFirst()
                .orElse(ErrorLogger.class.getName()));
        return LoggerFactory.getLogger(callerClassName);
    }
}
