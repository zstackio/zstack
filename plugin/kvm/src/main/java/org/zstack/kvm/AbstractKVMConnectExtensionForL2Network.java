package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.*;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractKVMConnectExtensionForL2Network {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    protected L2NetworkManager l2Mgr;
    protected static final CLogger logger = Utils.getLogger(AbstractKVMConnectExtensionForL2Network.class);

    @Transactional(readOnly = true)
    protected List<L2NetworkInventory> getL2Networks(String clusterUuid) {
        String sql = "select l2 from L2NetworkVO l2, L2NetworkClusterRefVO ref where l2.uuid = ref.l2NetworkUuid and ref.clusterUuid = :clusterUuid and l2.type in (:supportTypes)";
        TypedQuery<L2NetworkVO> q = dbf.getEntityManager().createQuery(sql, L2NetworkVO.class);
        q.setParameter("clusterUuid", clusterUuid);
        q.setParameter("supportTypes", getSupportTypes());
        List<L2NetworkVO> vos = q.getResultList();
        List<L2NetworkInventory> ret = new ArrayList<L2NetworkInventory>(vos.size());
        List<L2NetworkInventory> noVlanL2Networks = new ArrayList<>();
        for (L2NetworkVO vo : vos) {
            if (L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE.equals(vo.getType())) {
                noVlanL2Networks.add(L2NetworkInventory.valueOf(vo));
            } else if (L2NetworkConstant.L2_VLAN_NETWORK_TYPE.equals(vo.getType())) {
                L2VlanNetworkVO vlanvo = dbf.getEntityManager().find(L2VlanNetworkVO.class, vo.getUuid());
                ret.add(L2VlanNetworkInventory.valueOf(vlanvo));
            } else {
                ret.add(L2NetworkInventory.valueOf(vo));
            }
        }

        /* when prepare l2 network, first prepare no vlan network, because mtu of vlan network must less than
         * no vlan network */
        if (!noVlanL2Networks.isEmpty()) {
            noVlanL2Networks.addAll(ret);
            return noVlanL2Networks;
        } else {
            return ret;
        }
    }

    protected List<String> getSupportTypes() {
        List<String> types = Arrays.asList(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE, L2NetworkConstant.L2_VLAN_NETWORK_TYPE);
        return types;
    }

    protected void prepareNetwork(final List<L2NetworkInventory> l2Networks, final String hostUuid, final Completion completion) {
        if (l2Networks.isEmpty()) {
            completion.success();
            return;
        }

        HostVO hostVO = dbf.findByUuid(hostUuid, HostVO.class);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-l2-for-kvm-%s-connect", hostUuid));
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-network-physical-interface";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<BatchCheckNetworkPhysicalInterfaceMsg> batchCheckNetworkPhysicalInterfaceMsgs = new ArrayList<>();
                final List<L2NetworkInventory> l2NetworksCheckList =
                        l2Networks.stream()
                                .collect(Collectors.collectingAndThen(Collectors.toCollection(()->new TreeSet<>(Comparator.comparing(L2NetworkInventory::getPhysicalInterface))),ArrayList::new));
                int step = 100;
                int size = l2NetworksCheckList.size();
                int count = size / 100;
                if (size - count * 100 > 0) {
                    count++;
                }

                for (int i = 0; i < count; i++) {
                    int end = (i + 1) * step - 1;
                    List<String> interfaces = l2NetworksCheckList.subList(i * step, Math.min(end, l2NetworksCheckList.size()))
                            .stream()
                            .map(L2NetworkInventory::getPhysicalInterface)
                            .collect(Collectors.toList());

                    BatchCheckNetworkPhysicalInterfaceMsg bmsg = new BatchCheckNetworkPhysicalInterfaceMsg();
                    bmsg.setPhysicalInterfaces(interfaces);
                    bmsg.setHostUuid(hostUuid);
                    bus.makeTargetServiceIdByResourceUuid(bmsg, HostConstant.SERVICE_ID, hostUuid);
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
            String __name__ = "realize_no_vlan";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<L2NetworkInventory> novlanNetworks = l2Networks.stream().filter(l2 -> l2.getType().equals(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE)).collect(Collectors.toList());
                new While<>(novlanNetworks).step((l2, c) -> {
                    L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(L2NetworkType.valueOf(l2.getType()), VSwitchType.valueOf(l2.getvSwitchType()), HypervisorType.valueOf(hostVO.getHypervisorType()));
                    ext.realize(l2, hostUuid, true, new Completion(c) {
                        @Override
                        public void success() {
                            c.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            c.addError(errorCode);
                            c.allDone();
                        }
                    });
                }, 10).run(new WhileDoneCompletion(trigger) {
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
            String __name__ = "realize_vlan";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<L2NetworkInventory> vlanNetworks = l2Networks.stream().filter(l2 -> l2.getType().equals(L2NetworkConstant.L2_VLAN_NETWORK_TYPE)).collect(Collectors.toList());
                new While<>(vlanNetworks).step((l2, c) -> {
                    L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(L2NetworkType.valueOf(l2.getType()), VSwitchType.valueOf(l2.getvSwitchType()), HypervisorType.valueOf(hostVO.getHypervisorType()));
                    ext.realize(l2, hostUuid, true, new Completion(c) {
                        @Override
                        public void success() {
                            c.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            c.addError(errorCode);
                            c.allDone();
                        }
                    });
                }, 10).run(new WhileDoneCompletion(trigger) {
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
            String __name__ = "sync_vlan_isolated";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<L2NetworkInventory> vlanNetworks = l2Networks.stream()
                        .filter(l2 -> l2.getType().equals(L2NetworkConstant.L2_VLAN_NETWORK_TYPE) && l2.getIsolated())
                        .collect(Collectors.toList());
                List<String> localVmUuidList = Q.New(VmInstanceVO.class)
                        .select(VmInstanceVO_.uuid)
                        .eq(VmInstanceVO_.hostUuid, hostUuid)
                        .notEq(VmInstanceVO_.state, VmInstanceState.Stopped)
                        .listValues();
                List<String> localStopVmUuidList = Q.New(VmInstanceVO.class)
                        .select(VmInstanceVO_.uuid)
                        .eq(VmInstanceVO_.lastHostUuid, hostUuid)
                        .eq(VmInstanceVO_.state, VmInstanceState.Stopped)
                        .listValues();
                localVmUuidList.addAll(localStopVmUuidList);
                Map<String, List<String>> isolatedL2NetworkMacMap = buildIsolatedL2NetworkMacMap(vlanNetworks, localVmUuidList);
                if (isolatedL2NetworkMacMap.isEmpty()) {
                    trigger.next();
                    return;
                }
                L2NetworkIsolatedSyncOnHostMsg smsg= new L2NetworkIsolatedSyncOnHostMsg();
                smsg.setHostUuid(hostUuid);
                smsg.setIsolatedL2NetworkMacMap(isolatedL2NetworkMacMap);
                bus.makeTargetServiceIdByResourceUuid(smsg, L2NetworkConstant.L2_PRIVATE_VLAN_SERVICE_ID, hostUuid);
                bus.send(smsg, new CloudBusCallBack(smsg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(reply.getError().toString());
                            trigger.fail(reply.getError());
                            return;
                        }
                        trigger.next();
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

    private Map<String, List<String>> buildIsolatedL2NetworkMacMap(List<L2NetworkInventory> vlanNetworks, List<String> localVmUuidList) {
        Map<String, List<String>> isolatedL2NetworkMacMap = new HashMap<>();
        for (L2NetworkInventory l2inv : vlanNetworks) {
            List<String> l3Uuids = Q.New(L3NetworkVO.class)
                    .select(L3NetworkVO_.uuid)
                    .eq(L3NetworkVO_.l2NetworkUuid, l2inv.getUuid())
                    .listValues();
            if (l3Uuids == null || l3Uuids.isEmpty()) {
                continue;
            }
            List<String> macList;
            if (localVmUuidList != null && !localVmUuidList.isEmpty()) {
                macList = Q.New(VmNicVO.class)
                        .select(VmNicVO_.mac)
                        .in(VmNicVO_.l3NetworkUuid, l3Uuids)
                        .notIn(VmNicVO_.vmInstanceUuid, localVmUuidList)
                        .listValues();
            } else {
                macList = Q.New(VmNicVO.class)
                        .select(VmNicVO_.mac)
                        .in(VmNicVO_.l3NetworkUuid, l3Uuids)
                        .listValues();
            }
            if (macList != null && !macList.isEmpty()) {
                isolatedL2NetworkMacMap.put(l2inv.getUuid(), macList);
            }
        }
        return isolatedL2NetworkMacMap;
    }
}
