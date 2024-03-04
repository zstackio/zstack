package org.zstack.header.vm;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/3/4 13:14
 */
public interface KvmReportVmShutdownEventExtensionPoint {
    void kvmReportVmShutdownEvent(String vmUuid);
}
