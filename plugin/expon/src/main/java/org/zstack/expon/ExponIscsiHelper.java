package org.zstack.expon;

import org.zstack.header.storage.addon.primary.BaseVolumeInfo;

public class ExponIscsiHelper {

    static String iscsiTargetPrefix = "iscsi_zstack_active";
    static String iscsiExportTargetName = "iscsi_zstack_export";

    static String buildIscsiClientName(String clientIp) {
        return "iscsi_" + clientIp.replace(".", "_");
    }

    static String buildIscsiClientName(BaseVolumeInfo vol) {
        return "volume_" + vol.getUuid();
    }

    static String buildVolumeIscsiTargetName(int index) {
        return String.format("%s_%d", iscsiTargetPrefix, index);
    }
}
