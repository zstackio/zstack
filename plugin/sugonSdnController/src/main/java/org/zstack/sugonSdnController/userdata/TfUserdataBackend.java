package org.zstack.sugonSdnController.userdata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.UserdataBuilder;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
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
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.vm.*;
import org.zstack.kvm.*;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;
import org.zstack.network.service.NetworkProviderFinder;
import org.zstack.network.service.NetworkServiceFilter;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.userdata.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by fuwei on 11/15/2022.
 */
public class TfUserdataBackend implements UserdataBackend, KVMHostConnectExtensionPoint,
        VmInstanceMigrateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(TfUserdataBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private NetworkServiceManager nsMgr;

    public static final String APPLY_USER_DATA = "/tfnetworkprovider/userdata/apply";
    public static final String BATCH_APPLY_USER_DATA = "/tfnetworkprovider/userdata/batchapply";
    public static final String RELEASE_USER_DATA = "/tfnetworkprovider/userdata/release";

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "prepare-userdata";

            @Transactional(readOnly = true)
            private List<String> getVmsNeedUserdataOnHost() {
                String sql = "select vm.uuid from VmInstanceVO vm where vm.hostUuid = :huuid and vm.state = :state and vm.type = :type";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("state", VmInstanceState.Running);
                q.setParameter("huuid", context.getInventory().getUuid());
                q.setParameter("type", VmInstanceConstant.USER_VM_TYPE);
                List<String> vmUuids = q.getResultList();
                if (vmUuids.isEmpty()) {
                    return null;
                }

                vmUuids = new NetworkServiceFilter().filterVmByServiceTypeAndProviderType(vmUuids, UserdataConstant.USERDATA_TYPE_STRING, TfNetworkServiceConstant.TF_NETWORK_SERVICE_TYPE_STRING);
                if (vmUuids.isEmpty()) {
                    return null;
                }

                return vmUuids;
            }

            class VmIpL3Uuid {
                String vmIp;
                String netmask;
                String l3Uuid;
            }

            @Transactional(readOnly = true)
            private Map<String, VmIpL3Uuid> getVmIpL3Uuid(List<String> vmUuids) {
                String sql = "select vm.uuid, ip.ip, ip.l3NetworkUuid, ip.netmask from VmInstanceVO vm," +
                        "VmNicVO nic, NetworkServiceL3NetworkRefVO ref," +
                        "NetworkServiceProviderVO pro, UsedIpVO ip where " +
                        " vm.uuid = nic.vmInstanceUuid and vm.uuid in (:uuids)" +
                        " and nic.uuid = ip.vmNicUuid " +
                        " and ip.l3NetworkUuid = vm.defaultL3NetworkUuid and ip.ipVersion = :ipversion" +
                        " and ref.networkServiceProviderUuid = pro.uuid" +
                        " and ref.l3NetworkUuid = vm.defaultL3NetworkUuid" +
                        " and pro.type = :proType";

                TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("uuids", vmUuids);
                q.setParameter("proType", TfNetworkServiceConstant.TF_NETWORK_SERVICE_TYPE_STRING);
                /* current only ipv4 has userdata */
                q.setParameter("ipversion", IPv6Constants.IPv4);
                List<Tuple> ts = q.getResultList();

                Map<String, VmIpL3Uuid> ret = new HashMap<String, VmIpL3Uuid>();
                for (Tuple t : ts) {
                    String vmUuid = t.get(0, String.class);
                    VmIpL3Uuid v = new VmIpL3Uuid();
                    v.vmIp = t.get(1, String.class);
                    v.l3Uuid = t.get(2, String.class);
                    v.netmask = t.get(3, String.class);
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

                // filter out vm that not using tf network provider
                vmUuids = vmUuids.stream().filter(vmipl3::containsKey).collect(Collectors.toList());
                if (vmUuids.isEmpty()) {
                    return null;
                }

                Map<String, List<String>> userdata = new UserdataBuilder().buildByVmUuids(vmUuids);

                List<UserdataTO> tos = new ArrayList<UserdataTO>();
                for (String vmuuid : vmUuids) {
                    UserdataTO to = new UserdataTO();
                    MetadataTO mto = new MetadataTO();
                    mto.vmUuid = vmuuid;
                    mto.vmHostname = VmSystemTags.HOSTNAME.getTokenByResourceUuid(vmuuid, VmSystemTags.HOSTNAME_TOKEN);
                    to.metadata = mto;

                    VmIpL3Uuid l = vmipl3.get(vmuuid);
                    if (l.vmIp == null) {
                        continue;
                    }

                    to.vmIp = l.vmIp;
                    to.netmask = l.netmask;
                    if (userdata.get(vmuuid) != null) {
                        to.userdataList.addAll(userdata.get(vmuuid));
                    }
                    to.port = UserdataGlobalProperty.HOST_PORT;
                    to.l3NetworkUuid = l.l3Uuid;
                    to.agentConfig = new HashMap<>();
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
                        return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
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

    private UserdataStruct makeUserdataStructForMigratingVm(VmInstanceInventory inv, String hostUuid) {
        if (!nsMgr.isVmNeedNetworkService(inv.getType(), UserdataConstant.USERDATA_TYPE)) {
            return null;
        }

        String providerType = new NetworkProviderFinder().getNetworkProviderTypeByNetworkServiceType(inv.getDefaultL3NetworkUuid(), UserdataConstant.USERDATA_TYPE_STRING);
        if (!TfNetworkServiceConstant.TF_NETWORK_SERVICE_TYPE_STRING.equals(providerType)) {
            return null;
        }

        List<String> userdataList = new UserdataBuilder().buildByVmUuid(inv.getUuid());
        if (userdataList == null) {
            // Apply userdata anyway after migrating VM to redeploy lighttpd server
            // so that the VM can still send metric data to pushgateway through lighttpd
            userdataList = Collections.emptyList();
        }

        UserdataStruct struct = new UserdataStruct();
        struct.setParametersFromVmInventory(inv);
        struct.setHostUuid(hostUuid);
        struct.setL3NetworkUuid(inv.getDefaultL3NetworkUuid());
        struct.setUserdataList(userdataList);
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
            cmd.vmIp = CollectionUtils.find(struct.getVmNics(), arg -> arg.getL3NetworkUuid().equals(struct.getL3NetworkUuid()) ? arg.getIp() : null);
            cmd.vmUuid = struct.getVmUuid();

            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setHostUuid(struct.getHostUuid());
            msg.setCommand(cmd);
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
                        completion.fail(operr("operation error, because:%s", rsp.getError()));
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
        if (destHostUuid == null) {
            return;
        }

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
        public List<String> userdataList = new ArrayList<>();
        public String vmIp;
        public String netmask;
        public String l3NetworkUuid;
        public Map<String, String> agentConfig;
        public int port;
    }

    public static class MetadataTO {
        public String vmUuid;
        public String vmHostname;
    }

    public static class BatchApplyUserdataCmd extends KVMAgentCommands.AgentCommand {
        public List<UserdataTO> userdata;
        public boolean rebuild;
    }

    public static class ApplyUserdataCmd extends KVMAgentCommands.AgentCommand {
        public String hostUuid;
        public UserdataTO userdata;
    }

    public static class ApplyUserdataRsp extends AgentResponse {

    }

    public static class ReleaseUserdataCmd extends KVMAgentCommands.AgentCommand {
        public String hostUuid;
        public String vmIp;
        public String vmUuid;
    }

    public static class ReleaseUserdataRsp extends AgentResponse {
    }

    @Override
    public NetworkServiceProviderType getProviderType() {
        return TfNetworkServiceConstant.TF_NETWORK_SERVICE_TYPE;
    }

    private boolean hasMetedata(UserdataStruct struct) {
        return VmSystemTags.HOSTNAME.getTokenByResourceUuid(struct.getVmUuid(), VmSystemTags.HOSTNAME_TOKEN) != null;
    }

    private boolean hasUserdata(UserdataStruct struct) {
        return struct.getUserdataList() != null && !struct.getUserdataList().isEmpty();
    }

    @Override
    public void applyUserdata(final UserdataStruct struct, final Completion completion) {
        if (!UserdataGlobalConfig.OPEN_USERDATA_SERVICE_BY_DEFAULT.value(Boolean.class)) {
            if ( !hasMetedata(struct) && !hasUserdata(struct)) {
                completion.success();
                return;
            }
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("tf-network-userdata-set-for-vm-%s", struct.getVmUuid()));
        chain.then(new ShareFlow() {

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-user-data";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ApplyUserdataCmd cmd = new ApplyUserdataCmd();
                        cmd.hostUuid = struct.getHostUuid();

                        MetadataTO to = new MetadataTO();
                        to.vmUuid = struct.getVmUuid();
                        to.vmHostname = VmSystemTags.HOSTNAME.getTokenByResourceUuid(struct.getVmUuid(), VmSystemTags.HOSTNAME_TOKEN);
                        UserdataTO uto = new UserdataTO();
                        uto.metadata = to;
                        uto.userdataList = struct.getUserdataList();
                        uto.vmIp = CollectionUtils.find(struct.getVmNics(), new Function<String, VmNicInventory>() {
                            @Override
                            public String call(VmNicInventory arg) {
                                return arg.getL3NetworkUuid().equals(struct.getL3NetworkUuid()) ? arg.getIp() : null;
                            }
                        });
                        uto.netmask = CollectionUtils.find(struct.getVmNics(), new Function<String, VmNicInventory>() {
                            @Override
                            public String call(VmNicInventory arg) {
                                return arg.getL3NetworkUuid().equals(struct.getL3NetworkUuid()) ? arg.getNetmask() : null;
                            }
                        });
                        uto.port = UserdataGlobalProperty.HOST_PORT;
                        uto.l3NetworkUuid = struct.getL3NetworkUuid();
                        uto.agentConfig = new HashMap<>();
                        cmd.userdata = uto;

                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setHostUuid(struct.getHostUuid());
                        msg.setCommand(cmd);
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
                                    trigger.fail(operr("operation error, because:%s", rsp.getError()));
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
        chain.setName(String.format("tf-network-userdata-release-for-vm-%s", struct.getVmUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "release-user-data";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ReleaseUserdataCmd cmd = new ReleaseUserdataCmd();
                        cmd.hostUuid = struct.getHostUuid();
                        cmd.vmIp = CollectionUtils.find(struct.getVmNics(), new Function<String, VmNicInventory>() {
                            @Override
                            public String call(VmNicInventory arg) {
                                return arg.getL3NetworkUuid().equals(struct.getL3NetworkUuid()) ? arg.getIp() : null;
                            }
                        });
                        cmd.vmUuid = struct.getVmUuid();

                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setHostUuid(struct.getHostUuid());
                        msg.setCommand(cmd);
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
                                    trigger.fail(operr("operation error, because:%s", rsp.getError()));
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
