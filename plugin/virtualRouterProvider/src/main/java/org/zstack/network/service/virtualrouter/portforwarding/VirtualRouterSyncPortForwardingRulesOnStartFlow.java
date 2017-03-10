package org.zstack.network.service.virtualrouter.portforwarding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.portforwarding.PortForwardingConstant;
import org.zstack.network.service.portforwarding.PortForwardingGlobalConfig;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.SyncPortForwardingRuleCmd;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.SyncPortForwardingRuleRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterSyncPortForwardingRulesOnStartFlow implements Flow {
    private static CLogger logger = Utils.getLogger(VirtualRouterSyncPortForwardingRulesOnStartFlow.class);
    
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;

    @Transactional
    private List<PortForwardingRuleVO> findRulesForThisRouter(VirtualRouterVmInventory vr, Map<String, Object> data, boolean isNewCreated) {
        if (!isNewCreated) {
            String sql = "select rule from PortForwardingRuleVO rule, VirtualRouterPortForwardingRuleRefVO ref, VmNicVO nic, VmInstanceVO vm where vm.state = :vmState and nic.vmInstanceUuid = vm.uuid and rule.vmNicUuid = nic.uuid and rule.uuid = ref.uuid and ref.virtualRouterVmUuid = :vrUuid";
            TypedQuery<PortForwardingRuleVO> q = dbf.getEntityManager().createQuery(sql, PortForwardingRuleVO.class);
            q.setParameter("vrUuid", vr.getUuid());
            q.setParameter("vmState", VmInstanceState.Running);
            return q.getResultList();
        } else {
            VmNicInventory publicNic = vr.getPublicNic();
            VmNicInventory guestNic = vr.getGuestNic();
            String sql = "select rule from PortForwardingRuleVO rule, VipVO vip, VmNicVO nic, VmInstanceVO vm where vm.uuid = nic.vmInstanceUuid and vm.state = :vmState and rule.vipUuid = vip.uuid and rule.vmNicUuid = nic.uuid and vip.l3NetworkUuid = :vipL3Uuid and nic.l3NetworkUuid = :guestL3Uuid";
            TypedQuery<PortForwardingRuleVO> q = dbf.getEntityManager().createQuery(sql, PortForwardingRuleVO.class);
            q.setParameter("vipL3Uuid", publicNic.getL3NetworkUuid());
            q.setParameter("guestL3Uuid", guestNic.getL3NetworkUuid());
            q.setParameter("vmState", VmInstanceState.Running);

            List<PortForwardingRuleVO> rules =  q.getResultList();

            if (!rules.isEmpty()) {
                List<VirtualRouterPortForwardingRuleRefVO> refs = new ArrayList<VirtualRouterPortForwardingRuleRefVO>();
                for (PortForwardingRuleVO rule : rules) {
                    VirtualRouterPortForwardingRuleRefVO ref = new VirtualRouterPortForwardingRuleRefVO();
                    ref.setVirtualRouterVmUuid(vr.getUuid());
                    ref.setVipUuid(rule.getVipUuid());
                    ref.setUuid(rule.getUuid());
                    dbf.getEntityManager().persist(ref);
                    refs.add(ref);
                }

                data.put(VirtualRouterSyncPortForwardingRulesOnStartFlow.class.getName(), refs);
            }

            return rules;
        }
    }
    
    @Transactional(readOnly=true)
    private Collection<PortForwardingRuleTO> calculateAllRules(Map<String, PortForwardingRuleVO> ruleMap, String vrUuid) {
        String sql = "select rule.uuid, nic.ip, vip.ip from PortForwardingRuleVO rule, VmNicVO nic, VipVO vip where rule.vmNicUuid = nic.uuid and rule.uuid in (:ruleUuids) and vip.uuid = rule.vipUuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("ruleUuids", ruleMap.keySet());
        List<Tuple> privateIps = q.getResultList();
        
        Map<String, PortForwardingRuleTO> tos = new HashMap<String, PortForwardingRuleTO>();
        for (Tuple t : privateIps) {
            String ruleUuid = t.get(0, String.class);
            PortForwardingRuleTO to = new PortForwardingRuleTO();
            to.setPrivateIp(t.get(1, String.class));
            
            PortForwardingRuleVO ruleVO = ruleMap.get(ruleUuid);
            to.setAllowedCidr(ruleVO.getAllowedCidr());
            to.setPrivatePortEnd(ruleVO.getPrivatePortEnd());
            to.setPrivatePortStart(ruleVO.getPrivatePortStart());
            to.setVipPortEnd(ruleVO.getVipPortEnd());
            to.setSnatInboundTraffic(PortForwardingGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
            to.setVipPortStart(ruleVO.getVipPortStart());
            to.setVipIp(t.get(2, String.class));
            to.setProtocolType(ruleVO.getProtocolType().toString());
            tos.put(ruleUuid, to);
        }
        
        assert tos.size() == ruleMap.size();
        
        sql = "select rule.uuid, vrnic.mac from PortForwardingRuleVO rule, VmNicVO vrnic, VmNicVO nic2, ApplianceVmVO vr where vr.uuid = vrnic.vmInstanceUuid and vrnic.l3NetworkUuid = nic2.l3NetworkUuid and nic2.uuid = rule.vmNicUuid and rule.uuid in (:ruleUuids) and vr.uuid = :vrUuid";
        TypedQuery<Tuple> privateMacQuery = dbf.getEntityManager().createQuery(sql, Tuple.class);
        privateMacQuery.setParameter("ruleUuids", ruleMap.keySet());
        privateMacQuery.setParameter("vrUuid", vrUuid);
        List<Tuple> privateMacs = privateMacQuery.getResultList();
        for (Tuple t: privateMacs) {
            String ruleUuid = t.get(0, String.class);
            PortForwardingRuleTO to = tos.get(ruleUuid);
            to.setPrivateMac(t.get(1, String.class));
        }
        
        return tos.values();
    }
    
    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        VmNicInventory guestNic = vr.getGuestNic();
        if (!vrMgr.isL3NetworkNeedingNetworkServiceByVirtualRouter(guestNic.getL3NetworkUuid(), PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE)) {
            chain.next();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(vr.getUuid()) && !VirtualRouterSystemTags.VR_PORT_FORWARDING_ROLE.hasTag(vr.getUuid())) {
            chain.next();
            return;
        }

        new VirtualRouterRoleManager().makePortForwardingRole(vr.getUuid());

        boolean isNewCreated = data.containsKey(Param.IS_NEW_CREATED.toString());

        List<PortForwardingRuleVO> ruleVOs = findRulesForThisRouter(vr, data, isNewCreated);
        if (ruleVOs.isEmpty()) {
            chain.next();
            return;
        }

        Map<String, PortForwardingRuleVO> ruleMap = new HashMap<String, PortForwardingRuleVO>(ruleVOs.size());
        for (PortForwardingRuleVO rvo : ruleVOs) {
            ruleMap.put(rvo.getUuid(), rvo);
        }

        Collection<PortForwardingRuleTO> tos = calculateAllRules(ruleMap, vr.getUuid());
        List<PortForwardingRuleTO> toList = new ArrayList<PortForwardingRuleTO>(tos.size());
        toList.addAll(tos);

        SyncPortForwardingRuleCmd cmd = new SyncPortForwardingRuleCmd();
        cmd.setRules(toList);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
        msg.setPath(VirtualRouterConstant.VR_SYNC_PORT_FORWARDING);
        msg.setVmInstanceUuid(vr.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    chain.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                SyncPortForwardingRuleRsp ret = re.toResponse(SyncPortForwardingRuleRsp.class);
                if (ret.isSuccess()) {
                    String info = String.format("successfully sync port forwarding rules served by virtual router[name: %s uuid: %s]",
                            vr.getName(), vr.getUuid());
                    logger.debug(info);
                    chain.next();
                } else {
                    ErrorCode err = operr("failed to sync port forwarding rules served by virtual router[name: %s, uuid: %s], because %s",
                            vr.getName(), vr.getUuid(), ret.getError());
                    chain.fail(err);
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        List<VirtualRouterPortForwardingRuleRefVO> refs = (List<VirtualRouterPortForwardingRuleRefVO>) data.get(VirtualRouterSyncPortForwardingRulesOnStartFlow.class.getName());
        if (refs != null) {
            dbf.removeCollection(refs, VirtualRouterPortForwardingRuleRefVO.class);
        }

        chain.rollback();
    }
}
