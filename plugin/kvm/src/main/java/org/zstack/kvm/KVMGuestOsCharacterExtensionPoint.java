package org.zstack.kvm;

import org.zstack.core.config.schema.GuestOsCharacter;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.vm.VmAfterAttachNicExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.kvm.KVMHostFactory.allGuestOsCharacter;

public class KVMGuestOsCharacterExtensionPoint implements VmAfterAttachNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KVMGuestOsCharacterExtensionPoint.class);

    @Override
    public void afterAttachNic(String nicUuid, VmInstanceInventory vmInstanceInventory, Completion completion) {
        if (!Q.New(VmNicVO.class)
                .eq(VmNicVO_.uuid, nicUuid)
                .isExists()) {
            logger.warn(String.format("cannot find nic[uuid:%s]", nicUuid));
            completion.success();
            return;
        }

        String vmArchPlatformRelease = String.format("%s_%s_%s", vmInstanceInventory.getArchitecture(), vmInstanceInventory.getPlatform(), vmInstanceInventory.getGuestOsType());
        GuestOsCharacter.Config config = allGuestOsCharacter.get(vmArchPlatformRelease);
        if (config == null) {
            logger.warn(String.format("cannot find guest os character for vm[uuid:%s, arch:%s, platform:%s, guestOsType:%s]",
                    vmInstanceInventory.getUuid(), vmInstanceInventory.getArchitecture(), vmInstanceInventory.getPlatform(), vmInstanceInventory.getGuestOsType()));
            completion.success();
            return;
        }

        if (config.getNicDriver() == null) {
            completion.success();
            return;
        }

        SQL.New(VmNicVO.class)
                .eq(VmNicVO_.uuid, nicUuid)
                .set(VmNicVO_.driverType, config.getNicDriver())
                .update();
        logger.warn(String.format("set nic[uuid:%s] driver type to %s", nicUuid, config.getNicDriver()));
    }

    @Override
    public void afterAttachNicRollback(String nicUuid, VmInstanceInventory vmInstanceInventory, NoErrorCompletion completion) {
        completion.done();
    }
}
