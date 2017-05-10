package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/3/26.
 */
public class KvmFactory implements HypervisorFactory, KVMHostConnectExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    @Override
    public HypervisorBackend getHypervisorBackend(PrimaryStorageVO vo) {
        return new KvmBackend(vo);
    }


    @Transactional(readOnly = true)
    private String findLocalStorageUuidByHostUuid(String clusterUuid) {
        String sql = "select pri.uuid from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref where pri.uuid = ref.primaryStorageUuid and ref.clusterUuid = :cuuid and pri.type = :ptype";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("cuuid", clusterUuid);
        q.setParameter("ptype", SMPConstants.SMP_TYPE);
        List<String> ret = q.getResultList();
        return ret.isEmpty() ? null : ret.get(0);
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __init__ = "init-smp-primary-storage";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                String psUuid = findLocalStorageUuidByHostUuid(context.getInventory().getClusterUuid());
                if (psUuid == null) {
                    trigger.next();
                    return;
                }

                InitKvmHostMsg msg = new InitKvmHostMsg();
                msg.setHypervisorType(context.getInventory().getHypervisorType());
                msg.setHostUuid(context.getInventory().getUuid());
                msg.setPrimaryStorageUuid(psUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, psUuid);
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            trigger.next();
                        }
                    }
                });
            }
        };
    }
}
