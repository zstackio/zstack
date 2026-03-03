package org.zstack.compute.legacy;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class ComputeLegacyGlobalProperty {
    @GlobalProperty(name="enable.nv.ram.type.volume", defaultValue = "false")
    public static boolean enableNvRamTypeVolume;
}
