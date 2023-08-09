package org.zstack.abstraction;

import org.zstack.abstraction.sns.EndpointDriver;

import java.util.List;
import java.util.stream.Collectors;

public class PluginEndpointValidator implements PluginValidator {
    @Override
    public Class<? extends PluginDriver> pluginClass() {
        return EndpointDriver.class;
    }

    @Override
    public void validate(PluginDriver register) {
        if (register.type() == null) {
            throw new InvalidPluginDefinitionException();
        }
    }

    @Override
    public void validateAllPlugins(List<PluginDriver> pluginDriverList) {
        int pluginNumber = pluginDriverList.size();

        int distinctPluginByEndpointTypeNumber = pluginDriverList
                .stream()
                .map(PluginDriver::type)
                .collect(Collectors.toSet()).size();

        if (pluginNumber == distinctPluginByEndpointTypeNumber) {
            return;
        }

        throw new InvalidPluginDefinitionException();
    }
}
