package org.zstack.compute.vm.devices;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.BuildVmSpecExtensionPoint;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.db.Q;
import org.zstack.header.tpm.entity.TpmSpec;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.VmInstanceCreateExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.devices.VmDevicesSpec;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;

import static org.zstack.compute.vm.VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT;
import static org.zstack.header.vm.VmInstanceConstant.NV_RAM_DEFAULT_SIZE;

public class VmTpmExtensions implements VmInstanceCreateExtensionPoint,
        BuildVmSpecExtensionPoint {
    @Autowired
    private VmTpmManager vmTpmManager;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;

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

        boolean tpmExists = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, vmUuid)
                .isExists();
        boolean needRegisterNvRam = tpmExists;
        if (!needRegisterNvRam) {
            String bootMode = VmSystemTags.BOOT_MODE.getTokenByResourceUuid(vmUuid, VmSystemTags.BOOT_MODE_TOKEN);
            if (vmTpmManager.isUefiBootMode(bootMode)) {
                ResourceConfig resourceConfig = resourceConfigFacade.getResourceConfig(ENABLE_UEFI_SECURE_BOOT.getIdentity());
                needRegisterNvRam = resourceConfig.getResourceConfigValue(vmUuid, Boolean.class) == Boolean.TRUE;
            }
        }

        if (needRegisterNvRam) {
            DiskAO nvRamSpec = new DiskAO();
            nvRamSpec.setSize(NV_RAM_DEFAULT_SIZE);
            nvRamSpec.setName("NvRam-of-VM-" + vmUuid);
            spec.setNvRamSpec(nvRamSpec);
        }

        if (tpmExists && (spec.getDevicesSpec() == null || spec.getDevicesSpec().getTpm() == null)) {
            VmDevicesSpec devicesSpec = spec.getDevicesSpec() == null ? new VmDevicesSpec() : spec.getDevicesSpec();
            spec.setDevicesSpec(devicesSpec);

            devicesSpec.setTpm(new TpmSpec());
            devicesSpec.getTpm().setEnable(true);
        }
    }
}
