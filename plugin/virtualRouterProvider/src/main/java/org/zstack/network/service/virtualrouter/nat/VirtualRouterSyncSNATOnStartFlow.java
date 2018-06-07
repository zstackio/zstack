package org.zstack.network.service.virtualrouter.nat;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.portforwarding.PortForwardingConstant;
import org.zstack.network.service.vip.ModifyVipAttributesStruct;
import org.zstack.network.service.vip.Vip;
import org.zstack.network.service.vip.VipConstant;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.SNATInfo;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.SyncSNATRsp;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterSyncSNATOnStartFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VirtualRouterSyncSNATOnStartFlow.class);

	@Autowired
	private CloudBus bus;
	@Autowired
	private VirtualRouterManager vrMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private NetworkServiceManager nwServiceMgr;

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        List<String> nwServed = vr.getAllL3Networks();
        nwServed = vrMgr.selectL3NetworksNeedingSpecificNetworkService(nwServed, NetworkServiceType.SNAT);
        if (nwServed.isEmpty()) {
            chain.next();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(vr.getUuid()) && !VirtualRouterSystemTags.VR_SNAT_ROLE.hasTag(vr.getUuid())) {
            chain.next();
            return;
        }

        new VirtualRouterRoleManager().makeSnatRole(vr.getUuid());

        final List<SNATInfo> snatInfo = new ArrayList<SNATInfo>();
        for (VmNicInventory nic : vr.getVmNics()) {
            if (nwServed.contains(nic.getL3NetworkUuid())) {
                SNATInfo info = new SNATInfo();
                info.setPrivateNicIp(nic.getIp());
                info.setPrivateNicMac(nic.getMac());
                info.setPublicIp(vr.getPublicNic().getIp());
                info.setPublicNicMac(vr.getPublicNic().getMac());
                info.setSnatNetmask(nic.getNetmask());
                snatInfo.add(info);
            }
        }

        VirtualRouterCommands.SyncSNATCmd cmd = new VirtualRouterCommands.SyncSNATCmd();
        cmd.setSnats(snatInfo);
        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_SYNC_SNAT_PATH);
        msg.setCommand(cmd);
        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
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
                SyncSNATRsp ret = re.toResponse(SyncSNATRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("virtual router[name: %s, uuid: %s] failed to sync snat%s, %s",
                            vr.getName(), vr.getUuid(), JSONObjectUtil.toJsonString(snatInfo), ret.getError());
                    chain.fail(err);
                } else {
                    Vip vip = getVipWithSnatService(data);
                    if (vip != null){
                        vip.acquire(new Completion(chain) {
                            @Override
                            public void success() {
                                chain.next();
                                return;
                            }
                            @Override
                            public void fail(ErrorCode errorCode) {
                                chain.fail(errorCode);
                            }
                        });
                    } else {
                        chain.next();
                    }
                }
            }
        });
    }

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        /* no need to release vip here, because when delete router, it will delete vip*/
        chain.rollback();
    }

    Vip getVipWithSnatService(Map data){
        String vipUuid = (String)data.get(VirtualRouterConstant.Param.PUB_VIP_UUID.toString());
        if (vipUuid == null){
            return null;
        }

        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
        struct.setUseFor(NetworkServiceType.SNAT.toString());

        Vip vip = new Vip(vipUuid);
        VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        if (!vr.getGuestL3Networks().isEmpty()){
            String l3NetworkUuuid = vr.getGuestL3Networks().get(0);
            try {
                NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(l3NetworkUuuid, NetworkServiceType.SNAT);
                struct.setPeerL3NetworkUuid(l3NetworkUuuid);
                struct.setServiceProvider(providerType.toString());
            } catch (OperationFailureException e){
                logger.debug(String.format("Get providerType exception %s", e.toString()));
            }
        }
        vip.setStruct(struct);
        return vip;
    }
}
