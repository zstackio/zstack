package org.zstack.resourceconfig;

import org.zstack.core.config.GlobalConfig;

import java.util.List;

public interface ResourceConfigFacade {
    String SERVICE_ID = ResourceConfigConstant.SERVICE_ID;

    ResourceConfig getResourceConfig(String identity);

    <T> T getResourceConfigValue(GlobalConfig gc, String resourceUuid, Class<T> clz);

    List<ResourceConfigVO> getResourceConfigValues(String name, String category, List<String> resourceUuids);
}
