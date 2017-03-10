package org.zstack.network.service.virtualrouter.portforwarding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
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
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.identity.AccountManager;
import org.zstack.network.service.portforwarding.PortForwardingBackend;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.portforwarding.PortForwardingStruct;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.function.Function;

import static org.zstack.core.Platform.operr;

import javax.persistence.TypedQuery;
import java.util.*;

public class VirtualRouterPortForwardingBackend extends AbstractVirtualRouterBackend implements PortForwardingBackend, Component {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected AccountManager acntMgr;

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
}
