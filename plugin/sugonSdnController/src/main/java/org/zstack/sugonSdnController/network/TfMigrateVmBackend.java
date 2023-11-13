package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.DeviceAddress;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.*;
import org.zstack.network.service.MtuGetter;
import org.zstack.sugonSdnController.userdata.TfUserdataBackend;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;


public class TfMigrateVmBackend implements VmInstanceMigrateExtensionPoint, VmPreMigrationExtensionPoint {
    private static final CLogger logger = Utils.getLogger(TfMigrateVmBackend.class);
    public static final String NOTIFY_TF_NIC = "/vm/nodifytfnic";
    @Autowired
    private CloudBus bus;
    @Autowired
    private AccountManager accountMgr;
    @Autowired
    protected DatabaseFacade dbf;

    public static class SugonNicNotifyCmd extends KVMAgentCommands.AgentCommand {
        private String sugonSdnAction;
        private List<KVMAgentCommands.NicTO> nics;
        private String vmInstanceUuid;

        private String accountUuid;
        public String getSugonSdnAction() {
            return sugonSdnAction;
        }

        public void setSugonSdnAction(String sugonSdnAction) {
            this.sugonSdnAction = sugonSdnAction;
        }

        public List<KVMAgentCommands.NicTO> getNics() {
            return nics;
        }

        public void setNics(List<KVMAgentCommands.NicTO> nics) {
            this.nics = nics;
        }
        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }

    }

    public static class SugonNicNotifyCmdRsp extends KVMAgentCommands.AgentResponse {

    }


    @Override
    public void preVmMigration(VmInstanceInventory vm, VmMigrationType type, String dstHostUuid, Completion completion) {
        try {
            notifySugonSdn(vm, dstHostUuid, "add");
            completion.success();
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
        }
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        // pre支持物理机+本地存储迁移，以及物理机+共享存储迁移，before不支持物理机+共享存储的迁移
        notifySugonSdn(inv, destHostUuid, "add");
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {

    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        notifySugonSdn(inv, srcHostUuid, "delete");
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        notifySugonSdn(inv, destHostUuid, "delete");
    }

    private boolean needNotiySugonSdn(VmInstanceInventory inv) {
        for (VmNicInventory nic : inv.getVmNics()) {
            if (VmInstanceConstant.TF_VIRTUAL_NIC_TYPE.equalsIgnoreCase(nic.getType())) {
                return true;
            }
        }
        return false;
    }

    private void notifySugonSdn(VmInstanceInventory inv, String destHostUuid, String operate) {
        if (!needNotiySugonSdn(inv)) {
            return;
        }
        logger.info(String.format("notifySugonSdn: start to notify sugon sdn to %s vrouter for vm[uuid:%s] in success flow", operate, inv.getUuid()));
        SugonNicNotifyCmd cmd = new SugonNicNotifyCmd();
        cmd.setSugonSdnAction(operate);
        cmd.setVmInstanceUuid(inv.getUuid());
        cmd.setAccountUuid(accountMgr.getOwnerAccountUuidOfResource(cmd.getVmInstanceUuid()));
        List<VmNicVO> nics = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, inv.getUuid()).list();
        List<VmNicInventory> nicInvs = VmNicInventory.valueOf(nics);
        if (nicInvs == null || nicInvs.isEmpty()) {
            logger.info(String.format("notifySugonSdn: nic count is zero, will not call sugon sdn"));
            return;
        }
        cmd.setNics(nicInvs.stream().filter(vmNic -> VmInstanceConstant.TF_VIRTUAL_NIC_TYPE.equalsIgnoreCase(vmNic.getType()))
                .map(this::completeNicInfo).collect(Collectors.toList()));
        logger.info(String.format("after completeNicInfo:nic count: %s", cmd.getNics().size()));
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(destHostUuid);
        msg.setCommand(cmd);
        msg.setPath(NOTIFY_TF_NIC);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, destHostUuid);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            logger.error(String.format("notifySugonSdn: failed to notify sugon sdn to %s vrouter for vm [uuid:%s], %s", operate, inv.getUuid(),
                    reply.getError()));
            throw new OperationFailureException(operr("notifySugonSdn: failed to notify sugon sdn to %s vrouter for vm [uuid:%s], on the destination host[uuid:%s]",
                    operate, inv.getUuid(), destHostUuid).causedBy(reply.getError()));
        }

        KVMHostAsyncHttpCallReply r = reply.castReply();
        TfMigrateVmBackend.SugonNicNotifyCmdRsp rsp = r.toResponse(TfMigrateVmBackend.SugonNicNotifyCmdRsp.class);
        if (!rsp.isSuccess()) {
            logger.error(String.format("notifySugonSdn: failed to notify sugon sdn to %s vrouter for vm [uuid:%s], %s", operate, inv.getUuid(),
                    rsp.getError()));
            throw new OperationFailureException(operr("notifySugonSdn: failed to notify sugon sdn to %s vrouter for vm [uuid:%s], on the destination host[uuid:%s], error is:%s",
                    operate, inv.getUuid(), destHostUuid, rsp.getError()));
        }
        logger.info(String.format("notifySugonSdn: successfully to notify sugon sdn to %s vrouter for vm[uuid:%s]", operate, inv.getUuid()));
    }

    @Transactional(readOnly = true)
    private KVMAgentCommands.NicTO completeNicInfo(VmNicInventory nic) {
        L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, nic.getL3NetworkUuid()).find();
        L2NetworkVO l2NetworkVO = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l3NetworkVO.getL2NetworkUuid()).find();
        L2NetworkInventory l2inv = L2NetworkInventory.valueOf(l2NetworkVO);
        KVMAgentCommands.NicTO to = KVMAgentCommands.NicTO.fromVmNicInventory(nic);
        to.setIpForTf(nic.getIp());
        to.setMtu(new MtuGetter().getMtu(l3NetworkVO.getUuid()));
        to.setL2NetworkUuid(l2inv.getUuid());
        to.setResourceUuid(nic.getUuid());
        logger.debug("notifySugonSdn: Complete nic information for TfL2Network");
        return to;
    }
}
