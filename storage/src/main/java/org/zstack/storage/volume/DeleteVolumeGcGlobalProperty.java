package org.zstack.storage.volume;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class DeleteVolumeGcGlobalProperty {
    @GlobalProperty(name="deleteVolumeGc", defaultValue = "ture")
    public static boolean DELETE_VOLUME_GC;
}
