package org.zstack.kvm;

import org.zstack.header.host.ConnectHostInfo;

import java.util.Map;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2023/1/5 10:25
 */
public interface KVMDeployHostExtensionPoint {
    Map<String, Object> generateDeployHostConfiguration(ConnectHostInfo info, KVMHostVO host);
}
