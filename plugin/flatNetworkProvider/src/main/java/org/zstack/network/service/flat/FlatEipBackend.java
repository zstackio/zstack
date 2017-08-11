package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.notification.N;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostErrors;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.network.service.NetworkServiceFilter;
import org.zstack.network.service.eip.EipBackend;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.eip.EipStruct;
import org.zstack.network.service.eip.EipVO;
import org.zstack.network.service.flat.FlatNetworkServiceConstant.AgentCmd;
import org.zstack.network.service.flat.FlatNetworkServiceConstant.AgentRsp;
import org.zstack.network.service.vip.VipVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/4/4.
 */
public class FlatEipBackend implements EipBackend, KVMHostConnectExtensionPoint,
        VmAbnormalLifeCycleExtensionPoint, VmInstanceMigrateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FlatEipBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    private DatabaseFacade dbf;

    public static class EipTO {
        public String eipUuid;
        public String vmUuid;
        public String nicUuid;
        public String vip;
        public String vipNetmask;
        public String vipGateway;
        public String nicIp;
        public String nicMac;
        public String nicGateway;
        public String nicNetmask;
        public String nicName;
        public String vmBridgeName;
        public String publicBridgeName;
    }

    public static class ApplyEipCmd extends AgentCmd {
        public EipTO eip;
    }

    public static class DeleteEipCmd extends AgentCmd {
        public EipTO eip;
    }

    public static class BatchApplyEipCmd extends AgentCmd {
        public List<EipTO> eips;
    }

    public static class BatchDeleteEipCmd extends AgentCmd {
        public List<EipTO> eips;
    }

    public static final String APPLY_EIP_PATH = "/flatnetworkprovider/eip/apply";
    public static final String DELETE_EIP_PATH = "/flatnetworkprovider/eip/delete";
    public static final String BATCH_APPLY_EIP_PATH = "/flatnetworkprovider/eip/batchapply";
    public static final String BATCH_DELETE_EIP_PATH = "/flatnetworkprovider/eip/batchdelete";

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {

    }

    @Override
    public void afterMigrateVm(final VmInstanceInventory inv, String srcHostUuid) {
        List<EipTO> eips = getEipsByVmUuid(inv.getUuid());
        if (eips == null || eips.isEmpty()) {
            return;
        }

        batchDeleteEips(eips, srcHostUuid, new NopeCompletion());
        batchApplyEips(eips, inv.getHostUuid(), new Completion(null) {
            @Override
            public void success() {
                // pass
            }

            @Override
            public void fail(ErrorCode errorCode) {
                N.New(VmInstanceVO.class, inv.getUuid()).warn_("after migration, failed to apply EIPs[uuids:%s] to the vm[uuid:%s, name:%s] on the destination host[uuid:%s], %s",
                        eips.stream().map(e -> e.vip).collect(Collectors.toList()), inv.getUuid(), inv.getName(), inv.getHostUuid(), errorCode);
            }
        });
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {

    }

    @Transactional(readOnly = true)
    private List<EipTO> getEipsByVmUuid(String vmUuid) {
        List<VmNicVO> nics = new VmNicFinder().findVmNicsByVmUuid(vmUuid);
        if (nics == null) {
            return null;
        }

        return getEipsByNics(nics);
    }

    @Override
    public Flow createVmAbnormalLifeCycleHandlingFlow(final VmAbnormalLifeCycleStruct struct) {
        return new Flow() {
            String __name__ = "flat-network-configure-eip";

            VmInstanceInventory vm = struct.getVmInstance();
            List<EipTO> eips = getEipsByVmUuid(vm.getUuid());
            VmAbnormalLifeCycleOperation operation = struct.getOperation();
            String applyHostUuidForRollback;
            String releaseHostUuidForRollback;

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (eips == null) {
                    trigger.next();
                    return;
                }

                if (operation == VmAbnormalLifeCycleOperation.VmRunningOnTheHost) {
                    vmRunningOnTheHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmStoppedOnTheSameHost) {
                    vmStoppedOnTheSameHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmRunningFromUnknownStateHostChanged) {
                    vmRunningFromUnknownStateHostChanged(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmMigrateToAnotherHost) {
                    vmMigrateToAnotherHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmRunningFromIntermediateState) {
                    vmRunningFromIntermediateState(trigger);
                } else {
                    trigger.next();
                }
            }

            private void vmRunningFromIntermediateState(final FlowTrigger trigger) {
                batchApplyEips(eips, struct.getCurrentHostUuid(), new Completion(trigger) {
                    @Override
                    public void success() {
                        releaseHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            private void vmMigrateToAnotherHost(final FlowTrigger trigger) {
                batchDeleteEips(eips, struct.getOriginalHostUuid(), new NopeCompletion());
                applyHostUuidForRollback = struct.getOriginalHostUuid();
                batchApplyEips(eips, struct.getCurrentHostUuid(), new Completion(trigger) {
                    @Override
                    public void success() {
                        releaseHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }


            private void vmRunningFromUnknownStateHostChanged(final FlowTrigger trigger) {
                batchDeleteEips(eips, struct.getOriginalHostUuid(), new NopeCompletion());
                applyHostUuidForRollback = struct.getOriginalHostUuid();
                batchApplyEips(eips, struct.getCurrentHostUuid(), new Completion(trigger) {
                    @Override
                    public void success() {
                        releaseHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            private void vmStoppedOnTheSameHost(final FlowTrigger trigger) {
                batchDeleteEips(eips, struct.getCurrentHostUuid(), new Completion(trigger) {
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

            private void vmRunningOnTheHost(final FlowTrigger trigger) {
                batchApplyEips(eips, struct.getCurrentHostUuid(), new Completion(trigger) {
                    @Override
                    public void success() {
                        releaseHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (eips == null) {
                    trigger.rollback();
                    return;
                }

                if (releaseHostUuidForRollback != null) {
                    batchDeleteEips(eips, releaseHostUuidForRollback, new NopeCompletion());
                }
                if (applyHostUuidForRollback != null) {
                    batchApplyEips(eips, applyHostUuidForRollback, new Completion(null) {
                        @Override
                        public void success() {
                            // pass
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            N.New(VmInstanceVO.class, vm.getUuid()).warn_("after migration, failed to apply EIPs[uuids:%s] to the vm[uuid:%s, name:%s] on the destination host[uuid:%s], %s." +
                                            "You may need to reboot the VM to resolve the issue",
                                    eips.stream().map(e -> e.vip).collect(Collectors.toList()), vm.getUuid(), vm.getName(), applyHostUuidForRollback, errorCode);
                        }
                    });
                }

                trigger.rollback();
            }
        };
    }

    @Transactional
    private Map<String, String> getPublicL3BridgeNamesByVipUuids(List<String> vipsUuids) {
        String sql = "select l3.uuid, vip.uuid from L3NetworkVO l3, VipVO vip where vip.l3NetworkUuid = l3.uuid and vip.uuid in (:uuids)";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("uuids", vipsUuids);
        List<Tuple> ts = q.getResultList();
        Map<String, String> vipL3 = new HashMap<String, String>();
        for (Tuple t : ts) {
            String l3Uuid = t.get(0, String.class);
            String vipUuid = t.get(1, String.class);
            vipL3.put(vipUuid, l3Uuid);
        }

        DebugUtils.Assert(!vipL3.isEmpty(), "how can we get an empty public L3Network list?");

        Map<String, String> brNames = new BridgeNameFinder().findByL3Uuids(vipL3.values());
        Map<String, String> vipBr = new HashMap<String, String>();
        for (Map.Entry<String, String> e : vipL3.entrySet()) {
            vipBr.put(e.getKey(), brNames.get(e.getValue()));
        }
        return vipBr;
    }

    @Transactional(readOnly = true)
    private List<EipTO> getEipsByNics(List<VmNicVO> vmNics) {
        List<String> nicUuids = CollectionUtils.transformToList(vmNics, new Function<String, VmNicVO>() {
            @Override
            public String call(VmNicVO arg) {
                return arg.getUuid();
            }
        });

        nicUuids = new NetworkServiceFilter().filterNicByServiceTypeAndProviderType(nicUuids, EipConstant.EIP_NETWORK_SERVICE_TYPE, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        if (nicUuids.isEmpty()) {
            return null;
        }

        String sql = "select eip from EipVO eip where eip.vmNicUuid in (:nicUuids)";
        TypedQuery<EipVO> q = dbf.getEntityManager().createQuery(sql, EipVO.class);
        q.setParameter("nicUuids", nicUuids);
        List<EipVO> eips = q.getResultList();
        if (eips.isEmpty()) {
            return null;
        }

        List<String> vipUuids = CollectionUtils.transformToList(eips, new Function<String, EipVO>() {
            @Override
            public String call(EipVO arg) {
                return arg.getVipUuid();
            }
        });
        sql = "select vip from VipVO vip where vip.uuid in (:uuids)";
        TypedQuery<VipVO> vq = dbf.getEntityManager().createQuery(sql, VipVO.class);
        vq.setParameter("uuids", vipUuids);
        List<VipVO> vips = vq.getResultList();
        final Map<String, VipVO> vipMap = new HashMap<String, VipVO>();
        for (VipVO v : vips) {
            vipMap.put(v.getUuid(), v);
        }

        List<String> l3Uuids = CollectionUtils.transformToList(vmNics, new Function<String, VmNicVO>() {
            @Override
            public String call(VmNicVO arg) {
                return arg.getL3NetworkUuid();
            }
        });

        final Map<String, VmNicVO> nicMap = new HashMap<String, VmNicVO>();
        for (VmNicVO nic : vmNics) {
            nicMap.put(nic.getUuid(), nic);
        }
        final Map<String, String> bridgeNames = new BridgeNameFinder().findByL3Uuids(l3Uuids);
        final Map<String, String> pubBridgeNames = getPublicL3BridgeNamesByVipUuids(vipUuids);

        return CollectionUtils.transformToList(eips, new Function<EipTO, EipVO>() {
            @Override
            public EipTO call(EipVO eip) {
                EipTO to = new EipTO();
                VmNicVO nic = nicMap.get(eip.getVmNicUuid());
                VipVO vip = vipMap.get(eip.getVipUuid());
                to.eipUuid = eip.getUuid();
                to.vmUuid = nic.getVmInstanceUuid();
                to.nicName = nic.getInternalName();
                to.nicGateway = nic.getGateway();
                to.nicNetmask = nic.getNetmask();
                to.nicIp = nic.getIp();
                to.nicMac = nic.getMac();
                to.nicUuid = nic.getUuid();
                to.vip = eip.getVipIp();
                to.vipGateway = vip.getGateway();
                to.vipNetmask = vip.getNetmask();
                to.vmBridgeName = bridgeNames.get(nic.getL3NetworkUuid());
                to.publicBridgeName = pubBridgeNames.get(eip.getVipUuid());
                return to;
            }
        });
    }

    private void batchDeleteEips(List<EipTO> eips, String hostUuid, final Completion completion) {
        batchDeleteEips(eips, hostUuid, false, completion);
    }

    private void batchDeleteEips(final List<EipTO> eips, final String hostUuid, boolean noHostStatusCheck, final Completion completion) {
        BatchDeleteEipCmd cmd = new BatchDeleteEipCmd();
        cmd.eips = eips;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setPath(BATCH_DELETE_EIP_PATH);
        msg.setNoStatusCheck(noHostStatusCheck);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {

                    ErrorCode err = reply.getError();
                    if (err.isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {

                        FlatEipGC gc = new FlatEipGC();
                        gc.eips = eips;
                        gc.hostUuid = hostUuid;
                        gc.NAME = String.format("gc-flat-eips-on-hosts-%s", hostUuid);
                        gc.submit();

                        completion.success();
                    } else {
                        completion.fail(reply.getError());
                    }

                    return;
                }

                KVMHostAsyncHttpCallReply ar = reply.castReply();
                AgentRsp rsp = ar.toResponse(AgentRsp.class);
                if (!rsp.success) {
                    completion.fail(operr(rsp.error));
                    return;
                }

                completion.success();
            }
        });
    }

    private void batchApplyEips(List<EipTO> eips, String hostUuid, final Completion completion) {
        batchApplyEips(eips, hostUuid, false, completion);
    }

    private void batchApplyEips(List<EipTO> eips, String hostUuid, boolean noHostStatusCheck, final Completion completion) {
        BatchApplyEipCmd cmd = new BatchApplyEipCmd();
        cmd.eips = eips;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setPath(BATCH_APPLY_EIP_PATH);
        msg.setNoStatusCheck(noHostStatusCheck);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply ar = reply.castReply();
                AgentRsp rsp = ar.toResponse(AgentRsp.class);
                if (!rsp.success) {
                    completion.fail(operr(rsp.error));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "sync-distributed-eip";

            @Transactional(readOnly = true)
            private List<EipTO> getEipsOnTheHost() {
                List<VmNicVO> vmNics = new VmNicFinder().findVmNicsByHostUuid(context.getInventory().getUuid());
                if (vmNics == null) {
                    return null;
                }

                return getEipsByNics(vmNics);
            }

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<EipTO> tos = getEipsOnTheHost();
                if (tos == null) {
                    trigger.next();
                    return;
                }

                batchApplyEips(tos, context.getInventory().getUuid(), true, new Completion(trigger) {
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


    @Transactional(readOnly = true)
    private String getHostUuidByVmUuid(String vmUuid) {
        String sql = "select h.uuid from HostVO h, VmInstanceVO vm where h.uuid = vm.hostUuid and vm.uuid = :vmUuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("vmUuid", vmUuid);
        List<String> ret = q.getResultList();

        if (ret.isEmpty()) {
            VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
            if (vm == null) {
                throw new CloudRuntimeException(String.format("cannot find the vm[uuid:%s]", vmUuid));
            } else {
                throw new OperationFailureException(operr("unable to apply the EIP operation for the the vm[uuid:%s, state:%s], because cannot find the VM's hostUUid",
                                vmUuid, vm.getState()));
            }
        }

        return ret.get(0);
    }

    private EipTO eipStructToEipTO(EipStruct struct) {
        EipTO to = new EipTO();
        to.eipUuid = struct.getEip().getUuid();
        to.vmUuid = struct.getNic().getVmInstanceUuid();
        to.nicUuid = struct.getNic().getUuid();
        to.nicName = struct.getNic().getInternalName();
        to.nicIp = struct.getNic().getIp();
        to.nicMac = struct.getNic().getMac();
        to.nicNetmask = struct.getNic().getNetmask();
        to.nicGateway = struct.getNic().getGateway();
        to.vip = struct.getVip().getIp();
        to.vipGateway = struct.getVip().getGateway();
        to.vipNetmask = struct.getVip().getNetmask();
        to.vmBridgeName = new BridgeNameFinder().findByL3Uuid(struct.getNic().getL3NetworkUuid());
        to.publicBridgeName = new BridgeNameFinder().findByL3Uuid(struct.getVip().getL3NetworkUuid());
        return to;
    }

    @Override
    public void applyEip(EipStruct struct, final Completion completion) {
        ApplyEipCmd cmd = new ApplyEipCmd();
        cmd.eip = eipStructToEipTO(struct);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(getHostUuidByVmUuid(cmd.eip.vmUuid));
        msg.setPath(APPLY_EIP_PATH);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply ar = reply.castReply();
                AgentRsp rsp = ar.toResponse(AgentRsp.class);
                if (!rsp.success) {
                    completion.fail(operr(rsp.error));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    public void revokeEip(EipStruct struct, final Completion completion) {
        VmInstanceState state = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, struct.getNic().getVmInstanceUuid())
                .select(VmInstanceVO_.state)
                .findValue();
        if (EipConstant.noNeedApplyOnBackendVmStates.contains(state)) {
            // eip netns has been revoke when vm is stopped,
            completion.success();
            return;
        }

        final DeleteEipCmd cmd = new DeleteEipCmd();
        cmd.eip = eipStructToEipTO(struct);

        final KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(getHostUuidByVmUuid(cmd.eip.vmUuid));
        msg.setPath(DELETE_EIP_PATH);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {

                    ErrorCode err = reply.getError();
                    if (err.isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {

                        FlatEipGC gc = new FlatEipGC();
                        gc.eips = list(cmd.eip);
                        gc.hostUuid = msg.getHostUuid();
                        gc.NAME = String.format("gc-eips-on-host-%s", msg.getHostUuid());
                        gc.submit();

                        completion.success();
                    } else {
                        completion.fail(reply.getError());
                    }

                    return;
                }

                KVMHostAsyncHttpCallReply ar = reply.castReply();
                AgentRsp rsp = ar.toResponse(AgentRsp.class);
                if (!rsp.success) {
                    completion.fail(operr(rsp.error));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    public String getNetworkServiceProviderType() {
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING;
    }
}
