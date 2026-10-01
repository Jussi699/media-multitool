package model.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorLogger {
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

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

    public static void info(String message) {
        getCallerLogger().info(message);
    }

    public static void info(Class<?> sourceClass, String message) {
        LoggerFactory.getLogger(sourceClass).info(message);
    }

    public static void warn(String message) {
        getCallerLogger().warn(message);
    }

    public static void warn(Class<?> sourceClass, String message) {
        LoggerFactory.getLogger(sourceClass).warn(message);
    }

    public static void error(String message) {
        getCallerLogger().error(message);
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
