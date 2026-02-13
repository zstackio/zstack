package org.zstack.compute.vm.devices;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.BuildVmSpecExtensionPoint;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.VmInstanceCreateExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.devices.VmDevicesSpec;

import static org.zstack.header.vm.VmInstanceConstant.NV_RAM_DEFAULT_SIZE;

public class VmTpmExtensions implements VmInstanceCreateExtensionPoint,
        BuildVmSpecExtensionPoint {
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

    @Override
    public void afterBuildVmSpec(VmInstanceSpec spec) {
        String vmUuid = spec.getVmInventory().getUuid();
        if (!vmTpmManager.needRegisterNvRam(vmUuid)) {
            return;
        }

        DiskAO nvRamSpec = new DiskAO();
        nvRamSpec.setSize(NV_RAM_DEFAULT_SIZE);
        nvRamSpec.setName("NvRam-of-VM-" + vmUuid);
        spec.setNvRamSpec(nvRamSpec);
    }
}
