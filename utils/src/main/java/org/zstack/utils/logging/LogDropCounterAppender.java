package org.zstack.utils.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import javax.management.JMException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

@Plugin(name = "LogDropCounter", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class LogDropCounterAppender extends AbstractAppender implements LogDropCounterMBean {
    private final LongAdder dropped = new LongAdder();
    private final LongAdder importantDropped = new LongAdder();
    private final ObjectName objectName;
    private volatile long lastDropTimestamp;

    private LogDropCounterAppender(String name, Configuration configuration) throws JMException {
        super(name, null, null, true, Property.EMPTY_ARRAY);
        objectName = new ObjectName("org.zstack.logging:type=LogDropCounter,context=" +
                ObjectName.quote(String.valueOf(configuration.getName())) + ",name=" + ObjectName.quote(name) +
                ",id=" + System.identityHashCode(this));
    }

    @PluginFactory
    public static LogDropCounterAppender createAppender(@PluginAttribute("name") String name,
                                                       @PluginConfiguration Configuration configuration) throws JMException {
        return new LogDropCounterAppender(name, configuration);
    }

    @Override
    public void start() {
        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(
                    new StandardMBean(this, LogDropCounterMBean.class), objectName);
        } catch (JMException e) {
            LOGGER.error("Cannot register log drop counter {}", objectName, e);
        }
        super.start();
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        try {
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName);
        } catch (JMException e) {
            LOGGER.error("Cannot unregister log drop counter {}", objectName, e);
        }
        return super.stop(timeout, timeUnit);
    }

    @Override
    public void append(LogEvent event) {
        dropped.increment();
        if (event.getLevel().isMoreSpecificThan(Level.WARN)) {
            importantDropped.increment();
        }
        lastDropTimestamp = System.currentTimeMillis();
    }

    @Override
    public long getDroppedCount() {
        return dropped.sum();
    }

    @Override
    public long getImportantDroppedCount() {
        return importantDropped.sum();
    }

    @Override
    public long getLastDropTimestamp() {
        return lastDropTimestamp;
    }

    public ObjectName getObjectName() {
        return objectName;
    }
}
