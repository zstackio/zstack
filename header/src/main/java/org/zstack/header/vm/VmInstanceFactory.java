package org.zstack.header.vm;

public interface VmInstanceFactory {
    VmInstanceType getType();
    
    VmInstanceVO createVmInstance(VmInstanceVO vo, APICreateVmInstanceMsg msg);
    
    VmInstance getVmInstance(VmInstanceVO vo);
}
