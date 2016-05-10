package org.zstack.utils.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CLoggerImpl implements CLogger {
    private final Logger logger;
    private static boolean initialized = false;

    private CLoggerImpl(String name) {
        logger = LogManager.getLogger(name);
    }

    private CLoggerImpl(Class<?> clazz) {
        logger = LogManager.getLogger(clazz);
    }

    /*
    private static void initialize() {
        if (!initialized) {
            String conf = System.getProperty("log4j");
            if (conf == null) {
                conf = "zstack-log4j.xml";
            }
        	URL path = CLoggerImpl.class.getClassLoader().getResource(conf);
        	DOMConfigurator.configure(path);
            initialized = true;
        }
    }
    */

    public static CLogger getLogger(String name) {
        //initialize();
        return new CLoggerImpl(name);
    }

    public static CLogger getLogger(Class<?> clazz) {
        //initialize();
        return new CLoggerImpl(clazz);
    }

    @Override
    public void trace(String msg, Throwable e) {
        logger.trace(msg, e);
    }

    @Override
    public void trace(String msg) {
        logger.trace(msg);
    }

    @Override
    public void debug(String msg, Throwable e) {
        logger.debug(msg, e);
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void info(String msg, Throwable e) {
        logger.info(msg, e);
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warn(String msg, Throwable e) {
        logger.warn(msg, e);
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void error(String msg, Throwable e) {
        logger.error(msg, e);
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }

    @Override
    public void fatal(String msg, Throwable e) {
        logger.fatal(msg, e);
    }

    @Override
    public void fatal(String msg) {
        logger.fatal(msg);
    }

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }
}
