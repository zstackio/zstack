package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageKvmFactory implements LocalStorageHypervisorFactory, KVMHostConnectExtensionPoint,
        FailToAddHostExtensionPoint, KVMStartVmExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LocalStorageKvmFactory.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    @Override
    public LocalStorageHypervisorBackend getHypervisorBackend(PrimaryStorageVO vo) {
        return new LocalStorageKvmBackend(vo);
    }

    @Transactional(readOnly = true)
    private List<String> findLocalStorageUuidByHostUuid(String clusterUuid) {
        String sql = "select pri.uuid" +
                " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = :cuuid" +
                " and pri.type = :ptype";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("cuuid", clusterUuid);
        q.setParameter("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE);
        return q.getResultList();
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "init-local-storage";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                final List<String> priUuids = findLocalStorageUuidByHostUuid(context.getInventory().getClusterUuid());
                if (priUuids == null || priUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                Iterator<String> iterator = priUuids.iterator();
                initLocalStorage(iterator, trigger, data, context);
            }
        };
    }

    private void initLocalStorage(final Iterator<String> iterator,
                                  final FlowTrigger trigger,
                                  Map data,
                                  final KVMHostConnectedContext context) {
        if (!iterator.hasNext()) {
            trigger.next();
            return;
        }
        final String priUuid = iterator.next();
        InitPrimaryStorageOnHostConnectedMsg msg = new InitPrimaryStorageOnHostConnectedMsg();
        msg.setPrimaryStorageUuid(priUuid);
        msg.setHostUuid(context.getInventory().getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, priUuid);
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(operr("KVM host[uuid: %s] fails to be added into local primary storage[uuid: %s], %s",
                            context.getInventory().getUuid(), priUuid, reply.getError()));
                } else {
                    initLocalStorage(iterator, trigger, data, context);
                }
            }
        });
    }

    @Override
    public void failedToAddHost(HostInventory host, AddHostMessage amsg) {
        final List<String> priUuids = findLocalStorageUuidByHostUuid(host.getClusterUuid());
        if (priUuids == null || priUuids.isEmpty()) {
            return;
        }

        for (String priUUid : priUuids) {
            LocalStorageRecalculateCapacityMsg msg = new LocalStorageRecalculateCapacityMsg();
            msg.setPrimaryStorageUuid(priUUid);
            msg.setNeedRecalculateRef(false);
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, priUUid);
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to sync primary storage[uuid:%s] capacity, %s",
                                priUuids, reply.getError()));
                    }
                }
            });
        }
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (spec.getMemorySnapshotUuid() == null) {
            return;
        }

        VolumeSnapshotVO vo = dbf.findByUuid(spec.getMemorySnapshotUuid(), VolumeSnapshotVO.class);

        if (!Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .eq(PrimaryStorageVO_.uuid, vo.getPrimaryStorageUuid()).isExists()) {
            return;
        }

        cmd.setMemorySnapshotPath(vo.getPrimaryStorageInstallPath());
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }
}
