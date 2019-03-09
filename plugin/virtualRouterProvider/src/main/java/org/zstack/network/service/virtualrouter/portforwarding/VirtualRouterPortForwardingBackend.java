package org.zstack.network.service.virtualrouter.portforwarding;

import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.VirtualRouterAfterAttachNicExtensionPoint;
import org.zstack.header.network.service.VirtualRouterBeforeDetachNicExtensionPoint;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.portforwarding.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;

public class VirtualRouterPortForwardingBackend extends AbstractVirtualRouterBackend implements
        PortForwardingBackend, Component, VirtualRouterAfterAttachNicExtensionPoint, VirtualRouterBeforeDetachNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VirtualRouterPortForwardingBackend.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private NetworkServiceManager nwServiceMgr;

    public static final Set<VmInstanceState> SYNC_PF_VM_STATES = ImmutableSet.<VmInstanceState> of(
            VmInstanceState.Running
    );

    private List<String> applyPortForwardingRuleElements;
    private FlowChainBuilder applyRuleChainBuilder;
    private FlowChainBuilder releaseRuleChainBuilder;
    private List<String> releasePortForwardingRuleElements;

    private PortForwardingRuleTO makePortForwardingRuleTO(final PortForwardingStruct struct, VirtualRouterVmInventory vr) {
        String privateMac = CollectionUtils.find(vr.getVmNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                if (arg.getL3NetworkUuid().equals(struct.getGuestL3Network().getUuid())) {
                    return arg.getMac();
                }
                return null;
            }
        });

        DebugUtils.Assert(privateMac!=null, String.format("cannot find guest nic[l3NetworkUuid:%s] on virtual router[uuid:%s, name:%s]",
                struct.getGuestL3Network().getUuid(), vr.getUuid(), vr.getName()));

        PortForwardingRuleTO to = new PortForwardingRuleTO();
        to.setUuid(struct.getRule().getUuid());
        to.setAllowedCidr(struct.getRule().getAllowedCidr());
        to.setPrivateIp(struct.getGuestIp());
        to.setPrivateMac(privateMac);
        to.setPrivatePortEnd(struct.getRule().getPrivatePortEnd());
        to.setPrivatePortStart(struct.getRule().getPrivatePortStart());
        to.setVipIp(struct.getVip().getIp());
        to.setVipPortEnd(struct.getRule().getVipPortEnd());
        to.setSnatInboundTraffic(struct.isSnatInboundTraffic());
        to.setVipPortStart(struct.getRule().getVipPortStart());
        to.setProtocolType(struct.getRule().getProtocolType());
        return to;
    }

    @Transactional(readOnly = true)
    private VirtualRouterVmInventory findRunningVirtualRouterForRule(String ruleUuid) {
        String sql = "select vr from VirtualRouterPortForwardingRuleRefVO ref, VirtualRouterVmVO vr where ref.virtualRouterVmUuid = vr.uuid and ref.uuid = :ruleUuid and vr.state = :vrState";
        TypedQuery<VirtualRouterVmVO> q = dbf.getEntityManager().createQuery(sql, VirtualRouterVmVO.class);
        q.setParameter("ruleUuid", ruleUuid);
        q.setParameter("vrState", VmInstanceState.Running);
        q.setMaxResults(1);
        List<VirtualRouterVmVO> vrs = q.getResultList();
        if (vrs.isEmpty()) {
            return null;
        } else {
            return VirtualRouterVmInventory.valueOf(vrs.get(0));
        }
    }

    private void buildWorkFlowsBuilder() {
        try{
            applyRuleChainBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(applyPortForwardingRuleElements).construct();
            releaseRuleChainBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(releasePortForwardingRuleElements).construct();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    public void setApplyPortForwardingRuleElements(List<String> applyPortForwardingRuleElements) {
        this.applyPortForwardingRuleElements = applyPortForwardingRuleElements;
    }

    public void setReleasePortForwardingRuleElements(List<String> releasePortForwardingRuleElements) {
        this.releasePortForwardingRuleElements = releasePortForwardingRuleElements;
    }

    @Override
    public boolean start() {
        buildWorkFlowsBuilder();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public NetworkServiceProviderType getProviderType() {
        return VirtualRouterConstant.PROVIDER_TYPE;
    }

    private void applyRule(final Iterator<PortForwardingStruct> it, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final PortForwardingStruct struct = it.next();
        VirtualRouterStruct s = new VirtualRouterStruct();
        s.setL3Network(struct.getGuestL3Network());
        s.setOfferingValidator(new VirtualRouterOfferingValidator() {
            @Override
            public void validate(VirtualRouterOfferingInventory offering) throws OperationFailureException {
                if (!offering.getPublicNetworkUuid().equals(struct.getVip().getL3NetworkUuid())) {
                    throw new OperationFailureException(operr("found a virtual router offering[uuid:%s] for L3Network[uuid:%s] in zone[uuid:%s]; however, the network's public network[uuid:%s] is not the same to PortForwarding rule[uuid:%s]'s; you may need to use system tag" +
                                    " guestL3Network::l3NetworkUuid to specify a particular virtual router offering for the L3Network", offering.getUuid(), struct.getGuestL3Network().getUuid(), struct.getGuestL3Network().getZoneUuid(), struct.getVip().getL3NetworkUuid(), struct.getRule().getUuid()));
                }
            }
        });

        acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(completion) {
            @Override
            public void success(final VirtualRouterVmInventory vr) {
                applyRule(struct, vr, new Completion(completion) {
                    @Override
                    public void success() {
                        new VirtualRouterRoleManager().makePortForwardingRole(vr.getUuid());
                        applyRule(it, completion);
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

    private void applyRule(final PortForwardingStruct struct, final VirtualRouterVmInventory vr, final Completion completion) {
        final PortForwardingRuleTO to = makePortForwardingRuleTO(struct, vr);
        FlowChain chain = applyRuleChainBuilder.build();

        chain.setName(String.format("vr-apply-port-forwarding-rule-%s", struct.getRule().getUuid()));
        chain.getData().put(VirtualRouterConstant.VR_RESULT_VM, vr);
        chain.getData().put(VirtualRouterConstant.VR_PORT_FORWARDING_RULE, to);
        chain.getData().put(VirtualRouterConstant.VR_VIP, struct.getVip());
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                SimpleQuery<VirtualRouterPortForwardingRuleRefVO> q = dbf.createQuery(VirtualRouterPortForwardingRuleRefVO.class);
                q.add(VirtualRouterPortForwardingRuleRefVO_.uuid, Op.EQ, struct.getRule().getUuid());
                if (!q.isExists()) {
                    // if virtual router is stopped outside zstack (e.g. the host reboot)
                    // database will still have VirtualRouterPortForwardingRuleRefVO for this PF rule.
                    // in this case, don't create the record again
                    VirtualRouterPortForwardingRuleRefVO ref = new VirtualRouterPortForwardingRuleRefVO();
                    ref.setUuid(struct.getRule().getUuid());
                    ref.setVirtualRouterVmUuid(vr.getUuid());
                    ref.setVipUuid(struct.getVip().getUuid());
                    dbf.persist(ref);
                }
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void applyPortForwardingRule(PortForwardingStruct struct, Completion completion) {
        PortForwardingRuleInventory rule = struct.getRule();
        if ((rule.getVipPortStart() != rule.getPrivatePortStart() || rule.getVipPortEnd() != rule.getPrivatePortEnd()) && (rule.getVipPortStart() != rule.getVipPortEnd()) && (rule.getPrivatePortStart() != rule.getPrivatePortEnd())) {
            throw new OperationFailureException(operr("virtual router doesn't support port forwarding range redirection, the vipPortStart must be equals to privatePortStart and vipPortEnd must be equals to privatePortEnd;" +
                            "but this rule rule has a mismatching range: vip port[%s, %s], private port[%s, %s]", rule.getVipPortStart(), rule.getVipPortEnd(), rule.getPrivatePortStart(), rule.getPrivatePortEnd()));
        }

        applyRule(Arrays.asList(struct).iterator(), completion);
    }

    private void revokeRule(final PortForwardingStruct struct, final Completion completion) {
        VirtualRouterVmInventory vr = findRunningVirtualRouterForRule(struct.getRule().getUuid());
        if (vr == null) {
            // vr is either destroyed or not running
            // no need to do anything in each case, if vr is not running, rules will get synced once it gets running
            completion.success();
            return;
        }

        PortForwardingRuleTO to = makePortForwardingRuleTO(struct, vr);

        FlowChain chain = releaseRuleChainBuilder.build();
        chain.setName(String.format("vr-release-port-forwarding-rule-%s", struct.getRule().getUuid()));
        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put(VirtualRouterConstant.VR_RESULT_VM, vr);
        ctx.put(VirtualRouterConstant.VR_PORT_FORWARDING_RULE, to);
        ctx.put(VirtualRouterConstant.VR_VIP_L3NETWORK, struct.getVip().getL3NetworkUuid());

        chain.setData(ctx).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                dbf.removeByPrimaryKey(struct.getRule().getUuid(), VirtualRouterPortForwardingRuleRefVO.class);
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
    public void revokePortForwardingRule(PortForwardingStruct struct, Completion completion) {
        revokeRule(struct, completion);
    }

    private List<PortForwardingRuleTO> findPortforwardingsOnVmNic(VmNicInventory nic, VirtualRouterVmInventory vr) {
        List<Tuple> pfs = findPortForwardingTuplesOnVmNic(nic);

        if (pfs == null || pfs.isEmpty()) {
            return null;
        }

        List<VirtualRouterPortForwardingRuleRefVO> refs = new ArrayList<VirtualRouterPortForwardingRuleRefVO>();
        for (Tuple t : pfs) {
            PortForwardingRuleVO rule = t.get(0, PortForwardingRuleVO.class);
            if (!Q.New(VirtualRouterPortForwardingRuleRefVO.class)
                    .eq(VirtualRouterPortForwardingRuleRefVO_.uuid, rule.getUuid())
                    .eq(VirtualRouterPortForwardingRuleRefVO_.virtualRouterVmUuid, nic.getVmInstanceUuid())
                    .isExists()) {
                VirtualRouterPortForwardingRuleRefVO ref = new VirtualRouterPortForwardingRuleRefVO();
                ref.setVirtualRouterVmUuid(nic.getVmInstanceUuid());
                ref.setVipUuid(rule.getVipUuid());
                ref.setUuid(rule.getUuid());
                refs.add(ref);
            }
        }
        if (!refs.isEmpty()) {
            dbf.persistCollection(refs);
        }

        List<PortForwardingRuleTO> tos = new ArrayList<>();
        for (Tuple t : pfs) {
            PortForwardingRuleVO pf = t.get(0, PortForwardingRuleVO.class);
            PortForwardingRuleTO to = new PortForwardingRuleTO();
            to.setUuid(pf.getUuid());
            to.setAllowedCidr(pf.getAllowedCidr());
            to.setPrivateIp(t.get(1, String.class));
            to.setPrivateMac(
                    vr.getVmNics().stream()
                            .filter(n -> n.getL3NetworkUuid().equals(nic.getL3NetworkUuid()))
                            .findFirst().get().getMac());
            to.setPrivatePortStart(pf.getPrivatePortStart());
            to.setPrivatePortEnd(pf.getPrivatePortEnd());
            to.setProtocolType(pf.getProtocolType().toString());
            to.setSnatInboundTraffic(PortForwardingGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
            to.setVipIp(pf.getVipIp());
            to.setVipPortStart(pf.getVipPortStart());
            to.setVipPortEnd(pf.getVipPortEnd());
            tos.add(to);
        }

        return tos;
    }

    private List<Tuple> findPortForwardingTuplesOnVmNic(VmNicInventory nic) {
        return SQL.New("select pf, nic.ip, nic.mac " +
                    "from PortForwardingRuleVO pf, VmNicVO nic, VmInstanceVO vm " +
                    "where pf.vmNicUuid = nic.uuid " +
                    "and nic.vmInstanceUuid = vm.uuid " +
                    "and nic.l3NetworkUuid = :l3Uuid " +
                    "and vm.state in (:syncPfVmStates) " +
                    "and pf.state = :enabledState", Tuple.class)
                    .param("l3Uuid", nic.getL3NetworkUuid())
                    .param("syncPfVmStates", SYNC_PF_VM_STATES)
                    .param("enabledState", PortForwardingRuleState.Enabled)
                    .list();
    }

    @Override
    public void afterAttachNic(VmNicInventory nic, Completion completion) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            completion.success();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(nic.getVmInstanceUuid())) {
            completion.success();
            return;
        }

        try {
            nwServiceMgr.getTypeOfNetworkServiceProviderForService(nic.getL3NetworkUuid(), PortForwardingConstant.PORTFORWARDING_TYPE);
        } catch (OperationFailureException e) {
            completion.success();
            return;
        }

        VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find();
        DebugUtils.Assert(vrVO != null,
                String.format("can not find virtual router[uuid: %s] for nic[uuid: %s, ip: %s, l3NetworkUuid: %s]",
                        nic.getVmInstanceUuid(), nic.getUuid(), nic.getIp(), nic.getL3NetworkUuid()));
        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrVO);

        List<PortForwardingRuleTO> pfs = findPortforwardingsOnVmNic(nic, vr);
        if (pfs == null || pfs.isEmpty()) {
            completion.success();
            return;
        }

        VirtualRouterCommands.CreatePortForwardingRuleCmd cmd = new VirtualRouterCommands.CreatePortForwardingRuleCmd();
        cmd.setRules(pfs);
        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING);
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vrVO.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrVO.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VirtualRouterCommands.SyncEipRsp ret = re.toResponse(VirtualRouterCommands.SyncEipRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("failed to add portforwardings on virtual router[uuid:%s], %s",
                            vrVO.getUuid(), ret.getError());
                    completion.fail(err);
                } else {
                    String info = String.format("sync port forwardings on virtual router[uuid:%s] successfully",
                            vrVO.getUuid());
                    logger.debug(info);
                    completion.success();
                }
            }
        });
    }

    @Override
    public void afterAttachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic, Completion completion) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            completion.success();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(nic.getVmInstanceUuid())) {
            completion.success();
            return;
        }

        try {
            nwServiceMgr.getTypeOfNetworkServiceProviderForService(nic.getL3NetworkUuid(), PortForwardingConstant.PORTFORWARDING_TYPE);
        } catch (OperationFailureException e) {
            completion.success();
            return;
        }

        VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find();
        DebugUtils.Assert(vrVO != null,
                String.format("can not find virtual router[uuid: %s] for nic[uuid: %s, ip: %s, l3NetworkUuid: %s]",
                        nic.getVmInstanceUuid(), nic.getUuid(), nic.getIp(), nic.getL3NetworkUuid()));
        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrVO);

        List<PortForwardingRuleTO> pfs = findPortforwardingsOnVmNic(nic, vr);
        if (pfs == null || pfs.isEmpty()) {
            completion.success();
            return;
        }

        VirtualRouterCommands.RevokePortForwardingRuleCmd cmd = new VirtualRouterCommands.RevokePortForwardingRuleCmd();
        cmd.setRules(pfs);
        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_REVOKE_PORT_FORWARDING);
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vrVO.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrVO.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VirtualRouterCommands.RevokePortForwardingRuleRsp ret = re.toResponse(VirtualRouterCommands.RevokePortForwardingRuleRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("failed to revoke port forwardings on virtual router[uuid:%s], %s",
                            vrVO.getUuid(), ret.getError());
                    completion.fail(err);
                } else {
                    List<Tuple> pfs = findPortForwardingTuplesOnVmNic(nic);
                    for (Tuple t : pfs) {
                        PortForwardingRuleVO rule = t.get(0, PortForwardingRuleVO.class);
                        new SQLBatch(){
                            @Override
                            protected void scripts() {
                                sql(VirtualRouterPortForwardingRuleRefVO.class).eq(VirtualRouterPortForwardingRuleRefVO_.uuid, rule.getUuid()).delete();
                                sql(PortForwardingRuleVO.class).eq(PortForwardingRuleVO_.uuid, rule.getUuid())
                                        .set(PortForwardingRuleVO_.guestIp, null).set(PortForwardingRuleVO_.vmNicUuid, null).update();
                            }
                        }.execute();
                    }
                    String info = String.format("sync port forwardings on virtual router[uuid:%s] successfully",
                            vrVO.getUuid());
                    logger.debug(info);
                    completion.success();
                }
            }
        });
    }

    @Override
    public void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }
}
