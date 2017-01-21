package org.zstack.core.timeout;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/3/26.
 */
@GlobalPropertyDefinition
public class ApiTimeoutGlobalProperty {
    @GlobalProperty(name="ApiTimeout.org.zstack.header.image.APIAddImageMsg", defaultValue = "timeout::3h")
    public static String APIAddImageMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg", defaultValue = "timeout::3h")
    public static String APICreateRootVolumeTemplateFromRootVolumeMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeMsg", defaultValue = "timeout::3h")
    public static String APICreateDataVolumeTemplateFromVolumeMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.volume.APICreateDataVolumeFromVolumeSnapshotMsg", defaultValue = "timeout::3h")
    public static String APICreateDataVolumeFromVolumeSnapshotMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.storage.primary.local.APILocalStorageMigrateVolumeMsg", defaultValue = "timeout::24h")
    public static String APILocalStorageMigrateVolumeMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.image.APICreateRootVolumeTemplateFromVolumeSnapshotMsg", defaultValue = "timeout::3h")
    public static String APICreateRootVolumeTemplateFromVolumeSnapshotMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.volume.APICreateVolumeSnapshotMsg", defaultValue = "timeout::3h")
    public static String APICreateVolumeSnapshotMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.vm.APICreateVmInstanceMsg", defaultValue = "timeout::3h")
    public static String APICreateVmInstanceMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg", defaultValue = "timeout::3h")
    public static String APIDeleteVolumeSnapshotMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.vm.APIExpungeVmInstanceMsg", defaultValue = "timeout::3h")
    public static String APIExpungeVmInstanceMsg;
}
