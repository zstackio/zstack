package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostConnectException;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;

import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageKvmFactory implements LocalStorageHypervisorFactory, KVMHostConnectExtensionPoint {
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
        String sql = "select pri.uuid from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref where pri.uuid = ref.primaryStorageUuid and ref.clusterUuid = :cuuid and pri.type = :ptype";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("cuuid", clusterUuid);
        q.setParameter("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE);
        List<String> ret = q.getResultList();
        return ret.isEmpty() ? null : ret.get(0);
    }

    @Override
    public void kvmHostConnected(KVMHostConnectedContext context) throws KVMHostConnectException {
        String priUuid = findLocalStorageUuidByHostUuid(context.getInventory().getClusterUuid());
        if (priUuid == null) {
            return;
        }

        InitPrimaryStorageOnHostConnectedMsg msg = new InitPrimaryStorageOnHostConnectedMsg();
        msg.setPrimaryStorageUuid(priUuid);
        msg.setHostUuid(context.getInventory().getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, priUuid);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("KVM host[uuid: %s] fails to add into local primary storage[uuid: %s], %s",
                            context.getInventory().getUuid(), priUuid, reply.getError())
            ));
        }
    }
}
