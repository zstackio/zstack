package org.zstack.core.config.resourceconfig;

import org.zstack.core.config.GlobalConfig;

public interface ResourceConfigFacade {
    String SERVICE_ID = "resourceConfig";

    <T> T getResourceConfigValue(GlobalConfig gc, String resourceUuid, Class<T> clz);
}
