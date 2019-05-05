package org.zstack.header.vm;

/**
 * Created by lining on 2019/4/17.
 */
public interface VmInstanceCreateExtensionPoint {
    void preCreateVmInstance(CreateVmInstanceMsg msg);
}
