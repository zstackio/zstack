package org.zstack.sdnController.hardwareVxlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.CheckNetworkPhysicalInterfaceMsg;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.sdnController.header.SdnControllerConstant;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by shixin.ruan on 10/15/2019.
 */
public class KVMConnectExtensionForHardwareVxlanNetwork implements KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint {

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private KVMRealizeHardwareVxlanNetworkBackend hardwareVxlanNetworkBackend;
    @Autowired
    private CloudBus bus;

    @Transactional(readOnly = true)
    private List<L2NetworkInventory> getL2Networks(String clusterUuid) {
        String sql = "select l2.uuid from L2NetworkVO l2, L2NetworkClusterRefVO ref where l2.uuid = ref.l2NetworkUuid and ref.clusterUuid = :clusterUuid and l2.type = :type";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("clusterUuid", clusterUuid);
        q.setParameter("type", SdnControllerConstant.HARDWARE_VXLAN_NETWORK_TYPE);
        List<String> l2uuids = q.getResultList();
        List<L2NetworkInventory> ret = new ArrayList<L2NetworkInventory>(l2uuids.size());
        for (String l2uuid : l2uuids) {
            VxlanNetworkVO vxlanvo = dbf.getEntityManager().find(VxlanNetworkVO.class, l2uuid);
            ret.add(L2VxlanNetworkInventory.valueOf(vxlanvo));
        }
        return ret;
    }

    private void prepareNetwork(final Iterator<L2NetworkInventory> it, final HostInventory host, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final L2NetworkInventory l2 = it.next();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-hareware-vxlan-%s-for-kvm-%s-connect", l2.getUuid(), host.getUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                CheckNetworkPhysicalInterfaceMsg cmsg = new CheckNetworkPhysicalInterfaceMsg();
                cmsg.setHostUuid(host.getUuid());
                cmsg.setPhysicalInterface(l2.getPhysicalInterface());
                bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, host.getUuid());
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

        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                hardwareVxlanNetworkBackend.realize(l2, host.getUuid(), true, new Completion(trigger) {
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

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                prepareNetwork(it, host, completion);
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
        prepareNetwork(l2s.iterator(), inv, completion);
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
            String __name__ = "prepare-hardware-vxlan-network";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<L2NetworkInventory> l2s = getL2Networks(context.getInventory().getClusterUuid());
                if (l2s.isEmpty()) {
                    trigger.next();
                    return;
                }

                prepareNetwork(l2s.iterator(), context.getInventory(), new Completion(trigger) {
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
