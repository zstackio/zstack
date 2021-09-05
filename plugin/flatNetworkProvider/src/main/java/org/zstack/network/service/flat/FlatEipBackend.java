package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
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
import org.zstack.header.network.l3.NormalIpRangeVO;
import org.zstack.header.network.l3.NormalIpRangeVO_;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.service.AfterApplyFlatEipExtensionPoint;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.network.service.NetworkServiceFilter;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.eip.*;
import org.zstack.network.service.flat.FlatNetworkServiceConstant.AgentCmd;
import org.zstack.network.service.flat.FlatNetworkServiceConstant.AgentRsp;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/4/4.
 */
public class FlatEipBackend implements EipBackend, KVMHostConnectExtensionPoint,
        VmAbnormalLifeCycleExtensionPoint, VmInstanceMigrateExtensionPoint, FilterVmNicsForEipInVirtualRouterExtensionPoint, GetL3NetworkForEipInVirtualRouterExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FlatEipBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected NetworkServiceManager nsMgr;

    public static class EipTO {
        public String eipUuid;
        public String vipUuid;
        public String vmUuid;
        public String nicUuid;
        public Integer ipVersion;
        public String vip;
        public String vipNetmask;
        public String vipGateway;
        public Integer vipPrefixLen;
        public String nicIp;
        public String nicMac;
        public String nicGateway;
        public String nicNetmask;
        public Integer nicPrefixLen;
        public String nicName;
        public String vmBridgeName;
        public String publicBridgeName;
        public boolean skipArpCheck;
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

        batchDeleteEips(eips, srcHostUuid, new Completion(null) {
            @Override
            public void success() {
                batchApplyEips(eips, inv.getHostUuid(), new Completion(null) {
                    @Override
                    public void success() {
                        logger.warn(String.format("after migration, successfully applied EIPs[uuids:%s] to the vm[uuid:%s, name:%s] on the destination host[uuid:%s] after delete eip on src host[uuid:%s] succeeded",
                                eips.stream().map(e -> e.vip).collect(Collectors.toList()), inv.getUuid(), inv.getName(), inv.getHostUuid(), srcHostUuid));
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("after migration, failed to apply EIPs[uuids:%s] to the vm[uuid:%s, name:%s] on the destination host[uuid:%s] after delete eip on src host[uuid:%s] succeeded, %s",
                                eips.stream().map(e -> e.vip).collect(Collectors.toList()), inv.getUuid(), inv.getName(), inv.getHostUuid(), srcHostUuid, errorCode));
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                batchApplyEips(eips, inv.getHostUuid(), new Completion(null) {
                    @Override
                    public void success() {
                        logger.warn(String.format("after migration, successfully applied EIPs[uuids:%s] to the vm[uuid:%s, name:%s] on the destination host[uuid:%s] after delete eip on src host[uuid:%s] failed",
                                eips.stream().map(e -> e.vip).collect(Collectors.toList()), inv.getUuid(), inv.getName(), inv.getHostUuid(), srcHostUuid));
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("after migration, failed to apply EIPs[uuids:%s] to the vm[uuid:%s, name:%s] on the destination host[uuid:%s] after delete eip on src host[uuid:%s] failed, %s",
                                eips.stream().map(e -> e.vip).collect(Collectors.toList()), inv.getUuid(), inv.getName(), inv.getHostUuid(), srcHostUuid, errorCode));
                    }
                });
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
                            logger.warn(String.format("after migration, failed to apply EIPs[uuids:%s] to the vm[uuid:%s, name:%s] on the destination host[uuid:%s], %s." +
                                            "You may need to reboot the VM to resolve the issue",
                                    eips.stream().map(e -> e.vip).collect(Collectors.toList()), vm.getUuid(), vm.getName(), applyHostUuidForRollback, errorCode));
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

        Map<String, List<String>> nicGuestipMap = new HashMap<String, List<String>>();
        for (EipVO eip : eips) {
            nicGuestipMap.computeIfAbsent(eip.getVmNicUuid(), k -> new ArrayList<String>()).add(eip.getGuestIp());
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

        List<String> l3Uuids = new ArrayList<>();
        for (VmNicVO nicVO : vmNics) {
            for (UsedIpVO ip : nicVO.getUsedIps()) {
                List<String> guestips = nicGuestipMap.get(nicVO.getUuid());
                if (guestips != null && guestips.contains(ip.getIp())) {
                    l3Uuids.add(ip.getL3NetworkUuid());
                }
            }
        }
        l3Uuids = l3Uuids.stream().distinct().collect(Collectors.toList());

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
                if (nic.getIp() == null) {
                    return null;
                }
                to.eipUuid = eip.getUuid();
                to.vmUuid = nic.getVmInstanceUuid();
                to.nicName = nic.getInternalName();
                for (UsedIpVO ip : nic.getUsedIps()) {
                    if (ip.getIp().equals(eip.getGuestIp())) {
                        to.ipVersion = ip.getIpVersion();
                        to.nicIp = ip.getIp();
                        to.nicGateway = ip.getGateway();
                        to.nicNetmask = ip.getNetmask();
                        NormalIpRangeVO ipr = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.uuid, ip.getIpRangeUuid()).find();
                        to.nicPrefixLen = ipr.getPrefixLen();
                        to.vmBridgeName = bridgeNames.get(ip.getL3NetworkUuid());
                    }
                }
                to.nicMac = nic.getMac();
                to.nicUuid = nic.getUuid();
                to.vip = eip.getVipIp();
                to.vipUuid = eip.getVipUuid();
                to.vipGateway = vip.getGateway();
                to.vipNetmask = vip.getNetmask();
                to.vipUuid = vip.getUuid();
                List<NormalIpRangeVO> vipIprs = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, vip.getL3NetworkUuid())
                        .eq(NormalIpRangeVO_.ipVersion, to.ipVersion).list();
                to.vipPrefixLen = vipIprs.get(0).getPrefixLen();
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
                        gc.deduplicateSubmit();

                        completion.success();
                    } else {
                        completion.fail(reply.getError());
                    }

                    return;
                }

                KVMHostAsyncHttpCallReply ar = reply.castReply();
                AgentRsp rsp = ar.toResponse(AgentRsp.class);
                if (!rsp.success) {
                    completion.fail(operr("operation error, because:%s", rsp.error));
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
                    completion.fail(operr("operation error, because:%s", rsp.error));
                    return;
                }

                List<String> vipUuids = CollectionUtils.transformToList(eips, new Function<String, EipTO>() {
                    @Override
                    public String call(EipTO arg) {
                        return arg.vipUuid;
                    }
                });
                for (AfterApplyFlatEipExtensionPoint ext : pluginRgty.getExtensionList(AfterApplyFlatEipExtensionPoint.class)) {
                    ext.AfterApplyFlatEip(vipUuids, hostUuid);
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
        to.nicMac = struct.getNic().getMac();
        to.skipArpCheck = struct.isSkipArpCheck();
        if (struct.getGuestIp() != null) {
            /* for attachEip */
            to.nicIp = struct.getGuestIp().getIp();
            to.nicNetmask = struct.getGuestIp().getNetmask();
            to.nicGateway = struct.getGuestIp().getGateway();
            to.ipVersion = struct.getGuestIp().getIpVersion();
            to.vmBridgeName = new BridgeNameFinder().findByL3Uuid(struct.getGuestIp().getL3NetworkUuid());
        } else {
            /* for detachEip */
            to.nicIp = struct.getEip().getGuestIp();
            to.ipVersion = NetworkUtils.getIpversion(to.nicIp);
        }
        if (struct.getGuestIpRange() != null) {
            /* when delete eip, no need nicPrefixLen */
            to.nicPrefixLen = struct.getGuestIpRange().getPrefixLen();
        }
        to.vip = struct.getVip().getIp();
        to.vipGateway = struct.getVip().getGateway();
        to.vipNetmask = struct.getVip().getNetmask();
        to.vipUuid = struct.getVip().getUuid();
        List<NormalIpRangeVO> iprs = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, struct.getVip().getL3NetworkUuid())
                .eq(NormalIpRangeVO_.ipVersion, to.ipVersion).list();
        to.vipPrefixLen = iprs.get(0).getPrefixLen();
        to.publicBridgeName = new BridgeNameFinder().findByL3Uuid(struct.getVip().getL3NetworkUuid());
        return to;
    }

    @Override
    public void applyEip(EipStruct struct, final Completion completion) {
        ApplyEipCmd cmd = new ApplyEipCmd();
        cmd.eip = eipStructToEipTO(struct);

        String hostUuid = getHostUuidByVmUuid(cmd.eip.vmUuid);
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(APPLY_EIP_PATH);
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
                    completion.fail(operr("operation error, because:%s", rsp.error));
                    return;
                }

                for (AfterApplyFlatEipExtensionPoint ext : pluginRgty.getExtensionList(AfterApplyFlatEipExtensionPoint.class)) {
                    ext.AfterApplyFlatEip(asList(struct.getVip().getUuid()), hostUuid);
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
        if (struct.getHostUuid() != null) {
            msg.setHostUuid(struct.getHostUuid());
        } else {
            msg.setHostUuid(getHostUuidByVmUuid(cmd.eip.vmUuid));
        }
        msg.setPath(DELETE_EIP_PATH);
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
                        gc.deduplicateSubmit();

                        completion.success();
                    } else {
                        completion.fail(reply.getError());
                    }

                    return;
                }

                KVMHostAsyncHttpCallReply ar = reply.castReply();
                AgentRsp rsp = ar.toResponse(AgentRsp.class);
                if (!rsp.success) {
                    completion.fail(operr("operation error, because:%s", rsp.error));
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

    @Override
    public List<VmNicInventory> filterVmNicsForEipInVirtualRouter(VipInventory vip, List<VmNicInventory> vmNics) {
        /* if vmnic is in flat network, it will be filtered out if it already has eip attached */
        Map<String, NetworkServiceProviderType> l3Maps = new HashMap<>();
        List<VmNicInventory> ret = new ArrayList<>();
        boolean isIpv4 = NetworkUtils.isIpv4Address(vip.getIp());
        for (VmNicInventory nic : vmNics) {
            NetworkServiceProviderType l3ProviderType = l3Maps.get(nic.getL3NetworkUuid());
            if (l3ProviderType == null) {
                try {
                    NetworkServiceProviderType providerType = nsMgr.getTypeOfNetworkServiceProviderForService(nic.getL3NetworkUuid(), EipConstant.EIP_TYPE);
                    l3Maps.put(nic.getL3NetworkUuid(), providerType);
                } catch (Exception e) {
                    l3Maps.put(nic.getL3NetworkUuid(), FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE);
                }
                l3ProviderType = l3Maps.get(nic.getL3NetworkUuid());
            }

            /* vmnic in flat network, it can have only 1 eip */
            if (l3ProviderType == FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE) {
                List<String> vipIps = Q.New(EipVO.class).eq(EipVO_.vmNicUuid, nic.getUuid()).select(EipVO_.vipIp).listValues();
                boolean attached = false;
                for (String vipIp : vipIps) {
                    if (vipIp != null) {
                        boolean isVipIpv4 = NetworkUtils.isIpv4Address(vipIp);
                        if (isIpv4 == isVipIpv4) {
                            attached = true;
                            break;
                        }
                    }
                }
                if (attached) {
                    continue;
                }
            }

            ret.add(nic);
        }

        return ret;
    }

    @Override
    public List<String> getL3NetworkForEipInVirtualRouter(String networkServiceProviderType, VipInventory vip, List<String> l3Uuids) {
        if (networkServiceProviderType.equals(FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING)) {
            /* get vpc network or vrouter network */
            return SQL.New("select distinct l3.uuid" +
                    " from  L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO provider" +
                    " where l3.uuid = ref.l3NetworkUuid and ref.networkServiceProviderUuid = provider.uuid" +
                    " and ref.networkServiceType = :serviceType and provider.type = :providerType" +
                    " and l3.ipVersion in (:ipVersions) and l3.uuid in (:l3Uuids)")
                    .param("serviceType", EipConstant.EIP_NETWORK_SERVICE_TYPE)
                    .param("providerType", FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING)
                    .param("ipVersions", vip.getCandidateIpversion())
                    .param("l3Uuids", l3Uuids)
                    .list();
        }
        return new ArrayList<>();
    }
}
