package org.zstack.test.core.config;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;

/**
 */
@GlobalConfigDefinition
public class GlobalConfigForTest {
    public static final String CATEGORY = "Test";

    public static GlobalConfig TEST = new GlobalConfig(CATEGORY, "Test");
    public static GlobalConfig TEST2 = new GlobalConfig("Others", "Test2");
    public static GlobalConfig TEST3 = new GlobalConfig(CATEGORY, "Test3");
    public static GlobalConfig TEST4 = new GlobalConfig(CATEGORY, "Test4");
}
