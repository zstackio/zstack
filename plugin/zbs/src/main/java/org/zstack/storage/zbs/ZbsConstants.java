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
    Integer PRIMARY_STORAGE_MDS_MAXIMUM_PING_FAILURE = 3;
}
