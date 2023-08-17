package org.zstack.resourceconfig;

import org.zstack.core.config.GlobalConfig;

import java.util.List;
import java.util.Map;

public interface ResourceConfigFacade {
    String SERVICE_ID = ResourceConfigConstant.SERVICE_ID;

    ResourceConfig getResourceConfig(String identity);

    <T> T getResourceConfigValue(GlobalConfig gc, String resourceUuid, Class<T> clz);

    <T> Map<String, T> getResourceConfigValues(GlobalConfig gc, List<String> resourceUuids, Class<T> clz);
}
