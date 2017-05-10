package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.UserdataBuilder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkDeleteExtensionPoint;
import org.zstack.header.network.l3.L3NetworkException;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefInventory;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceProviderVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.*;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;
import org.zstack.network.service.NetworkProviderFinder;
import org.zstack.network.service.NetworkServiceFilter;
import org.zstack.network.service.userdata.UserdataBackend;
import org.zstack.network.service.userdata.UserdataConstant;
import org.zstack.network.service.userdata.UserdataGlobalProperty;
import org.zstack.network.service.userdata.UserdataStruct;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by frank on 10/13/2015.
 */
public class FlatUserdataBackend implements UserdataBackend, KVMHostConnectExtensionPoint,
        L3NetworkDeleteExtensionPoint, VmInstanceMigrateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FlatUserdataBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    private FlatDhcpBackend dhcpBackend;

    public static final String APPLY_USER_DATA = "/flatnetworkprovider/userdata/apply";
    public static final String BATCH_APPLY_USER_DATA = "/flatnetworkprovider/userdata/batchapply";
    public static final String RELEASE_USER_DATA = "/flatnetworkprovider/userdata/release";
    public static final String CLEANUP_USER_DATA = "/flatnetworkprovider/userdata/cleanup";

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "prepare-userdata";

            @Transactional(readOnly = true)
            private List<String> getVmsNeedUserdataOnHost() {
                String sql = "select vm.uuid from VmInstanceVO vm where vm.hostUuid = :huuid and vm.state = :state";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("state", VmInstanceState.Running);
                q.setParameter("huuid", context.getInventory().getUuid());
                List<String> vmUuids = q.getResultList();
                if (vmUuids.isEmpty()) {
                    return null;
                }

                vmUuids = new NetworkServiceFilter().filterVmByServiceTypeAndProviderType(vmUuids, UserdataConstant.USERDATA_TYPE_STRING, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
                if (vmUuids.isEmpty()) {
                    return null;
                }

                return vmUuids;
            }

            class VmIpL3Uuid {
                String vmIp;
                String l3Uuid;
                String dhcpServerIp;
            }

            @Transactional(readOnly = true)
            private Map<String, VmIpL3Uuid> getVmIpL3Uuid(List<String> vmUuids) {
                String sql = "select vm.uuid, nic.ip, nic.l3NetworkUuid from VmInstanceVO vm," +
                        "VmNicVO nic, NetworkServiceL3NetworkRefVO ref," +
                        "NetworkServiceProviderVO pro where " +
                        " vm.uuid = nic.vmInstanceUuid and vm.uuid in (:uuids)" +
                        " and nic.l3NetworkUuid = vm.defaultL3NetworkUuid" +
                        " and ref.networkServiceProviderUuid = pro.uuid" +
                        " and ref.l3NetworkUuid = vm.defaultL3NetworkUuid" +
                        " and pro.type = :proType";

                TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("uuids", vmUuids);
                q.setParameter("proType", FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
                List<Tuple> ts = q.getResultList();

                Map<String, VmIpL3Uuid> ret = new HashMap<String, VmIpL3Uuid>();
                for (Tuple t : ts) {
                    String vmUuid = t.get(0, String.class);
                    VmIpL3Uuid v = new VmIpL3Uuid();
                    v.vmIp = t.get(1, String.class);
                    v.l3Uuid = t.get(2, String.class);
                    ret.put(vmUuid, v);
                }

                return ret;
            }

            private List<UserdataTO> getUserData() {
                List<String> vmUuids = getVmsNeedUserdataOnHost();
                if (vmUuids == null) {
                    return null;
                }

                Map<String, VmIpL3Uuid> vmipl3 = getVmIpL3Uuid(vmUuids);
                if (vmipl3.isEmpty()) {
                    return null;
                }

                // filter out vm that not using flat network provider
                vmUuids = vmUuids.stream().filter(vmipl3::containsKey).collect(Collectors.toList());
                if (vmUuids.isEmpty()) {
                    return null;
                }

                Map<String, String> userdata = new UserdataBuilder().buildByVmUuids(vmUuids);
                Set<String> l3Uuids = new HashSet<String>();
                for (VmIpL3Uuid l : vmipl3.values()) {
                    l.dhcpServerIp = dhcpBackend.allocateDhcpIp(l.l3Uuid).getIp();
                    l3Uuids.add(l.l3Uuid);
                }

                Map<String, String> bridgeNames = new BridgeNameFinder().findByL3Uuids(l3Uuids);

                List<UserdataTO> tos = new ArrayList<UserdataTO>();
                for (String vmuuid : vmUuids) {
                    UserdataTO to = new UserdataTO();
                    MetadataTO mto = new MetadataTO();
                    mto.vmUuid = vmuuid;
                    to.metadata = mto;

                    VmIpL3Uuid l = vmipl3.get(vmuuid);
                    to.dhcpServerIp = l.dhcpServerIp;
                    to.vmIp = l.vmIp;
                    to.bridgeName = bridgeNames.get(l.l3Uuid);
                    to.namespaceName = FlatDhcpBackend.makeNamespaceName(to.bridgeName, l.l3Uuid);
                    to.userdata = userdata.get(vmuuid);
                    to.port = UserdataGlobalProperty.HOST_PORT;
                    tos.add(to);
                }

                return tos;
            }

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<UserdataTO> tos = getUserData();
                if (tos == null) {
                    trigger.next();
                    return;
                }

                BatchApplyUserdataCmd cmd = new BatchApplyUserdataCmd();
                cmd.userdata = tos;
                cmd.rebuild = true;

                new KvmCommandSender(context.getInventory().getUuid(), true).send(cmd, BATCH_APPLY_USER_DATA, new KvmCommandFailureChecker() {
                    @Override
                    public ErrorCode getError(KvmResponseWrapper wrapper) {
                        AgentResponse rsp = wrapper.getResponse(AgentResponse.class);
                        return rsp.isSuccess() ? null : operr(rsp.getError());
                    }
                }, new ReturnValueCompletion<KvmResponseWrapper>(trigger) {
                    @Override
                    public void success(KvmResponseWrapper returnValue) {
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

    @Override
    public String preDeleteL3Network(L3NetworkInventory inventory) throws L3NetworkException {
        return null;
    }

    @Override
    public void beforeDeleteL3Network(L3NetworkInventory inventory) {
    }

    @Override
    public void afterDeleteL3Network(L3NetworkInventory l3) {
        Optional<NetworkServiceL3NetworkRefInventory> o = l3.getNetworkServices().stream().filter(n -> n.getNetworkServiceType().equals(UserdataConstant.USERDATA_TYPE_STRING)).findAny();
        if (!o.isPresent()) {
            return;
        }

        NetworkServiceL3NetworkRefInventory ref = o.get();
        if (!dbf.isExist(ref.getNetworkServiceProviderUuid(), NetworkServiceProviderVO.class)) {
            return;
        }

        List<String> hostUuids = new Callable<List<String>>() {
            @Override
            @Transactional(readOnly = true)
            public List<String> call() {
                String sql = "select h.uuid from HostVO h, L2NetworkClusterRefVO ref where h.clusterUuid = ref.clusterUuid" +
                        " and ref.l2NetworkUuid = :l2Uuid and h.hypervisorType = :hvType";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("l2Uuid", l3.getL2NetworkUuid());
                q.setParameter("hvType", KVMConstant.KVM_HYPERVISOR_TYPE);
                return q.getResultList();
            }
        }.call();

        if (hostUuids.isEmpty()) {
            return;
        }

        CleanupUserdataCmd cmd = new CleanupUserdataCmd();
        cmd.bridgeName = new BridgeNameFinder().findByL3Uuid(l3.getUuid());

        for (String huuid : hostUuids) {
            new KvmCommandSender(huuid).send(cmd, CLEANUP_USER_DATA, new KvmCommandFailureChecker() {
                @Override
                public ErrorCode getError(KvmResponseWrapper w) {
                    CleanupUserdataRsp rsp = w.getResponse(CleanupUserdataRsp.class);
                    return rsp.isSuccess() ? null : operr(rsp.getError());
                }
            }, new ReturnValueCompletion<KvmResponseWrapper>(null) {
                @Override
                public void success(KvmResponseWrapper w) {
                    logger.debug(String.format("successfully cleanup userdata service on the host[uuid:%s] for the deleted L3 network[uuid:%s, name:%s]",
                            huuid, l3.getUuid(), l3.getName()));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    //TODO: Add GC
                    logger.warn(errorCode.toString());
                }
            });
        }
    }

    private UserdataStruct makeUserdataStructForMigratingVm(VmInstanceInventory inv, String hostUuid) {
        String providerType = new NetworkProviderFinder().getNetworkProviderTypeByNetworkServiceType(inv.getDefaultL3NetworkUuid(), UserdataConstant.USERDATA_TYPE_STRING);
        if (providerType == null || !FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING.equals(providerType)) {
            return null;
        }

        String userdata = new UserdataBuilder().buildByVmUuid(inv.getUuid());
        if (userdata == null) {
            return null;
        }

        UserdataStruct struct = new UserdataStruct();
        struct.setParametersFromVmInventory(inv);
        struct.setHostUuid(hostUuid);
        struct.setL3NetworkUuid(inv.getDefaultL3NetworkUuid());
        struct.setUserdata(userdata);
        return struct;
    }


    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        UserdataStruct struct = makeUserdataStructForMigratingVm(inv, destHostUuid);
        if (struct == null) {
            return;
        }

        FutureCompletion completion = new FutureCompletion(null);
        applyUserdata(struct, completion);
        completion.await();

        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {

    }

    public static class UserdataReleseGC extends TimeBasedGarbageCollector {
        public static long INTERVAL = 300;

        @GC
        public UserdataStruct struct;

        @Override
        protected void triggerNow(GCCompletion completion) {
            HostStatus status = Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid, struct.getHostUuid()).findValue();
            if (status == null) {
                // host deleted
                completion.cancel();
                return;
            }

            if (status != HostStatus.Connected) {
                completion.fail(operr("host[uuid:%s] is not connected", struct.getHostUuid()));
                return;
            }

            ReleaseUserdataCmd cmd = new ReleaseUserdataCmd();
            cmd.hostUuid = struct.getHostUuid();
            cmd.bridgeName = new BridgeNameFinder().findByL3Uuid(struct.getL3NetworkUuid());
            cmd.namespaceName = FlatDhcpBackend.makeNamespaceName(cmd.bridgeName, struct.getL3NetworkUuid());
            cmd.vmIp = CollectionUtils.find(struct.getVmNics(), arg -> arg.getL3NetworkUuid().equals(struct.getL3NetworkUuid()) ? arg.getIp() : null);

            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setHostUuid(struct.getHostUuid());
            msg.setCommand(cmd);
            msg.setCommandTimeout(TimeUnit.MINUTES.toMillis(5));
            msg.setPath(RELEASE_USER_DATA);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, struct.getHostUuid());
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }

                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    ReleaseUserdataRsp rsp = r.toResponse(ReleaseUserdataRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr(rsp.getError()));
                        return;
                    }

                    completion.success();
                }
            });
        }
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        UserdataStruct struct = makeUserdataStructForMigratingVm(inv, srcHostUuid);
        if (struct == null) {
            return;
        }

        releaseUserdata(struct, new Completion(null) {
            @Override
            public void success() {
                // nothing
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to release userdata on the source host[uuid:%s]" +
                        " for the migrated VM[uuid: %s, name:%s], %s. GC will take care it", srcHostUuid, inv.getUuid(), inv.getName(), errorCode));
                UserdataReleseGC gc = new UserdataReleseGC();
                gc.struct = struct;
                gc.submit(UserdataReleseGC.INTERVAL, TimeUnit.SECONDS);
            }
        });
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        UserdataStruct struct = makeUserdataStructForMigratingVm(inv, destHostUuid);
        if (struct == null) {
            return;
        }

        // clean the userdata that set on the dest host
        releaseUserdata(struct, new Completion(null) {
            @Override
            public void success() {
                // nothing
            }

            @Override
            public void fail(ErrorCode errorCode) {
                UserdataReleseGC gc = new UserdataReleseGC();
                gc.struct = struct;
                gc.submit(UserdataReleseGC.INTERVAL, TimeUnit.SECONDS);
            }
        });
    }

    public static class UserdataTO {
        public MetadataTO metadata;
        public String userdata;
        public String vmIp;
        public String dhcpServerIp;
        public String bridgeName;
        public String namespaceName;
        public int port;
    }

    public static class MetadataTO {
        public String vmUuid;
    }

    public static class CleanupUserdataCmd extends KVMAgentCommands.AgentCommand {
        public String bridgeName;
    }

    public static class CleanupUserdataRsp extends KVMAgentCommands.AgentResponse {

    }

    public static class BatchApplyUserdataCmd extends KVMAgentCommands.AgentCommand {
        public List<UserdataTO> userdata;
        public boolean rebuild;
    }

    public static class ApplyUserdataCmd extends KVMAgentCommands.AgentCommand {
        public String hostUuid;
        public UserdataTO userdata;
    }

    public static class ApplyUserdataRsp extends KVMAgentCommands.AgentResponse {

    }

    public static class ReleaseUserdataCmd extends KVMAgentCommands.AgentCommand {
        public String hostUuid;
        public String vmIp;
        public String bridgeName;
        public String namespaceName;
    }

    public static class ReleaseUserdataRsp extends KVMAgentCommands.AgentResponse {
    }

    @Override
    public NetworkServiceProviderType getProviderType() {
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE;
    }

    @Override
    public void applyUserdata(final UserdataStruct struct, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("flat-network-userdata-set-for-vm-%s", struct.getVmUuid()));
        chain.then(new ShareFlow() {
            String dhcpServerIp;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "get-dhcp-server-ip";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        FlatDhcpAcquireDhcpServerIpMsg msg = new FlatDhcpAcquireDhcpServerIpMsg();
                        msg.setL3NetworkUuid(struct.getL3NetworkUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, FlatNetworkServiceConstant.SERVICE_ID, struct.getL3NetworkUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                dhcpServerIp = ((FlatDhcpAcquireDhcpServerIpReply) reply).getIp();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "apply-user-data";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ApplyUserdataCmd cmd = new ApplyUserdataCmd();
                        cmd.hostUuid = struct.getHostUuid();

                        MetadataTO to = new MetadataTO();
                        to.vmUuid = struct.getVmUuid();
                        UserdataTO uto = new UserdataTO();
                        uto.metadata = to;
                        uto.userdata = struct.getUserdata();
                        uto.dhcpServerIp = dhcpServerIp;
                        uto.vmIp = CollectionUtils.find(struct.getVmNics(), new Function<String, VmNicInventory>() {
                            @Override
                            public String call(VmNicInventory arg) {
                                return arg.getL3NetworkUuid().equals(struct.getL3NetworkUuid()) ? arg.getIp() : null;
                            }
                        });
                        uto.bridgeName = new BridgeNameFinder().findByL3Uuid(struct.getL3NetworkUuid());
                        uto.namespaceName = FlatDhcpBackend.makeNamespaceName(uto.bridgeName, struct.getL3NetworkUuid());
                        uto.port = UserdataGlobalProperty.HOST_PORT;
                        cmd.userdata = uto;

                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setHostUuid(struct.getHostUuid());
                        msg.setCommand(cmd);
                        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
                        msg.setPath(APPLY_USER_DATA);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, struct.getHostUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                KVMHostAsyncHttpCallReply r = reply.castReply();
                                ApplyUserdataRsp rsp = r.toResponse(ApplyUserdataRsp.class);
                                if (!rsp.isSuccess()) {
                                    trigger.fail(operr(rsp.getError()));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }


    @Override
    public void releaseUserdata(final UserdataStruct struct, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("flat-network-userdata-release-for-vm-%s", struct.getVmUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "release-user-data";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ReleaseUserdataCmd cmd = new ReleaseUserdataCmd();
                        cmd.hostUuid = struct.getHostUuid();
                        cmd.bridgeName = new BridgeNameFinder().findByL3Uuid(struct.getL3NetworkUuid());
                        cmd.namespaceName = FlatDhcpBackend.makeNamespaceName(cmd.bridgeName, struct.getL3NetworkUuid());
                        cmd.vmIp = CollectionUtils.find(struct.getVmNics(), new Function<String, VmNicInventory>() {
                            @Override
                            public String call(VmNicInventory arg) {
                                return arg.getL3NetworkUuid().equals(struct.getL3NetworkUuid()) ? arg.getIp() : null;
                            }
                        });

                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setHostUuid(struct.getHostUuid());
                        msg.setCommand(cmd);
                        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
                        msg.setPath(RELEASE_USER_DATA);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, struct.getHostUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                KVMHostAsyncHttpCallReply r = reply.castReply();
                                ReleaseUserdataRsp rsp = r.toResponse(ReleaseUserdataRsp.class);
                                if (!rsp.isSuccess()) {
                                    trigger.fail(operr(rsp.getError()));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }
}
