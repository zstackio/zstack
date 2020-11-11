package org.zstack.utils;

import groovy.util.logging.Slf4j;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.pattern.RegexReplacement;

@Slf4j
@Plugin(name = "replaces", category = "Core", printObject = true)
public class MaskSensitiveRegexReplaces {

    private final RegexReplacement[] replaces;

    private MaskSensitiveRegexReplaces(RegexReplacement[] replaces) {
        this.replaces = replaces;
    }


    public String format(String msg) {
        if (msg.contains("Pass") || msg.contains("pass")) {
            for (RegexReplacement replace : replaces) {
                msg = replace.format(msg);
            }
        }
        return msg;
    }


    @PluginFactory
    public static MaskSensitiveRegexReplaces createRegexReplacement(
            @PluginElement("replaces") final RegexReplacement[] replaces) {
        if (replaces == null) {
            return null;
        }

        return new MaskSensitiveRegexReplaces(replaces);
    }
}
