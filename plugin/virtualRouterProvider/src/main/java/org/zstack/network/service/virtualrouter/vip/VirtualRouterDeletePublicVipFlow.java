package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterManager;
import org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterDeletePublicVipFlow extends NoRollbackFlow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected VirtualRouterManager vrMgr;
    @Autowired
    protected VipConfigProxy vipConfigProxy;

    private final static CLogger logger = Utils.getLogger(VirtualRouterDeletePublicVipFlow.class);

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        VmNicInventory nic = (VmNicInventory) data.get(VirtualRouterConstant.Param.VR_NIC.toString());

        if (!VirtualRouterNicMetaData.isPublicNic(nic) && !VirtualRouterNicMetaData.isAddinitionalPublicNic(nic)) {
            chain.next();
            return;
        }

        String vipIp = nic.getIp();
        if (nic.getL3NetworkUuid().equals(vr.getDefaultRouteL3NetworkUuid())) {
            VmNicInventory publicNic = vrMgr.getSnatPubicInventory(vr);
            vipIp = publicNic.getIp();
        }

        VipVO vipVO = Q.New(VipVO.class).eq(VipVO_.ip, vipIp).eq(VipVO_.l3NetworkUuid, nic.getL3NetworkUuid()).find();
        if (vipVO == null) {
            logger.debug(String.format("there is no vip for IP address %s", vipIp));
            chain.next();
            return;
        }

        /* when there is no service, vip can not be detached from virtual router */
        if (vipVO.getServicesTypes() == null || vipVO.getServicesTypes().isEmpty()) {
            vipConfigProxy.detachNetworkService(vr.getUuid(), VipVO.class.getSimpleName(), Collections.singletonList(vipVO.getUuid()));
        }

        VipDeletionMsg dmsg = new VipDeletionMsg();
        dmsg.setVipUuid(vipVO.getUuid());
        bus.makeTargetServiceIdByResourceUuid(dmsg, VipConstant.SERVICE_ID, dmsg.getVipUuid());
        bus.send(dmsg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                vipConfigProxy.detachNetworkService(vr.getUuid(), VipVO.class.getSimpleName(), Collections.singletonList(vipVO.getUuid()));
                chain.next();
            }
        });
    }
}
