package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.service.userdata.UserdataBackend;
import org.zstack.network.service.userdata.UserdataGlobalProperty;
import org.zstack.network.service.userdata.UserdataStruct;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.TagUtils;
import org.zstack.utils.function.Function;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 10/13/2015.
 */
public class FlatUserdataBackend implements UserdataBackend {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    public static final String APPLY_USER_DATA = "/flatnetworkprovider/userdata/apply";
    public static final String RELEASE_USER_DATA = "/flatnetworkprovider/userdata/release";

    public static class MetadataTO {
        public String vmUuid;
    }

    public static class ApplyUserdataCmd extends KVMAgentCommands.AgentCommand {
        public MetadataTO metadata;
        public String userdata;
        public String vmIp;
        public String dhcpServerIp;
        public String bridgeName;
        public int port;
    }

    public static class ApplyUserdataRsp extends KVMAgentCommands.AgentResponse {

    }

    public static class ReleaseUserdataCmd extends KVMAgentCommands.AgentCommand {
        public String vmIp;
        public String bridgeName;
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
        chain.setName(String.format("flat-network-userdata-set-for-vm-%s", struct.getVmSpec().getVmInventory().getUuid()));
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
                        MetadataTO to = new MetadataTO();
                        to.vmUuid = struct.getVmSpec().getVmInventory().getUuid();

                        ApplyUserdataCmd cmd = new ApplyUserdataCmd();
                        cmd.metadata = to;
                        cmd.userdata = struct.getUserdata();
                        cmd.dhcpServerIp = dhcpServerIp;
                        cmd.vmIp = CollectionUtils.find(struct.getVmSpec().getDestNics(), new Function<String, VmNicInventory>() {
                            @Override
                            public String call(VmNicInventory arg) {
                                return arg.getL3NetworkUuid().equals(struct.getL3NetworkUuid()) ? arg.getIp() : null;
                            }
                        });
                        cmd.bridgeName = getBridgeNameFromL3NetworkUuid(struct.getL3NetworkUuid());
                        cmd.port = UserdataGlobalProperty.HOST_PORT;

                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setHostUuid(struct.getVmSpec().getDestHost().getUuid());
                        msg.setCommand(cmd);
                        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
                        msg.setPath(APPLY_USER_DATA);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, struct.getVmSpec().getDestHost().getUuid());
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
                                    trigger.fail(errf.stringToOperationError(rsp.getError()));
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

    @Transactional(readOnly = true)
    private String getBridgeNameFromL3NetworkUuid(String l3Uuid) {
        String sql = "select t.tag from SystemTagVO t, L3NetworkVO l3 where t.resourceType = :ttype and t.tag like :tag" +
                " and t.resourceUuid = l3.l2NetworkUuid and l3.uuid = :l3Uuid";
        TypedQuery<String> tq = dbf.getEntityManager().createQuery(sql, String.class);
        tq.setParameter("tag", TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.getTagFormat()));
        tq.setParameter("l3Uuid", l3Uuid);
        tq.setParameter("ttype", L2NetworkVO.class.getSimpleName());
        List<String> lst = tq.getResultList();
        if (lst.isEmpty()) {
            throw new CloudRuntimeException(String.format("cannot find the bridge name for l3 network[uuid:%s]", l3Uuid));
        }
        String tag = lst.get(0);
        return KVMSystemTags.L2_BRIDGE_NAME.getTokenByTag(tag, KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
    }

    @Override
    public void releaseUserdata(final UserdataStruct struct, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("flat-network-userdata-release-for-vm-%s", struct.getVmSpec().getVmInventory().getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "release-user-data";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ReleaseUserdataCmd cmd = new ReleaseUserdataCmd();
                        cmd.bridgeName = getBridgeNameFromL3NetworkUuid(struct.getL3NetworkUuid());
                        cmd.vmIp = CollectionUtils.find(struct.getVmSpec().getDestNics(), new Function<String, VmNicInventory>() {
                            @Override
                            public String call(VmNicInventory arg) {
                                return arg.getL3NetworkUuid().equals(struct.getL3NetworkUuid()) ? arg.getIp() : null;
                            }
                        });

                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setHostUuid(struct.getVmSpec().getDestHost().getUuid());
                        msg.setCommand(cmd);
                        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
                        msg.setPath(RELEASE_USER_DATA);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, struct.getVmSpec().getDestHost().getUuid());
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
                                    trigger.fail(errf.stringToOperationError(rsp.getError()));
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
