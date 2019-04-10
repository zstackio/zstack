package org.zstack.resourceconfig;

import org.zstack.core.config.GlobalConfig;

public interface ResourceConfigFacade {
    String SERVICE_ID = ResourceConfigConstant.SERVICE_ID;

    ResourceConfig getResourceConfig(String identity);

    <T> T getResourceConfigValue(GlobalConfig gc, String resourceUuid, Class<T> clz);
}
