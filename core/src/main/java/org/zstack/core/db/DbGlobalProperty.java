package org.zstack.core.db;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class DbGlobalProperty {
    @GlobalProperty(name="entityPackages", defaultValue = "org.zstack")
    public static String ENTITY_PACKAGES;
}
