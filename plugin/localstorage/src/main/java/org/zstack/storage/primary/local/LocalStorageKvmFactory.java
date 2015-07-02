package org.zstack.storage.primary.local;

import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.kvm.KVMConstant;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageKvmFactory implements LocalStorageHypervisorFactory {
    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    @Override
    public LocalStorageHypervisorBackend getHypervisorBackend(PrimaryStorageVO vo) {
        return new LocalStorageKvmBackend(vo);
    }
}
