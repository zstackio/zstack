package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;

import java.util.Collections;
import java.util.List;

/**
 * Created by xing5 on 2016/3/26.
 */
public class SMPPrimaryStorageKvmBackend extends SMPPrimaryStorageHypervisorBackend {
    @Autowired
    private ApiTimeoutManager timeoutManager;

    public static class AgentCmd {
    }

    public static class AgentRsp {
        public boolean success = true;
        public String error;
        public Long totalCapacity;
        public Long availableCapacity;
    }

    public static class ConnectCmd extends AgentCmd {
        public String uuid;
        public String mountPoint;
    }

    public static final String CONNECT_PATH = "/sharedmountpointpirmarystorage/connect";

    public SMPPrimaryStorageKvmBackend(PrimaryStorageVO self) {
        super(self);
    }

    private String findConnectedHostByClusterUuid(String clusterUuid) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        q.add(HostVO_.status, Op.EQ, HostStatus.Connected);
        q.setLimit(200);
        List<String> hostUuids = q.listValue();
        if (hostUuids.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("no connected host found in the cluster[uuid:%s]", clusterUuid)
            ));
        }

        Collections.shuffle(hostUuids);
        return hostUuids.get(0);
    }

    private <T extends AgentRsp> void httpCall(String path, final String hostUuid, AgentCmd cmd, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        httpCall(path, hostUuid, cmd, false, rspType, completion);
    }

    private <T extends AgentRsp> void httpCall(String path, final String hostUuid, AgentCmd cmd, boolean noCheckStatus, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noCheckStatus);
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutManager.getTimeout(cmd.getClass(), "5m"));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                T rsp = r.toResponse(rspType);
                if (!rsp.success) {
                    completion.fail(errf.stringToOperationError(rsp.error));
                    return;
                }

                if (rsp.totalCapacity != null && rsp.availableCapacity != null) {
                    new PrimaryStorageCapacityUpdater(self.getUuid()).update(null, null, rsp.totalCapacity, rsp.availableCapacity);
                }

                completion.success(rsp);
            }
        });
    }

    @Override
    public void attachHook(String clusterUuid, final Completion completion) {
        String huuid = findConnectedHostByClusterUuid(clusterUuid);
        ConnectCmd cmd = new ConnectCmd();
        cmd.uuid = self.getUuid();
        cmd.mountPoint = self.getMountPath();

        httpCall(CONNECT_PATH, huuid, cmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp rsp) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void syncPhysicalCapacityInCluster(List<ClusterInventory> clusters, ReturnValueCompletion<PhysicalCapacityUsage> completion) {

    }

    @Override
    void handle(InstantiateVolumeMsg msg, ReturnValueCompletion<InstantiateVolumeReply> completion) {

    }

    @Override
    void handle(DeleteVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply> completion) {

    }

    @Override
    void handle(DownloadDataVolumeToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply> completion) {

    }

    @Override
    void handle(DeleteBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply> completion) {

    }

    @Override
    void handle(DownloadIsoToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadIsoToPrimaryStorageReply> completion) {

    }

    @Override
    void handle(DeleteIsoFromPrimaryStorageMsg msg, ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply> completion) {

    }

    @Override
    void handle(TakeSnapshotMsg msg, String hostUuid, ReturnValueCompletion<TakeSnapshotReply> completion) {

    }

    @Override
    void handle(DeleteSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply> completion) {

    }

    @Override
    void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply> completion) {

    }

    @Override
    void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg, String hostUuid, ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply> completion) {

    }

    @Override
    void handle(CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply> completion) {

    }

    @Override
    void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {

    }

    @Override
    void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<MergeVolumeSnapshotOnPrimaryStorageReply> completion) {

    }

    @Override
    void downloadImageToCache(ImageInventory img, String hostUuid, ReturnValueCompletion<String> completion) {

    }

    @Override
    void deleteBits(String path, String hostUuid, Completion completion) {

    }
}
