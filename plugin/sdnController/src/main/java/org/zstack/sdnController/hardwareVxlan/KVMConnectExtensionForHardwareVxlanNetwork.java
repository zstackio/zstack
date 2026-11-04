package org.zstack.sdnController.hardwareVxlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.BatchCheckNetworkPhysicalInterfaceMsg;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.network.l2.L2NetworkGlobalConfig;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.sdnController.header.HardwareL2VxlanNetworkInventory;
import org.zstack.sdnController.header.HardwareL2VxlanNetworkVO;
import org.zstack.sdnController.header.HardwareL2VxlanNetworkVO_;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        if (l2uuids.isEmpty()) {
            return Collections.emptyList();
        }
        List<HardwareL2VxlanNetworkVO> hwVxlanVos = Q.New(HardwareL2VxlanNetworkVO.class)
                .in(HardwareL2VxlanNetworkVO_.uuid, l2uuids)
                .list();
        Map<String, HardwareL2VxlanNetworkVO> voMap = hwVxlanVos.stream()
                .collect(Collectors.toMap(HardwareL2VxlanNetworkVO::getUuid, Function.identity()));
        List<L2NetworkInventory> ret = new ArrayList<L2NetworkInventory>(l2uuids.size());
        for (String l2uuid : l2uuids) {
            HardwareL2VxlanNetworkVO vxlanvo = voMap.get(l2uuid);
            if (vxlanvo != null) {
                ret.add(HardwareL2VxlanNetworkInventory.valueOf(vxlanvo));
            }
        }
        return ret;
    }

    private void prepareNetwork(final List<L2NetworkInventory> l2Networks, final HostInventory host, final Completion completion) {
        if (l2Networks.isEmpty()) {
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-hardware-vxlan-for-kvm-%s-connect", host.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-network-physical-interface";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<BatchCheckNetworkPhysicalInterfaceMsg> batchCheckNetworkPhysicalInterfaceMsgs = new ArrayList<>();
                final List<L2NetworkInventory> l2NetworksCheckList =
                        l2Networks.stream()
                                .collect(Collectors.collectingAndThen(
                                        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(L2NetworkInventory::getPhysicalInterface))),
                                        ArrayList::new));
                int step = L2NetworkGlobalConfig.RealizeL2NetworkBatchCount.value(Integer.class);
                int size = l2NetworksCheckList.size();
                for (int i = 0; i < size; i += step) {
                    List<String> interfaces = l2NetworksCheckList.subList(i, Math.min(i + step, size))
                            .stream()
                            .map(L2NetworkInventory::getPhysicalInterface)
                            .collect(Collectors.toList());

                    BatchCheckNetworkPhysicalInterfaceMsg bmsg = new BatchCheckNetworkPhysicalInterfaceMsg();
                    bmsg.setPhysicalInterfaces(interfaces);
                    bmsg.setHostUuid(host.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(bmsg, HostConstant.SERVICE_ID, host.getUuid());
                    batchCheckNetworkPhysicalInterfaceMsgs.add(bmsg);
                }

                new While<>(batchCheckNetworkPhysicalInterfaceMsgs).each((bmsg, c) -> {
                    bus.send(bmsg, new CloudBusCallBack(c) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                c.addError(reply.getError());
                                c.allDone();
                                return;
                            }
                            c.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            trigger.fail(errorCodeList.getCauses().get(0));
                            return;
                        }
                        trigger.next();
                    }
                });
            }
        });

        chain.then(new NoRollbackFlow() {
            String __name__ = "realize-hardware-vxlan";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                hardwareVxlanNetworkBackend.realize(l2Networks, host.getUuid(), true, new Completion(trigger) {
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
                completion.success();
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
        prepareNetwork(l2s, inv, completion);
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

                prepareNetwork(l2s, context.getInventory(), new Completion(trigger) {
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
