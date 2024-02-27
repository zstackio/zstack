package org.zstack.expon;

public class ExponIscsiHelper {

    static String iscsiTargetPrefix = "iscsi_zstack_active";
    static String iscsiExportTargetName = "iscsi_zstack_export";

    static String iscsiHeartbeatTargetName = "iscsi_zstack_heartbeat";

    static String iscsiHeartbeatClientName = "iscsi_zstack_heartbeat";

    static String iscsiHeartbeatVolumeName = "iscsi_zstack_heartbeat";

    static String buildIscsiExportClientName(String clientIp) {
        return "iscsi_" + clientIp.replace(".", "_");
    }

    static String buildIscsiVolumeClientName(String volUuid) {
        return "volume_" + volUuid;
    }

    static String buildVolumeIscsiTargetName(int index) {
        return String.format("%s_%d", iscsiTargetPrefix, index);
    }
}
