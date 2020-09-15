package org.zstack.network.service.flat;

import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.network.IPv6Constants;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class DhcpApply {
    CloudBus bus;

    void apply(Map.Entry<String, List<FlatDhcpBackend.DhcpInfo>> e, String hostUuid, boolean rebuild, Completion completion) {
        if (e == null) {
            completion.success();
            return;
        }

        final String l3Uuid = e.getKey();
        final List<FlatDhcpBackend.DhcpInfo> info = e.getValue();
        final List<FlatDhcpBackend.DhcpInfo> info4 = info.stream().filter(i -> i.ipVersion == IPv6Constants.IPv4).collect(Collectors.toList());
        final List<FlatDhcpBackend.DhcpInfo> info6 = info.stream().filter(i -> i.ipVersion == IPv6Constants.IPv6).collect(Collectors.toList());
        DebugUtils.Assert(!info.isEmpty(), "how can info be empty???");

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("flat-dhcp-provider-apply-dhcp-to-l3-network-%s", l3Uuid));
        chain.then(new ShareFlow() {
            FlatDhcpAcquireDhcpServerIpReply.DhcpServerIpStruct dhcp4Server = null;
            FlatDhcpAcquireDhcpServerIpReply.DhcpServerIpStruct dhcp6Server = null;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "get-dhcp-server-ip";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (info.isEmpty()) {
                            trigger.next();
                            return;
                        }
                        FlatDhcpAcquireDhcpServerIpMsg msg = new FlatDhcpAcquireDhcpServerIpMsg();
                        msg.setL3NetworkUuid(l3Uuid);
                        bus.makeTargetServiceIdByResourceUuid(msg, FlatNetworkServiceConstant.SERVICE_ID, l3Uuid);
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }
                                FlatDhcpAcquireDhcpServerIpReply r = reply.castReply();
                                List<FlatDhcpAcquireDhcpServerIpReply.DhcpServerIpStruct> dhcpServerIps = r.getDhcpServerList();
                                if (dhcpServerIps == null || dhcpServerIps.isEmpty()) {
                                    trigger.fail(operr("could not get dhcp server ip for l3 network [uuid:%s]", msg.getL3NetworkUuid()));
                                    return;
                                }

                                for (FlatDhcpAcquireDhcpServerIpReply.DhcpServerIpStruct struct : dhcpServerIps) {
                                    if (struct.getIpVersion() == IPv6Constants.IPv4) {
                                        dhcp4Server = struct;
                                    } else if (struct.getIpVersion() == IPv6Constants.IPv6) {
                                        dhcp6Server = struct;
                                    }
                                }
                                if (!info4.isEmpty() && dhcp4Server == null) {
                                    trigger.fail(operr("could not get dhcp4 server ip for l3 network [uuid:%s]", msg.getL3NetworkUuid()));
                                    return;
                                }
                                if (!info6.isEmpty() && dhcp6Server == null) {
                                    trigger.fail(operr("could not get dhcp6 server ip for l3 network [uuid:%s]", msg.getL3NetworkUuid()));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "prepare-distributed-dhcp-server-on-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        FlatDhcpBackend.DhcpInfo i = info.get(0);

                        FlatDhcpBackend.PrepareDhcpCmd cmd = new FlatDhcpBackend.PrepareDhcpCmd();
                        cmd.bridgeName = i.bridgeName;
                        cmd.namespaceName = i.namespaceName;
                        if (dhcp4Server != null) {
                            cmd.dhcpServerIp = dhcp4Server.getIp();
                            cmd.dhcpNetmask = dhcp4Server.getNetmask();
                        }
                        if (dhcp6Server != null) {
                            cmd.dhcp6ServerIp = dhcp6Server.getIp();
                            cmd.prefixLen = dhcp6Server.getIpr().getPrefixLen();
                            cmd.addressMode = dhcp6Server.getIpr().getAddressMode();
                        }
                        if (dhcp4Server != null && dhcp6Server == null) {
                            cmd.ipVersion = IPv6Constants.IPv4;
                        } else if (dhcp4Server == null && dhcp6Server != null) {
                            cmd.ipVersion = IPv6Constants.IPv6;
                        } else {
                            cmd.ipVersion = IPv6Constants.DUAL_STACK;
                        }

                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setHostUuid(hostUuid);
                        msg.setNoStatusCheck(true);
                        msg.setCommand(cmd);
                        msg.setPath(FlatDhcpBackend.PREPARE_DHCP_PATH);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                KVMHostAsyncHttpCallReply ar = reply.castReply();
                                FlatDhcpBackend.PrepareDhcpRsp rsp = ar.toResponse(FlatDhcpBackend.PrepareDhcpRsp.class);
                                if (!rsp.isSuccess()) {
                                    trigger.fail(operr("operation error, because:%s", rsp.getError()));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "apply-dhcp";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        FlatDhcpBackend.ApplyDhcpCmd cmd = new FlatDhcpBackend.ApplyDhcpCmd();
                        cmd.dhcp = info;
                        cmd.rebuild = rebuild;
                        cmd.l3NetworkUuid = l3Uuid;

                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setCommand(cmd);
                        msg.setHostUuid(hostUuid);
                        msg.setPath(FlatDhcpBackend.APPLY_DHCP_PATH);
                        msg.setNoStatusCheck(true);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                KVMHostAsyncHttpCallReply r = reply.castReply();
                                FlatDhcpBackend.ApplyDhcpRsp rsp = r.toResponse(FlatDhcpBackend.ApplyDhcpRsp.class);
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
