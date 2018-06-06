package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.IpRangeVO_;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.identity.Account;
import org.zstack.identity.AccountManager;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.vip.*;
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
    @Autowired
    private NetworkServiceManager nwServiceMgr;
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
        String accountUuid = Account.getAccountUuidOfResource(vr.getUuid());
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
        vipvo.setUsedIpUuid(nic.getUsedIpUuid());
        vipvo.setUseFor(VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE);
        vipvo.setAccountUuid(accountUuid);
        if(!vr.getGuestL3Networks().isEmpty()){
            String peerL3network = vr.getGuestL3Networks().get(0);
            try {
                NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(peerL3network,
                        NetworkServiceType.SNAT);
                vipvo.setServiceProvider(providerType.toString());
            } catch (OperationFailureException e){
                vipvo.setServiceProvider(null);
            }
        }

        VirtualRouterVipVO vrvip = new VirtualRouterVipVO();
        vrvip.setUuid(vipvo.getUuid());
        vrvip.setVirtualRouterVmUuid(vr.getUuid());
        new SQLBatch(){
            @Override
            protected void scripts() {
                persist(vipvo);
                persist(vrvip);
                tagMgr.copySystemTag(vr.getUuid(), VirtualRouterVmVO.class.getSimpleName(),
                        vipvo.getUuid(), VipVO.class.getSimpleName(), false);
            }
        }.execute();

        if(!vr.getGuestL3Networks().isEmpty()){
            VipPeerL3NetworkRefVO vo = new VipPeerL3NetworkRefVO();
            vo.setVipUuid(vipvo.getUuid());
            vo.setL3NetworkUuid(vr.getGuestL3Networks().get(0));
            dbf.persistAndRefresh(vo);
        }

        /* use for rollback */
        data.put(VirtualRouterConstant.Param.PUB_VIP_UUID.toString(), vipvo.getUuid());
        chain.next();
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        String vipUuid = (String) data.get(VirtualRouterConstant.Param.PUB_VIP_UUID.toString());
        if (vipUuid == null) {
            chain.rollback();
            return;
        }

        SQL.New(VirtualRouterVipVO.class).eq(VirtualRouterVipVO_.uuid, vipUuid).hardDelete();

        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
        struct.setUseFor(NetworkServiceType.SNAT.toString());
        Vip vip = new Vip(vipUuid);
        vip.setStruct(struct);
        vip.release(new Completion(chain) {
            @Override
            public void success() {
                chain.rollback();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                chain.rollback();
            }
        });
    }
}
