package org.zstack.abstraction.sns;

import org.zstack.abstraction.InvalidPluginDefinitionException;
import org.zstack.abstraction.PluginRegister;
import org.zstack.abstraction.PluginValidator;

import java.util.List;
import java.util.stream.Collectors;

public class PluginEndpointValidator implements PluginValidator {
    @Override
    public Class<? extends PluginRegister> pluginClass() {
        return PluginEndpointSender.class;
    }

    @Override
    public void validate(PluginRegister register) {
        PluginEndpointSender sender = (PluginEndpointSender) register;

        if (sender.endpointType() == null) {
            throw new InvalidPluginDefinitionException();
        }
    }

    @Override
    public void validateAllPlugins(List<PluginRegister> pluginRegisterList) {
        int pluginNumber = pluginRegisterList.size();

        int distinctPluginByEndpointTypeNumber = pluginRegisterList
                .stream()
                .map(plugin -> ((PluginEndpointSender) plugin).endpointType())
                .collect(Collectors.toSet()).size();

        if (pluginNumber == distinctPluginByEndpointTypeNumber) {
            return;
        }

        throw new InvalidPluginDefinitionException();
    }
}
