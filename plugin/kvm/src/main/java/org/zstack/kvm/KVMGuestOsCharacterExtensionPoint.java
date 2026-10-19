package org.zstack.kvm;

import org.zstack.core.config.GuestOsHelper;
import org.zstack.core.config.schema.GuestOsCharacter;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicSetDriverExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class KVMGuestOsCharacterExtensionPoint implements
        VmNicSetDriverExtensionPoint
{
    private static final CLogger logger = Utils.getLogger(KVMGuestOsCharacterExtensionPoint.class);

    private String getNicDriverFromConfig(VmInstanceInventory vm) {
        GuestOsCharacter.Config config = GuestOsHelper.getInstance()
                .getGuestOsCharacter(
                        vm.getArchitecture(),
                        vm.getPlatform(),
                        vm.getGuestOsType());
        if (config == null) {
            logger.warn(String.format("cannot find guest os character for vm[uuid:%s, arch:%s, platform:%s, guestOsType:%s]",
                    vm.getUuid(), vm.getArchitecture(), vm.getPlatform(), vm.getGuestOsType()));
            return null;
        }

        return config.getNicDriver();
    }

    @Override
    public String getPreferredVmNicDriver(VmInstanceInventory vm) {
        return getNicDriverFromConfig(vm);
    }
}
