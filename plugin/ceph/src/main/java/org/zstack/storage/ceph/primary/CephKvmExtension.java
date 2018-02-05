package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
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

        FutureCompletion completion = new FutureCompletion(null);
        createSecret(inv.getUuid(), inv.getClusterUuid(), new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });

        completion.await();

        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
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

    private void createSecret(final String hostUuid, String clusterUuid, Completion completion) {
        List<String> psUuids = findCephPrimaryStorage(clusterUuid);
        if (psUuids.isEmpty()) {
            completion.success();
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

        List<ErrorCode> errorCodeList = new ArrayList<>();

        new While<>(msgs).all((msg, noErrorCompletion) -> {
            bus.send(msg, new CloudBusCallBack(noErrorCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errorCodeList.add(reply.getError());
                    }

                    noErrorCompletion.done();
                }
            });
        }).run(new NoErrorCompletion(completion) {
            @Override
            public void done() {
                if (!errorCodeList.isEmpty()) {
                    completion.fail(errorCodeList.get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "prepare-ceph-primary-storage";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                createSecret(context.getInventory().getUuid(), context.getInventory().getClusterUuid(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        };
    }
}
