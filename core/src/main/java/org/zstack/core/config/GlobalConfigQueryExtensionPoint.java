package org.zstack.core.config;

import java.util.List;

public interface GlobalConfigQueryExtensionPoint {
    List<String> queryConfigValidValues();
}
