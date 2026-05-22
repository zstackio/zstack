package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.vm.ArchiveResourceConfigBundle;
import org.zstack.header.vm.ResourceConfigMemorySnapshotExtensionPoint;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.resourceconfig.ResourceConfigFacade;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class KvmResourceConfigExtension implements ResourceConfigMemorySnapshotExtensionPoint {
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public List<ArchiveResourceConfigBundle.ResourceConfigBundle> getNeedToArchiveResourceConfig(String resourceUuid) {
        List<ArchiveResourceConfigBundle.ResourceConfigBundle> bundleList = new ArrayList<>();
        ArchiveResourceConfigBundle.ResourceConfigBundle bundle = new ArchiveResourceConfigBundle.ResourceConfigBundle();
        bundle.setResourceUuid(resourceUuid);
        bundle.setIdentity(KVMGlobalConfig.NESTED_VIRTUALIZATION.getIdentity());
        bundle.setValue(rcf.getResourceConfigValue(KVMGlobalConfig.NESTED_VIRTUALIZATION, resourceUuid, String.class));
        bundleList.add(bundle);

        if (isWindowsVm(resourceUuid)) {
            Boolean cpuHardwareVirtualization = rcf.getResourceConfigValue(KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION,
                    resourceUuid, Boolean.class);
            ArchiveResourceConfigBundle.ResourceConfigBundle cpuHardwareVirtualizationBundle =
                    new ArchiveResourceConfigBundle.ResourceConfigBundle();
            cpuHardwareVirtualizationBundle.setResourceUuid(resourceUuid);
            cpuHardwareVirtualizationBundle.setIdentity(KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.getIdentity());
            cpuHardwareVirtualizationBundle.setValue(cpuHardwareVirtualization.toString());
            bundleList.add(cpuHardwareVirtualizationBundle);
        }
        return bundleList;
    }

    private boolean isWindowsVm(String vmUuid) {
        VmInstanceVO vm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).find();
        if (vm == null) {
            return false;
        }

        if (ImagePlatform.isType(vm.getPlatform(), ImagePlatform.Windows, ImagePlatform.WindowsVirtio)) {
            return true;
        }

        return vm.getGuestOsType() != null && vm.getGuestOsType().toLowerCase(Locale.ROOT).contains("windows");
    }
}
