package org.zstack.storage.ceph;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class DeleteCephVolumeGcGlobalProperty {
    @GlobalProperty(name="deleteCephVolumeGc", defaultValue = "false")
    public static boolean DELETE_CEPH_VOLUME_GC;
}
