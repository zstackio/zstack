package org.zstack.abstraction;

import java.util.List;

public interface PluginValidator {
    Class<? extends PluginDriver> pluginClass();

    void validate(PluginDriver register);

    void validateAllPlugins(List<PluginDriver> pluginRegisterList);
}
