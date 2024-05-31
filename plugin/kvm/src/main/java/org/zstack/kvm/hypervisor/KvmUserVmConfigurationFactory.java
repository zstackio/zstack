package org.zstack.kvm.hypervisor;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.UserVmFactory;
import org.zstack.core.config.schema.GuestOsCharacter;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.kvm.KVMSubTypeVmConfigurationFactory;
import org.zstack.core.config.GuestOsHelper;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class KvmUserVmConfigurationFactory implements KVMSubTypeVmConfigurationFactory {
    private static final CLogger logger = Utils.getLogger(KvmUserVmConfigurationFactory.class);

    @Autowired
    private UserVmFactory userVmFactory;
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public String getVmInstanceType() {
        return userVmFactory.getType().toString();
    }

    @Override
    public void createConfigurations(VmInstanceSpec spec) {
        GuestOsCharacter.Config config = GuestOsHelper.getInstance()
                .getGuestOsCharacter(
                        spec.getVmInventory().getArchitecture(),
                        spec.getVmInventory().getPlatform(),
                        spec.getVmInventory().getGuestOsType());
        if (config == null) {
            logger.warn(String.format("cannot find guest os character for vm[uuid:%s, arch:%s, platform:%s, guestOsType:%s]",
                    spec.getVmInventory().getUuid(), spec.getVmInventory().getArchitecture(), spec.getVmInventory().getPlatform(), spec.getVmInventory().getGuestOsType()));
            return;
        }

        if (config.getCpuModel() == null) {
            return;
        }

        ResourceConfig resourceConfig = rcf.getResourceConfig(KVMGlobalConfig.NESTED_VIRTUALIZATION.getIdentity());
        resourceConfig.updateValue(spec.getVmInventory().getUuid(), config.getCpuModel());
    }
}
