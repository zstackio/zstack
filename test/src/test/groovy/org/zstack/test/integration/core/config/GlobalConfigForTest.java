package org.zstack.test.integration.core.config;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;

/**
 */
@GlobalConfigDefinition
public class GlobalConfigForTest {
    public static final String CATEGORY = "Test";

    public static GlobalConfig TEST_GlobalConfig1_String = new GlobalConfig(CATEGORY, "TestString");
    public static GlobalConfig TEST_GlobalConfig2_Boolean = new GlobalConfig(CATEGORY, "TestBoolean");
}
