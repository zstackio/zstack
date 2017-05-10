package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 */
public class KVMConnectExtensionForL2Network implements KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private KVMRealizeL2NoVlanNetworkBackend noVlanNetworkBackend;
    @Autowired
    private KVMRealizeL2VlanNetworkBackend vlanNetworkBackend;
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
            if (L2NetworkConstant.L2_VLAN_NETWORK_TYPE.equals(vo.getType())) {
                L2VlanNetworkVO vlanvo = dbf.getEntityManager().find(L2VlanNetworkVO.class, vo.getUuid());
                ret.add(L2VlanNetworkInventory.valueOf(vlanvo));
            } else {
                ret.add(L2NetworkInventory.valueOf(vo));
            }
        }
        return ret;
    }

    private List<String> getSupportTypes() {
        List<String> types = Arrays.asList(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE, L2NetworkConstant.L2_VLAN_NETWORK_TYPE);
        return types;
    }

    private void prepareNetwork(final Iterator<L2NetworkInventory> it, final String hostUuid, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final L2NetworkInventory l2 = it.next();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-l2-%s-for-kvm-%s-connect", l2.getUuid(), hostUuid));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                CheckNetworkPhysicalInterfaceMsg cmsg = new CheckNetworkPhysicalInterfaceMsg();
                cmsg.setHostUuid(hostUuid);
                cmsg.setPhysicalInterface(l2.getPhysicalInterface());
                bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(cmsg, new CloudBusCallBack(completion) {
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
        });

        if (l2.getType().equals(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE)) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    noVlanNetworkBackend.realize(l2, hostUuid, true, new Completion(trigger) {
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
            });
        } else if (L2NetworkConstant.L2_VLAN_NETWORK_TYPE.equals(l2.getType())) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    vlanNetworkBackend.realize(l2, hostUuid, true, new Completion(trigger) {
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
            });
        } else {
            completion.fail(operr("KVMConnectExtensionForL2Network wont's support L2Network[type:%s]", l2.getType()));
            return;
        }

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                prepareNetwork(it, hostUuid, completion);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
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
            String __name__ = "prepare-l2-network";

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
