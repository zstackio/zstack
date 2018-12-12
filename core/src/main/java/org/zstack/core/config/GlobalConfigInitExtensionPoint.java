package org.zstack.core.config;

import java.util.List;

public interface GlobalConfigInitExtensionPoint {
    List<String> getPredefinedGlobalConfigCategories();
}
