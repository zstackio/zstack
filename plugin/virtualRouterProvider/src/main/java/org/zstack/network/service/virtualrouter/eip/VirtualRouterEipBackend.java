package org.zstack.network.service.virtualrouter.eip;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmFacade;
import org.zstack.appliancevm.ApplianceVmFirewallProtocol;
import org.zstack.appliancevm.ApplianceVmFirewallRuleInventory;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.eip.EipBackend;
import org.zstack.network.service.eip.EipStruct;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.CreateEipRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.RemoveEipRsp;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 */
public class VirtualRouterEipBackend extends AbstractVirtualRouterBackend implements EipBackend {
    private static final CLogger logger = Utils.getLogger(VirtualRouterEipBackend.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private ApplianceVmFacade asf;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;

    private List<ApplianceVmFirewallRuleInventory> getFirewallRules(EipStruct struct) {
        ApplianceVmFirewallRuleInventory tcp = new ApplianceVmFirewallRuleInventory();
        tcp.setProtocol(ApplianceVmFirewallProtocol.tcp.toString());
        tcp.setDestIp(struct.getVip().getIp());
        tcp.setStartPort(0);
        tcp.setEndPort(65535);

        ApplianceVmFirewallRuleInventory udp = new ApplianceVmFirewallRuleInventory();
        udp.setProtocol(ApplianceVmFirewallProtocol.udp.toString());
        udp.setDestIp(struct.getVip().getIp());
        udp.setStartPort(0);
        udp.setEndPort(65535);

        return Arrays.asList(tcp, udp);
    }

    private void applyEip(final VirtualRouterVmInventory vr, final EipStruct struct, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("apply-eip-%s-vr-%s", struct.getEip().getUuid(), vr.getUuid()));
        chain.then(new Flow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                asf.openFirewall(vr.getUuid(), struct.getVip().getL3NetworkUuid(), getFirewallRules(struct), new Completion(trigger) {
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

            @Override
            public void rollback(final FlowRollback trigger, Map data) {
                asf.removeFirewall(vr.getUuid(), struct.getVip().getL3NetworkUuid(), getFirewallRules(struct), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to remove firewall rules on virtual router[uuid:%s, l3Network uuid:%s], %s",
                                vr.getUuid(), struct.getVip().getL3NetworkUuid(), errorCode));
                        trigger.rollback();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                EipTO to = new EipTO();
                String priMac = CollectionUtils.find(vr.getVmNics(), new Function<String, VmNicInventory>() {
                    @Override
                    public String call(VmNicInventory arg) {
                        if (arg.getL3NetworkUuid().equals(struct.getNic().getL3NetworkUuid())) {
                            return arg.getMac();
                        }
                        return null;
                    }
                });
                to.setPrivateMac(priMac);
                to.setPublicMac(vr.getVmNics().stream().filter(
                        nic -> nic.getL3NetworkUuid().equals(vr.getPublicNetworkUuid())).findFirst().get().getMac());
                to.setVipIp(struct.getVip().getIp());
                to.setGuestIp(struct.getNic().getIp());
                to.setSnatInboundTraffic(struct.isSnatInboundTraffic());

                VirtualRouterCommands.CreateEipCmd cmd = new VirtualRouterCommands.CreateEipCmd();
                cmd.setEip(to);
                VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                msg.setCheckStatus(true);
                msg.setPath(VirtualRouterConstant.VR_CREATE_EIP);
                msg.setCommand(cmd);
                msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
                msg.setVmInstanceUuid(vr.getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        VirtualRouterAsyncHttpCallReply re = reply.castReply();
                        CreateEipRsp ret = re.toResponse(CreateEipRsp.class);
                        if (ret.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(operr("failed to create eip[uuid:%s, name:%s, ip:%s] for vm nic[uuid:%s] on virtual router[uuid:%s], %s",
                                            struct.getEip().getUuid(), struct.getEip().getName(), struct.getVip().getIp(), struct.getNic().getUuid(),
                                            vr.getUuid(), ret.getError()));
                        }
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                String info = String.format("successfully created eip[uuid:%s, name:%s, ip:%s] for vm nic[uuid:%s] on virtual router[uuid:%s]",
                        struct.getEip().getUuid(), struct.getEip().getName(), struct.getVip().getIp(), struct.getNic().getUuid(),
                        vr.getUuid());
                new VirtualRouterRoleManager().makeEipRole(vr.getUuid());
                logger.debug(info);
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
    public void applyEip(final EipStruct struct, final Completion completion) {
        L3NetworkVO l3vo = dbf.findByUuid(struct.getNic().getL3NetworkUuid(), L3NetworkVO.class);
        final L3NetworkInventory l3inv = L3NetworkInventory.valueOf(l3vo);

        VirtualRouterStruct s = new VirtualRouterStruct();
        s.setOfferingValidator(new VirtualRouterOfferingValidator() {
            @Override
            public void validate(VirtualRouterOfferingInventory offering) throws OperationFailureException {
                if (!offering.getPublicNetworkUuid().equals(struct.getVip().getL3NetworkUuid())) {
                    throw new OperationFailureException(operr("found a virtual router offering[uuid:%s] for L3Network[uuid:%s] in zone[uuid:%s]; however, the network's public network[uuid:%s] is not the same to EIP[uuid:%s]'s; you may need to use system tag" +
                                    " guestL3Network::l3NetworkUuid to specify a particular virtual router offering for the L3Network", offering.getUuid(), l3inv.getUuid(), l3inv.getZoneUuid(), struct.getVip().getL3NetworkUuid(), struct.getEip().getUuid()));
                }
            }
        });
        s.setL3Network(l3inv);

        acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(completion) {
            @Override
            public void success(final VirtualRouterVmInventory vr) {
                applyEip(vr, struct, new Completion(completion) {
                    @Override
                    public void success() {
                        SimpleQuery<VirtualRouterEipRefVO> q = dbf.createQuery(VirtualRouterEipRefVO.class);
                        q.add(VirtualRouterEipRefVO_.eipUuid, Op.EQ, struct.getEip().getUuid());
                        if (!q.isExists()) {
                            // if virtual router is stopped outside zstack (e.g. the host rebbot)
                            // database will still have VirtualRouterEipRefVO for this EIP.
                            // in this case, don't create the record again
                            VirtualRouterEipRefVO ref = new VirtualRouterEipRefVO();
                            ref.setEipUuid(struct.getEip().getUuid());
                            ref.setVirtualRouterVmUuid(vr.getUuid());
                            dbf.persist(ref);
                        }
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void revokeEip(final EipStruct struct, final Completion completion) {
        SimpleQuery<VirtualRouterEipRefVO> q = dbf.createQuery(VirtualRouterEipRefVO.class);
        q.add(VirtualRouterEipRefVO_.eipUuid, SimpleQuery.Op.EQ, struct.getEip().getUuid());
        final VirtualRouterEipRefVO ref = q.find();
        if (ref == null) {
            // vr may have been deleted
            completion.success();
            return;
        }

        VirtualRouterVmVO vrvo = dbf.findByUuid(ref.getVirtualRouterVmUuid(), VirtualRouterVmVO.class);
        if (vrvo.getState() != VmInstanceState.Running) {
            // rule will be synced when vr state changes to Running
            dbf.remove(ref);
            completion.success();
            return;
        }

        final VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrvo);

        //TODO: add GC
        final FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("revoke-eip-%s-vr-%s", struct.getEip().getUuid(), vr.getUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                VirtualRouterCommands.RemoveEipCmd cmd = new VirtualRouterCommands.RemoveEipCmd();
                EipTO to = new EipTO();
                String priMac = CollectionUtils.find(vr.getVmNics(), new Function<String, VmNicInventory>() {
                    @Override
                    public String call(VmNicInventory arg) {
                        if (arg.getL3NetworkUuid().equals(struct.getNic().getL3NetworkUuid())) {
                            return arg.getMac();
                        }
                        return null;
                    }
                });

                to.setPrivateMac(priMac);
                to.setSnatInboundTraffic(struct.isSnatInboundTraffic());
                to.setVipIp(struct.getVip().getIp());
                to.setGuestIp(struct.getNic().getIp());
                cmd.setEip(to);

                VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                msg.setVmInstanceUuid(vr.getUuid());
                msg.setCommand(cmd);
                msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
                msg.setCheckStatus(true);
                msg.setPath(VirtualRouterConstant.VR_REMOVE_EIP);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.setError(reply.getError());
                        } else {
                            VirtualRouterAsyncHttpCallReply re = reply.castReply();
                            RemoveEipRsp ret = re.toResponse(RemoveEipRsp.class);
                            if (!ret.isSuccess()) {
                                ErrorCode err = operr("failed to remove eip[uuid:%s, name:%s, ip:%s] for vm nic[uuid:%s] on virtual router[uuid:%s], %s",
                                        struct.getEip().getUuid(), struct.getEip().getName(), struct.getVip().getIp(), struct.getNic().getUuid(),
                                        vr.getUuid(), ret.getError());
                                trigger.setError(err);
                            }
                        }

                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                asf.removeFirewall(vr.getUuid(), struct.getVip().getL3NetworkUuid(), getFirewallRules(struct), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to remove firewall rules on virtual router[uuid:%s, l3Network uuid:%s], %s",
                                vr.getUuid(), struct.getVip().getL3NetworkUuid(), errorCode));
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                String info = String.format("successfully removed eip[uuid:%s, name:%s, ip:%s] for vm nic[uuid:%s] on virtual router[uuid:%s]",
                        struct.getEip().getUuid(), struct.getEip().getName(), struct.getVip().getIp(), struct.getNic().getUuid(),
                        vr.getUuid());
                logger.debug(info);
                dbf.remove(ref);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                // We need to remove the 'ref' record, otherwise the next time when
                // deleting EIP is requested, we will get ConstraintViolationException.
                dbf.remove(ref);
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public String getNetworkServiceProviderType() {
        return VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE;
    }
}
