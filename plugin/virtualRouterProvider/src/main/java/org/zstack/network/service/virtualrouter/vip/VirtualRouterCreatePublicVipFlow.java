package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.identity.Account;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterManager;
import org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterCreatePublicVipFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected VipConfigProxy vipConfigProxy;
    @Autowired
    protected VirtualRouterManager vrMgr;

    private final static CLogger logger = Utils.getLogger(VirtualRouterCreatePublicVipFlow.class);

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        VmNicInventory nic = (VmNicInventory) data.get(VirtualRouterConstant.Param.VR_NIC.toString());
        Boolean snat = (Boolean) data.get(VirtualRouterConstant.Param.SNAT.toString());
        boolean isNewCreated = data.containsKey(VirtualRouterConstant.Param.IS_NEW_CREATED.toString());

        /* this flow will be called when:
        *  1. virtual router post create flows, including ha virtual router, non-ha virtual router
        *  2. after attach nic flow, including ha virtual router, non-ha virtual router */
        if (isNewCreated && vr.isHaEnabled()) {
            /* ha public vip is created in ha code */
            chain.next();
            return;
        }

        if (!VirtualRouterNicMetaData.isPublicNic(nic) && !VirtualRouterNicMetaData.isAddinitionalPublicNic(nic)) {
            logger.debug(String.format("nic is not in public network [uuid:%s]", nic.getL3NetworkUuid()));
            chain.next();
            return;
        }

        String vipIp = nic.getIp();
        if (nic.getL3NetworkUuid().equals(vr.getManagementNetworkUuid()) && vr.isHaEnabled()) {
            VmNicInventory publicNic = vrMgr.getSnatPubicInventory(vr);
            vipIp = publicNic.getIp();
        }

        VipVO vipVO = Q.New(VipVO.class).eq(VipVO_.ip, vipIp).eq(VipVO_.l3NetworkUuid, nic.getL3NetworkUuid()).find();
        if (vipVO != null) {
            logger.debug(String.format("vip [ip:%s] in l3 network [uuid:%s] already created", vipIp, nic.getL3NetworkUuid()));
            chain.next();
            return;
        }

        CreateVipMsg cmsg = new CreateVipMsg();
        cmsg.setName(String.format("vip-for-%s", vr.getName()));
        cmsg.setL3NetworkUuid(nic.getL3NetworkUuid());
        cmsg.setRequiredIp(vipIp);
        cmsg.setSystem(true);
        String accountUuid = Account.getAccountUuidOfResource(vr.getUuid());
        SessionInventory session = new SessionInventory();
        session.setAccountUuid(accountUuid);
        cmsg.setSession(session);
        bus.makeTargetServiceIdByResourceUuid(cmsg, VipConstant.SERVICE_ID, nic.getUsedIpUuid());
        bus.send(cmsg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    chain.fail(reply.getError());
                    return;
                }

                CreateVipReply r = reply.castReply();
                VipInventory vipInventory = r.getVip();
                /* use for rollback */
                data.put(VirtualRouterConstant.Param.PUB_VIP_UUID.toString(), vipInventory.getUuid());
                vipConfigProxy.attachNetworkService(vr.getUuid(), VipVO.class.getSimpleName(), Arrays.asList(vipInventory.getUuid()));

                if (snat != null && !snat) {
                    logger.debug(String.format("SNAT is not applyied to vip [ip:%s, name:%s]",
                            vipInventory.getIp(), vipInventory.getName()));
                    chain.next();
                    return;
                }

                /* only default route network nic ip will apply snat */
                if (!vipInventory.getL3NetworkUuid().equals(vr.getDefaultRouteL3NetworkUuid())) {
                    chain.next();
                    return;
                }

                /* if there is no guest nic, don't apply snat */
                boolean hasGuestNic = false;
                for (VmNicInventory nic : vr.getVmNics()) {
                    if (VirtualRouterNicMetaData.isGuestNic(nic)) {
                        hasGuestNic = true;
                    }
                }
                if (!hasGuestNic) {
                    chain.next();
                    return;
                }

                ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                struct.setUseFor(VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE);
                struct.setServiceUuid(vipInventory.getUuid());
                Vip vip = new Vip(vipInventory.getUuid());
                vip.setStruct(struct);
                vip.acquire(new Completion(chain) {
                    @Override
                    public void success() {
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.fail(errorCode);
                    }
                });
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        String vipUuid = (String) data.get(VirtualRouterConstant.Param.PUB_VIP_UUID.toString());
        if (vipUuid == null) {
            chain.rollback();
            return;
        }

        VipDeletionMsg dmsg = new VipDeletionMsg();
        dmsg.setVipUuid(vipUuid);
        bus.makeTargetServiceIdByResourceUuid(dmsg, VipConstant.SERVICE_ID, vipUuid);
        bus.send(dmsg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                chain.rollback();
            }
        });
    }
}
