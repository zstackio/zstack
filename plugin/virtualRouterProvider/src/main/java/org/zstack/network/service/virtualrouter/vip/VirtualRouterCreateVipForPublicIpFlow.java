package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.*;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.IpRangeVO_;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.identity.AccountManager;
import org.zstack.network.service.vip.VipState;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterCreateVipForPublicIpFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private AccountManager acntMgr;
    private final static CLogger logger = Utils.getLogger(VirtualRouterCreateVipForPublicIpFlow.class);

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        VmNicInventory nic = vr.getPublicNic();
        IpRangeInventory ips = null;
        List<IpRangeVO> ipRanges = Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, nic.getL3NetworkUuid()).list();
        for (IpRangeVO range : ipRanges){
            if (NetworkUtils.isIpv4InRange(nic.getIp(), range.getStartIp(), range.getEndIp())){
                ips = IpRangeInventory.valueOf(range);
                break;
            }
        }
        if (ips == null) {
            ErrorCode err = operr("virtual router[name: %s, uuid: %s] failed to create vip for public ip %s because "+
                            "no ip range for l3NetworkUuid %s", vr.getName(), vr.getUuid(), nic.getIp(), nic.getL3NetworkUuid());
            chain.fail(err);
            return ;
        }

        /* vip db */
        VipVO vipvo = new VipVO();
        vipvo.setUuid(Platform.getUuid());
        vipvo.setName(String.format("vip-for-%s", vr.getName()));
        vipvo.setDescription("Vip backend created for virtual router");
        vipvo.setState(VipState.Enabled);
        vipvo.setGateway(nic.getGateway());
        vipvo.setIp(nic.getIp());
        vipvo.setIpRangeUuid(ips.getUuid());
        vipvo.setL3NetworkUuid(nic.getL3NetworkUuid());
        vipvo.setNetmask(nic.getNetmask());
        vipvo.setUsedIpUuid(nic.getUuid());
        //vipvo.setUseFor(VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE);

        VirtualRouterVipVO vrvip = new VirtualRouterVipVO();
        vrvip.setUuid(vipvo.getUuid());
        vrvip.setVirtualRouterVmUuid(vr.getUuid());
        String vrAccount = acntMgr.getOwnerAccountUuidOfResource(vr.getUuid());
        new SQLBatch(){
            @Override
            protected void scripts() {
                persist(vipvo);
                persist(vrvip);
                acntMgr.createAccountResourceRef(vrAccount, vipvo.getUuid(), VipVO.class);
                tagMgr.copySystemTag(vr.getUuid(), VirtualRouterVmVO.class.getSimpleName(),
                        vipvo.getUuid(), VipVO.class.getSimpleName());
            }
        }.execute();

        /* use for rollback */
        data.put(VirtualRouterConstant.Param.PUB_VIP_UUID.toString(), vipvo.getUuid());
        chain.next();
        return ;
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {

        VirtualRouterVipVO vrVip = new VirtualRouterVipVO();
        vrVip.setUuid((String) data.get(VirtualRouterConstant.Param.PUB_VIP_UUID.toString()));
        vrVip.setVirtualRouterVmUuid((String) data.get(VirtualRouterConstant.Param.VR_UUID.toString()));
        dbf.remove(vrVip);

        chain.rollback();
    }
}
