package org.zstack.abstraction;

import java.util.List;

public interface PluginValidator {
    Class<? extends PluginRegister> pluginClass();

    void validate(PluginRegister register);

    void validateAllPlugins(List<PluginRegister> pluginRegisterList);
}
