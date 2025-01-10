package org.zstack.storage.zbs;

import org.zstack.header.configuration.PythonClass;

/**
 * @author Xingwei Yu
 * @date 2024/3/21 13:10
 */
@PythonClass
public interface ZbsConstants {
    String IDENTITY = "zbs";
    String ZBS_PS_IPTABLES_COMMENTS = "Zbsp.allow.port";
    String ZBS_PS_ALLOW_PORTS = "7763";
    String ZBS_HEARTBEAT_VOLUME_NAME = "zbs_zstack_heartbeat";
    String ZBS_CBD_LUN_PATH_FORMAT = "cbd:%s/%s/%s";
    String ZBS_CBD_PREFIX_SCHEME = "cbd://";
    Integer PRIMARY_STORAGE_MDS_MAXIMUM_PING_FAILURE = 3;
}
