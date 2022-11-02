package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.vm.VmUpdateNicOnHypervisorMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;

public class TfAfterMigrateExtensionPointImpl implements VmInstanceMigrateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(TfAfterMigrateExtensionPointImpl.class);
    @Autowired
    private CloudBus bus;

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {

    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {

    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        VmUpdateNicOnHypervisorMsg cmsg = new VmUpdateNicOnHypervisorMsg();
        cmsg.setVmInstanceUuid(inv.getUuid());
        cmsg.setHostUuid(inv.getHostUuid());
        cmsg.setMigration(true);
        bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, inv.getUuid());
        bus.send(cmsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.error(String.format("failed to notify sugon sdn vrouter for vm [uuid:%s], %s", inv.getUuid(),
                            reply.getError()));
                } else {
                    logger.info(String.format("successfully to notify sugon sdn vrouter for vm[uuid:%s]", inv.getUuid()));
                }
            }
        });
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {

    }
}
