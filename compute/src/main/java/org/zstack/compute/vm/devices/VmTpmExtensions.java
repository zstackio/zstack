package org.zstack.compute.vm.devices;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceCreateExtensionPoint;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.devices.VmDevicesSpec;

public class VmTpmExtensions implements VmInstanceCreateExtensionPoint {
    @Autowired
    private VmTpmManager vmTpmManager;

    @Override
    public void preCreateVmInstance(CreateVmInstanceMsg msg) {
        // do-nothing
    }

    @Override
    public void afterPersistVmInstanceVO(VmInstanceVO vo, CreateVmInstanceMsg msg) {
        final VmDevicesSpec spec = msg.getDevicesSpec();
        if (spec == null || spec.getTpm() == null || !spec.getTpm().isEnable()) {
            return;
        }

        vmTpmManager.persistTpmVO(null, vo.getUuid());
    }
}
