package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConnectionReestablishExtensionPoint;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.kvm.KVMHostFactory;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 8/17/2015.
 */
public class CephKvmExtension implements KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(inv.getHypervisorType())) {
            return;
        }

        createSecret(inv.getUuid(), inv.getClusterUuid());
    }

    @Override
    public HypervisorType getHypervisorTypeForReestablishExtensionPoint() {
        return KVMHostFactory.hypervisorType;
    }

    @Transactional(readOnly = true)
    private List<String> findCephPrimaryStorage(String clusterUuid) {
        String sql = "select pri.uuid from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref where" +
                " ref.clusterUuid = :cuuid and pri.uuid = ref.primaryStorageUuid and pri.type = :ptype";
        TypedQuery<String>  q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("cuuid", clusterUuid);
        q.setParameter("ptype", CephConstants.CEPH_PRIMARY_STORAGE_TYPE);
        return q.getResultList();
    }

    private void createSecret(final String hostUuid, String clusterUuid) {
        List<String> psUuids = findCephPrimaryStorage(clusterUuid);
        if (psUuids.isEmpty()) {
            return;
        }

        List<CreateKvmSecretMsg> msgs = CollectionUtils.transformToList(psUuids, new Function<CreateKvmSecretMsg, String>() {
            @Override
            public CreateKvmSecretMsg call(String puuid) {
                CreateKvmSecretMsg msg = new CreateKvmSecretMsg();
                msg.setPrimaryStorageUuid(puuid);
                msg.setHostUuids(list(hostUuid));
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, puuid);
                return msg;
            }
        });

        List<MessageReply> replies = bus.call(msgs);
        for (MessageReply r : replies) {
            if (!r.isSuccess()) {
                throw new OperationFailureException(r.getError());
            }
        }
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "prepare-ceph-primary-storage";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                //TODO: change to async
                createSecret(context.getInventory().getUuid(), context.getInventory().getClusterUuid());
                trigger.next();
            }
        };
    }
}
