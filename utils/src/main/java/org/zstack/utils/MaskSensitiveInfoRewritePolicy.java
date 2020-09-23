package org.zstack.utils;

/**
 * Created by MaJin on 2019/10/8.
 */

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ReusableObjectMessage;
import org.apache.logging.log4j.message.ReusableSimpleMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Strings;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Plugin(name = "MaskSensitiveInfoRewritePolicy", category = Core.CATEGORY_NAME, elementType = "rewritePolicy", printObject = true)
public final class MaskSensitiveInfoRewritePolicy implements RewritePolicy {
    static Set<String> loggerNames = Collections.synchronizedSet(new HashSet<>());

    private static EncryptRSAUtil rsa = null;

    public static void initKey(String key) {
        rsa = Strings.isEmpty(key) ? null : new EncryptRSAUtil(key);
    }

    @Override
    public LogEvent rewrite(final LogEvent event) {
        if (rsa == null && !loggerNames.contains(event.getLoggerName())) {
            return event;
        }

        if (event.getMessage() instanceof ObjectMessage) {
            Log4jLogEvent.Builder builder = new Log4jLogEvent.Builder(event);
            builder.setMessage(new SimpleMessage(encryptLog(event.getMessage().getFormattedMessage(), event.getLoggerName())));
            return builder.build();
        } else if (event.getMessage() instanceof SimpleMessage) {
            Log4jLogEvent.Builder builder = new Log4jLogEvent.Builder(event);
            builder.setMessage(new SimpleMessage(encryptLog(event.getMessage().getFormattedMessage(), event.getLoggerName())));
            return builder.build();
        } else if (event.getMessage() instanceof ReusableObjectMessage) {
            ReusableObjectMessage msg = (ReusableObjectMessage) event.getMessage();
            msg.set(encryptLog(msg.getFormattedMessage(), event.getLoggerName()));
            return event;
        } else if (event.getMessage() instanceof ReusableSimpleMessage) {
            ReusableSimpleMessage msg = (ReusableSimpleMessage) event.getMessage();
            msg.set(encryptLog(msg.getFormattedMessage(), event.getLoggerName()));
            return event;
        } else if (rsa != null) {
            Log4jLogEvent.Builder builder = new Log4jLogEvent.Builder(event);
            builder.setMessage(new SimpleMessage(encryptLog(event.getMessage().getFormattedMessage(), event.getLoggerName())));
            return builder.build();
        } else {
            return event;
        }
    }

    private String encryptLog(String raw, String logName) {
        if (loggerNames.contains(logName)) {
            raw = Utils.maskSensitiveInfo(raw);
        }

        if (rsa != null) {
            raw = rsa.encrypt(raw);
        }
        return raw;
    }

    @PluginFactory
    public static MaskSensitiveInfoRewritePolicy createPolicy() {
        return new MaskSensitiveInfoRewritePolicy();
    }
}