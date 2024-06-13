package org.zstack.xinfini;

public class XInfiniIscsiHelper {

    static String iscsiHeartbeatVolumeName = "iscsi_zstack_heartbeat";
    static String buildIscsiClientGroupName(String clientIp) {
        return "iscsi_" + clientIp.replace(".", "_");
    }
}
