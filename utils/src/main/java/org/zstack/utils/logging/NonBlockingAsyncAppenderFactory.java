package org.zstack.utils.logging;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

@Plugin(name = "NonBlockingAsync", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public abstract class NonBlockingAsyncAppenderFactory extends AbstractAppender {
    // Log4j checks the factory class's Appender type before accepting its return value.
    private NonBlockingAsyncAppenderFactory() {
        super("non-blocking-async-factory", null, null, true, Property.EMPTY_ARRAY);
    }

    private static final ErrorHandler ERROR_HANDLER = new ErrorHandler() {
        @Override
        public void error(String message) {
        }

        @Override
        public void error(String message, Throwable error) {
        }

        @Override
        public void error(String message, LogEvent event, Throwable error) {
        }
    };

    @PluginFactory
    public static AsyncAppender createAppender(@PluginAttribute("name") @Required String name,
                                               @PluginAttribute("errorRef") @Required String errorRef,
                                               @PluginAttribute(value = "bufferSize", defaultInt = 512) int bufferSize,
                                               @PluginElement("AppenderRef") AppenderRef[] appenderRefs,
                                               @PluginElement("Filter") Filter filter,
                                               @PluginConfiguration Configuration configuration) {
        AsyncAppender.Builder<?> builder = AsyncAppender.newBuilder();
        builder.setFilter(filter);
        AsyncAppender appender = builder.setName(name).setAppenderRefs(appenderRefs)
                .setErrorRef(errorRef).setBufferSize(bufferSize).setBlocking(false)
                .setIgnoreExceptions(true).setConfiguration(configuration).build();
        // errorRef counts overflow; the default handler writes stderr in the caller.
        appender.setHandler(ERROR_HANDLER);
        return appender;
    }
}
