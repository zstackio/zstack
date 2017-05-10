package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.CheckL2NetworkOnHostMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.network.l2.vxlan.vxlanNetwork.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by weiwang on 10/05/2017.
 */
public class KVMConnectExtensionForVxlanNetwork implements KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KVMConnectExtensionForVxlanNetwork.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private KVMRealizeL2VxlanNetworkBackend kvmRealizeL2VxlanNetworkBackend;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;

    @Transactional
    private List<L2NetworkInventory> getL2Networks(String clusterUuid) {
        String sql = "select l2 from L2NetworkVO l2, L2NetworkClusterRefVO ref where l2.uuid = ref.l2NetworkUuid and ref.clusterUuid = :clusterUuid and l2.type in (:supportTypes)";
        TypedQuery<L2NetworkVO> q = dbf.getEntityManager().createQuery(sql, L2NetworkVO.class);
        q.setParameter("clusterUuid", clusterUuid);
        q.setParameter("supportTypes", getSupportTypes());
        List<L2NetworkVO> vos = q.getResultList();
        List<L2NetworkInventory> ret = new ArrayList<L2NetworkInventory>(vos.size());
        for (L2NetworkVO vo : vos) {
            if (VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE.equals(vo.getType())) {
                VxlanNetworkPoolVO poolvo = Q.New(VxlanNetworkPoolVO.class).eq(VxlanNetworkPoolVO_.uuid, vo.getUuid()).find();
                ret.add(L2VxlanNetworkPoolInventory.valueOf(poolvo));
            } else if (VxlanNetworkConstant.VXLAN_NETWORK_TYPE.equals(vo.getType())) {
                VxlanNetworkVO vxlanvo = Q.New(VxlanNetworkVO.class).eq(VxlanNetworkVO_.uuid, vo.getUuid()).find();
                ret.add(L2VxlanNetworkInventory.valueOf(vxlanvo));
            } else {
                logger.info(String.format("unsupport network type %s", vo.getType()));
            }
        }
        return ret;
    }

    private List<String> getSupportTypes() {
        List<String> types = Arrays.asList(VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE);
        return types;
    }

    private void prepareNetwork(final Iterator<L2NetworkInventory> it, final String hostUuid, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final L2NetworkInventory l2 = it.next();
        CheckL2NetworkOnHostMsg cmsg = new CheckL2NetworkOnHostMsg();
        cmsg.setHostUuid(hostUuid);
        cmsg.setL2NetworkUuid(l2.getUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, L2NetworkConstant.SERVICE_ID, l2.getUuid());
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success();
                }
            }
        });

    }

    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        //TODO: make connect async
        List<L2NetworkInventory> l2s = getL2Networks(inv.getClusterUuid());
        if (l2s.isEmpty()) {
            return;
        }

        FutureCompletion completion = new FutureCompletion(null);
        prepareNetwork(l2s.iterator(), inv.getUuid(), completion);
        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    public HypervisorType getHypervisorTypeForReestablishExtensionPoint() {
        return new HypervisorType(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "prepare-vxlan-network";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<L2NetworkInventory> l2s = getL2Networks(context.getInventory().getClusterUuid());
                if (l2s.isEmpty()) {
                    trigger.next();
                    return;
                }

                prepareNetwork(l2s.iterator(), context.getInventory().getUuid(), new Completion(trigger) {
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
