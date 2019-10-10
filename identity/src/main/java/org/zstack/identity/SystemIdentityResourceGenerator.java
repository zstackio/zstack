package org.zstack.identity;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SystemIdentityResourceGenerator implements PrepareDbInitialValueExtensionPoint {
    @Autowired
    private PluginRegistry pluginRgty;

    public void generate() {
        if (StringUtils.isEmpty(IdentityGlobalProperty.IDENTITY_INIT_TYPE)) {
            for (IdentityResourceGenerateExtensionPoint ext : pluginRgty.getExtensionList(IdentityResourceGenerateExtensionPoint.class)) {
                ext.prepareResources();
            }

            return;
        }

        Map<String, List<IdentityResourceGenerateExtensionPoint>> extensionMap = new HashMap<>();
        for (IdentityResourceGenerateExtensionPoint ext : pluginRgty.getExtensionList(IdentityResourceGenerateExtensionPoint.class)) {
            extensionMap.putIfAbsent(ext.getIdentityType(), new ArrayList<>());
            extensionMap.get(ext.getIdentityType()).add(ext);
        }

        for (String identityInitType : IdentityGlobalProperty.IDENTITY_INIT_TYPE.split(",")) {
            identityInitType = identityInitType.trim();
            List<IdentityResourceGenerateExtensionPoint> extensionPoints = extensionMap.get(identityInitType);

            if (extensionPoints != null && !extensionPoints.isEmpty()) {
                for (IdentityResourceGenerateExtensionPoint ext : extensionPoints) {
                    ext.prepareResources();
                }
            } else {
                throw new CloudRuntimeException(String.format("Unknown recognized identity init type %s", identityInitType));
            }
        }
    }

    @Override
    public void prepareDbInitialValue() {
        generate();
    }
}
