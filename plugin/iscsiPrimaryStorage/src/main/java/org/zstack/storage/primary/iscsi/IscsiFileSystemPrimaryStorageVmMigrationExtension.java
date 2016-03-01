package org.zstack.storage.primary.iscsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.LoginIscsiTargetCmd;
import org.zstack.kvm.KVMAgentCommands.LoginIscsiTargetRsp;
import org.zstack.kvm.KVMAgentCommands.LogoutIscsiTargetCmd;
import org.zstack.kvm.KVMAgentCommands.LogoutIscsiTargetRsp;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by frank on 6/16/2015.
 */
public class IscsiFileSystemPrimaryStorageVmMigrationExtension implements VmInstanceMigrateExtensionPoint {
    private CLogger logger = Utils.getLogger(IscsiFileSystemPrimaryStorageVmMigrationExtension.class);
    private Map<String, List<VolumeInventory>> vmVolumes = new ConcurrentHashMap<String, List<VolumeInventory>>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    @Override
    public String preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        List<VolumeInventory> iscsiVolumes = new ArrayList<VolumeInventory>();
        for (VolumeInventory vol : inv.getAllVolumes()) {
            if (vol.getInstallPath().startsWith("iscsi")) {
                iscsiVolumes.add(vol);
            }
        }

        if (iscsiVolumes.isEmpty()) {
            return null;
        }

        boolean useVirtio = ImagePlatform.valueOf(inv.getPlatform()).isParaVirtualization()
                && KVMSystemTags.VIRTIO_SCSI.hasTag(destHostUuid);

        if (useVirtio) {
            return null;
        }

        List<KVMHostAsyncHttpCallMsg> msgs = new ArrayList<KVMHostAsyncHttpCallMsg>();
        for (VolumeInventory vol : iscsiVolumes) {
            LoginIscsiTargetCmd cmd = new LoginIscsiTargetCmd();
            IscsiVolumePath path = new IscsiVolumePath(vol.getInstallPath());
            path.disassemble();
            cmd.setHostname(path.getHostname());
            cmd.setPort(path.getPort());
            cmd.setTarget(path.getTarget());

            SimpleQuery<IscsiFileSystemBackendPrimaryStorageVO> q = dbf.createQuery(IscsiFileSystemBackendPrimaryStorageVO.class);
            q.select(IscsiFileSystemBackendPrimaryStorageVO_.chapUsername, IscsiFileSystemBackendPrimaryStorageVO_.chapPassword);
            q.add(IscsiFileSystemBackendPrimaryStorageVO_.uuid, Op.EQ, vol.getPrimaryStorageUuid());
            Tuple t = q.findTuple();

            cmd.setChapUsername(t.get(0, String.class));
            cmd.setChapPassword(t.get(1, String.class));

            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setCommand(cmd);
            msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
            msg.setPath(KVMConstant.KVM_LOGIN_ISCSI_PATH);
            msg.setHostUuid(destHostUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, destHostUuid);
            msgs.add(msg);
        }

        List<MessageReply> replies = bus.call(msgs);
        ErrorCode errorCode = null;
        for (MessageReply reply : replies) {
            if (!reply.isSuccess()) {
                errorCode = reply.getError();
                break;
            } else {
                KVMHostAsyncHttpCallReply r = reply.castReply();
                LoginIscsiTargetRsp rsp = r.toResponse(LoginIscsiTargetRsp.class);
                if (!rsp.isSuccess()) {
                    errorCode = errf.stringToOperationError(rsp.getError());
                    break;
                }
            }
        }

        if (errorCode != null) {
            List<VolumeInventory> toCleanup = new ArrayList<VolumeInventory>();
            for (MessageReply reply : replies) {
                if (reply.isSuccess()) {
                    toCleanup.add(iscsiVolumes.get(replies.indexOf(reply)));
                }
            }

            logoutIscsiTarget(toCleanup, destHostUuid);
            throw new OperationFailureException(errorCode);
        }

        vmVolumes.put(inv.getUuid(), iscsiVolumes);

        return null;
    }

    private void logoutIscsiTarget(final List<VolumeInventory> toCleanup, final String hostUuid) {
        List<KVMHostAsyncHttpCallMsg> msgs = new ArrayList<KVMHostAsyncHttpCallMsg>();
        for (VolumeInventory vol : toCleanup) {
            LogoutIscsiTargetCmd cmd = new LogoutIscsiTargetCmd();
            IscsiVolumePath path = new IscsiVolumePath(vol.getInstallPath());
            path.disassemble();
            cmd.setHostname(path.getHostname());
            cmd.setPort(path.getPort());
            cmd.setTarget(path.getTarget());

            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setPath(KVMConstant.KVM_LOGOUT_ISCSI_PATH);
            msg.setCommand(cmd);
            msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
            msgs.add(msg);
            msg.setHostUuid(hostUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        }

        bus.send(msgs, new CloudBusListCallBack() {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply reply : replies) {
                    if (!reply.isSuccess()) {
                        VolumeInventory vol = toCleanup.get(replies.indexOf(reply));
                        logger.warn(String.format("failed to logout iscsi target for volume[uuid:%s, path:%s] on host[uuid:%s], %s",
                                vol.getUuid(), vol.getInstallPath(), hostUuid, reply.getError()));
                        continue;
                    }

                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    LogoutIscsiTargetRsp rsp = r.toResponse(LogoutIscsiTargetRsp.class);
                    if (!rsp.isSuccess()) {
                        VolumeInventory vol = toCleanup.get(replies.indexOf(reply));
                        logger.warn(String.format("failed to logout iscsi target for volume[uuid:%s, path:%s] on host[uuid:%s], %s",
                                vol.getUuid(), vol.getInstallPath(), hostUuid, rsp.getError()));
                    }
                }
            }
        });
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {

    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        List<VolumeInventory> volumes = vmVolumes.get(inv.getUuid());
        if (volumes != null) {
            logoutIscsiTarget(volumes, srcHostUuid);
        }
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        List<VolumeInventory> volumes = vmVolumes.get(inv.getUuid());
        if (volumes != null) {
            logoutIscsiTarget(volumes, destHostUuid);
        }
    }
}
