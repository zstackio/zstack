package org.zstack.storage.primary;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/5/10.
 */
@GlobalPropertyDefinition
public class PrimaryStorageGlobalProperty {
    @GlobalProperty(name="PrimaryStorage.capacityTrackerOn", defaultValue = "false")
    public static boolean CAPACITY_TRACKER_ON;
}
