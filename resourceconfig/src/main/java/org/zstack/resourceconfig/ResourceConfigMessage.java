package org.zstack.resourceconfig;

import org.zstack.core.config.GlobalConfig;

public interface ResourceConfigMessage {
    String getName();
    String getCategory();
    default String getIdentity() {
        return GlobalConfig.produceIdentity(getCategory(), getName());
    }
}
