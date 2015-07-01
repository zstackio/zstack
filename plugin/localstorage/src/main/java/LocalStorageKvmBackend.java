import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageKvmBackend extends LocalStorageHypervisorBackend {
    private final static CLogger logger = Utils.getLogger(LocalStorageKvmBackend.class);

    public static class AgentCommand {
    }

    public static class AgentResponse {
        private Long totalCapacity;
        private Long availableCapacity;

        private boolean success = true;
        private String error;
        public boolean isSuccess() {
            return success;
        }
        public void setSuccess(boolean success) {
            this.success = success;
        }
        public String getError() {
            return error;
        }
        public void setError(String error) {
            this.error = error;
        }
        public Long getTotalCapacity() {
            return totalCapacity;
        }

        public void setTotalCapacity(Long totalCapacity) {
            this.totalCapacity = totalCapacity;
        }

        public Long getAvailableCapacity() {
            return availableCapacity;
        }

        public void setAvailableCapacity(Long availableCapacity) {
            this.availableCapacity = availableCapacity;
        }
    }

    public class InitCmd extends AgentCommand {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public class GetPhysicalCapacityCmd extends AgentCommand {
    }

    public static String INIT_PATH = "/localstorage/init";
    public static String GET_PHYSICAL_CAPACITY_PATH = "/localstorage/getphysicalcapacity";

    public LocalStorageKvmBackend(PrimaryStorageVO self) {
        super(self);
    }

    @Override
    void syncPhysicalCapacityInCluster(List<ClusterInventory> clusters, final ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        List<String> clusterUuids = CollectionUtils.transformToList(clusters, new Function<String, ClusterInventory>() {
            @Override
            public String call(ClusterInventory arg) {
                return arg.getUuid();
            }
        });

        final PhysicalCapacityUsage ret = new PhysicalCapacityUsage();

        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.IN, clusterUuids);
        final List<String> hostUuids = q.listValue();

        if (hostUuids.isEmpty()) {
            completion.success(ret);
            return;
        }

        List<KVMHostAsyncHttpCallMsg> msgs = CollectionUtils.transformToList(hostUuids, new Function<KVMHostAsyncHttpCallMsg, String>() {
            @Override
            public KVMHostAsyncHttpCallMsg call(String arg) {
                GetPhysicalCapacityCmd cmd = new GetPhysicalCapacityCmd();

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(arg);
                msg.setCommand(cmd);
                msg.setPath(GET_PHYSICAL_CAPACITY_PATH);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, arg);
                return msg;
            }
        });

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply reply : replies) {
                    String hostUuid = hostUuids.get(replies.indexOf(reply));

                    if (!reply.isSuccess()) {
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s", hostUuid, reply.getError()));
                        continue;
                    }

                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    AgentResponse rsp = r.toResponse(AgentResponse.class);

                    if (!rsp.isSuccess()) {
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s", hostUuid, rsp.getError()));
                        continue;
                    }

                    ret.totalPhysicalSize += rsp.getTotalCapacity();
                    ret.availablePhysicalSize += rsp.getAvailableCapacity();
                }

                completion.success(ret);
            }
        });
    }

    @Override
    public void detachHook(String clusterUuid, final Completion completion) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        final List<String> hostUuids = q.listValue();

        if (hostUuids.isEmpty()) {
            completion.success();
            return;
        }

        SimpleQuery<LocalStorageHostRefVO> refq = dbf.createQuery(LocalStorageHostRefVO.class);
        refq.add(LocalStorageHostRefVO_.uuid, Op.EQ, self.getUuid());
        refq.add(LocalStorageHostRefVO_.hostUuid, Op.IN, hostUuids);
        List<LocalStorageHostRefVO> refs = refq.list();
        if (!refs.isEmpty()) {
            dbf.removeCollection(refs, LocalStorageHostRefVO.class);

            long total = 0;
            for (LocalStorageHostRefVO ref : refs) {
                total += ref.getTotalCapacity();
            }

            // after detaching, total capacity on those hosts should be deducted
            // from both total and available capacity of the primary storage
            decreaseCapacity(total, total, null, null);
        }

        syncPhysicalCapacity(new ReturnValueCompletion<PhysicalCapacityUsage>(completion) {
            @Override
            public void success(PhysicalCapacityUsage returnValue) {
                setCapacity(null, null, returnValue.totalPhysicalSize, returnValue.availablePhysicalSize);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to sync the physical capacity on the local primary storage[uuid:%s], %s", self.getUuid(), errorCode));
                completion.success();
            }
        });

    }

    @Override
    public void attachHook(String clusterUuid, final Completion completion) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        final List<String> hostUuids = q.listValue();

        if (hostUuids.isEmpty()) {
            completion.success();
            return;
        }

        List<KVMHostAsyncHttpCallMsg> msgs = CollectionUtils.transformToList(hostUuids, new Function<KVMHostAsyncHttpCallMsg, String>() {
            @Override
            public KVMHostAsyncHttpCallMsg call(String arg) {
                InitCmd cmd = new InitCmd();
                cmd.path = self.getUrl();

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setCommand(cmd);
                msg.setPath(INIT_PATH);
                msg.setHostUuid(arg);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, arg);
                return msg;
            }
        });

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                long total = 0;
                long avail = 0;
                List<LocalStorageHostRefVO> refs = new ArrayList<LocalStorageHostRefVO>();

                for (MessageReply reply : replies) {
                    String hostUuid = hostUuids.get(replies.indexOf(reply));
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s", hostUuid, reply.getError()));
                        continue;
                    }

                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    AgentResponse rsp = r.toResponse(AgentResponse.class);
                    if (!rsp.isSuccess()) {
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s", hostUuid, rsp.getError()));
                        continue;
                    }

                    total += rsp.getTotalCapacity();
                    avail += rsp.getAvailableCapacity();

                    LocalStorageHostRefVO ref = new LocalStorageHostRefVO();
                    ref.setUuid(self.getUuid());
                    ref.setHostUuid(hostUuid);
                    ref.setAvailablePhysicalCapacity(rsp.getAvailableCapacity());
                    ref.setAvailableCapacity(rsp.getAvailableCapacity());
                    ref.setTotalCapacity(rsp.getTotalCapacity());
                    ref.setTotalPhysicalCapacity(rsp.getTotalCapacity());
                    refs.add(ref);
                }

                dbf.persistCollection(refs);

                increaseCapacity(total, avail, total, avail);

                completion.success();
            }
        });
    }
}
