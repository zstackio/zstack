package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.logging.Log;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.AddHostMessage;
import org.zstack.header.host.FailToAddHostExtensionPoint;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.RecalculatePrimaryStorageCapacityMsg;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageKvmFactory implements LocalStorageHypervisorFactory, KVMHostConnectExtensionPoint,
        FailToAddHostExtensionPoint {
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
    private String findLocalStorageUuidByHostUuid(String clusterUuid) {
        String sql = "select pri.uuid" +
                " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = :cuuid" +
                " and pri.type = :ptype";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("cuuid", clusterUuid);
        q.setParameter("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE);
        List<String> ret = q.getResultList();
        return ret.isEmpty() ? null : ret.get(0);
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "init-local-storage";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                final String priUuid = findLocalStorageUuidByHostUuid(context.getInventory().getClusterUuid());
                if (priUuid == null) {
                    trigger.next();
                    return;
                }

                new Log(context.getInventory().getUuid()).log(LocalStorageLabels.INIT);

                InitPrimaryStorageOnHostConnectedMsg msg = new InitPrimaryStorageOnHostConnectedMsg();
                msg.setPrimaryStorageUuid(priUuid);
                msg.setHostUuid(context.getInventory().getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, priUuid);
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(errf.stringToOperationError(
                                    String.format("KVM host[uuid: %s] fails to be added into local primary storage[uuid: %s], %s",
                                            context.getInventory().getUuid(), priUuid, reply.getError())
                            ));
                        } else {
                            trigger.next();
                        }
                    }
                });
            }
        };
    }

    @Override
    public void failedToAddHost(HostInventory host, AddHostMessage amsg) {
        final String priUuid = findLocalStorageUuidByHostUuid(host.getClusterUuid());
        if (priUuid == null) {
            return;
        }

        RecalculatePrimaryStorageCapacityMsg msg = new RecalculatePrimaryStorageCapacityMsg();
        msg.setPrimaryStorageUuid(priUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, priUuid);
        bus.send(msg, new CloudBusCallBack() {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    //TODO:
                    logger.warn(String.format("failed to sync primary storage[uuid:%s] capacity, %s",
                            priUuid, reply.getError()));
                }
            }
        });
    }
}
