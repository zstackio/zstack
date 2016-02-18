package org.zstack.storage.primary.iscsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceDestroyExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceStopExtensionPoint;
import org.zstack.kvm.*;
import org.zstack.kvm.KVMAgentCommands.LogoutIscsiTargetCmd;
import org.zstack.kvm.KVMAgentCommands.LogoutIscsiTargetRsp;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.util.Collections;
import java.util.List;

/**
 * Created by frank on 6/8/2015.
 */
public class IscsiIsoStoreManagerImpl implements IscsiIsoStoreManager, Component, VmInstanceStopExtensionPoint, VmInstanceDestroyExtensionPoint {
    private static CLogger logger = Utils.getLogger(IscsiIsoStoreManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @Transactional
    public IscsiIsoVO take(IscsiIsoSpec spec) {
        SimpleQuery<IscsiIsoVO> q = dbf.createQuery(IscsiIsoVO.class);
        q.add(IscsiIsoVO_.imageUuid, Op.EQ, spec.getImageUuid());
        q.add(IscsiIsoVO_.vmInstanceUuid, Op.NULL);
        q.add(IscsiIsoVO_.primaryStorageUuid, Op.EQ, spec.getPrimaryStorageUuid());
        List<IscsiIsoVO> res = q.list();
        if (res.isEmpty()) {
            return null;
        }

        Collections.shuffle(res);

        for (IscsiIsoVO vo : res) {
            String sql = "update IscsiIsoVO i set i.vmInstanceUuid = :vmUuid where i.uuid = :uuid and i.vmInstanceUuid is null";
            Query uq = dbf.getEntityManager().createQuery(sql);
            uq.setParameter("vmUuid", spec.getVmInstanceUuid());
            uq.setParameter("uuid", vo.getUuid());
            if (uq.executeUpdate() != 0) {
                return vo;
            }
        }

        return null;
    }

    @Transactional
    @Override
    public void releaseByVmUuid(String vmUuid) {
        String sql = "update IscsiIsoVO i set i.vmInstanceUuid = null where i.vmInstanceUuid = :vmUuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("vmUuid", vmUuid);
        q.executeUpdate();
    }

    @Override
    public void store(IscsiIsoVO iso) {
        dbf.persist(iso);
    }

    @Override
    public String preStopVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStopVm(VmInstanceInventory inv) {

    }

    private void logoutIscsiTargetOnKvmHostIfNeeded(VmInstanceInventory inv, String hostUuid) {
        if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(inv.getHypervisorType())) {
            return;
        }

        SimpleQuery<IscsiIsoVO> q = dbf.createQuery(IscsiIsoVO.class);
        q.add(IscsiIsoVO_.vmInstanceUuid, Op.EQ, inv.getUuid());
        IscsiIsoVO iso = q.find();
        if (iso != null) {
            LogoutIscsiTargetCmd cmd = new LogoutIscsiTargetCmd();
            cmd.setTarget(iso.getTarget());
            cmd.setHostname(iso.getHostname());
            cmd.setPort(iso.getPort());

            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setPath(KVMConstant.KVM_LOGOUT_ISCSI_PATH);
            msg.setCommand(cmd);
            msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
            msg.setHostUuid(hostUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
            MessageReply reply = bus.call(msg);
            if (!reply.isSuccess()) {
                logger.warn(String.format("failed to logout iscsi target[name:%s, hostname:%s, port:%s] on KVM host[uuid:%s] for VM[uuid:%s], %s",
                        iso.getTarget(), iso.getHostname(), iso.getPort(), hostUuid, inv.getUuid(), reply.getError()));
            } else {
                KVMHostAsyncHttpCallReply r = (KVMHostAsyncHttpCallReply) reply;
                LogoutIscsiTargetRsp rsp = r.toResponse(LogoutIscsiTargetRsp.class);
                if (!rsp.isSuccess()) {
                    logger.warn(String.format("failed to logout iscsi target[name:%s, hostname:%s, port:%s] on KVM host[uuid:%s] for VM[uuid:%s], %s",
                            iso.getTarget(), iso.getHostname(), iso.getPort(), hostUuid, inv.getUuid(), rsp.getError()));
                }
            }
        }
    }

    @Override
    public void afterStopVm(VmInstanceInventory inv) {
        logoutIscsiTargetOnKvmHostIfNeeded(inv, inv.getLastHostUuid());
        releaseByVmUuid(inv.getUuid());

    }

    @Override
    public void failedToStopVm(VmInstanceInventory inv, ErrorCode reason) {

    }

    @Override
    public String preDestroyVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeDestroyVm(VmInstanceInventory inv) {
        if (inv.getHostUuid() != null) {
            logoutIscsiTargetOnKvmHostIfNeeded(inv, inv.getHostUuid());
        }
    }

    @Override
    public void afterDestroyVm(VmInstanceInventory inv) {

    }

    @Override
    public void failedToDestroyVm(VmInstanceInventory inv, ErrorCode reason) {

    }
}
