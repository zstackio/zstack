package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmType;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.appliancevm.ApvmCascadeFilterExtensionPoint;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.l3.UsedIpVO_;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.lb.LoadBalancerConstants;
import org.zstack.network.service.vip.VipGetUsedPortRangeExtensionPoint;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalConfig;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalProperty;
import org.zstack.network.service.virtualrouter.VirtualRouterManagerImpl;
import org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO_;
import org.zstack.utils.RangeSet;
import org.zstack.utils.Utils;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by weiwang on 19/09/2017
 */
public class VyosVmFactory extends VyosVmBaseFactory implements VipGetUsedPortRangeExtensionPoint, ApvmCascadeFilterExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VyosVmFactory.class);
    public static ApplianceVmType applianceVmType = new ApplianceVmType(VyosConstants.VYOS_VM_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VirtualRouterManagerImpl vrMgr;

    @Override
    public ApplianceVmType getApplianceVmType() {
        return applianceVmType;
    }

    @Override
    public RangeSet getVipUsePortRange(String vipUuid, String protocol, VipUseForList useForList) {
        RangeSet portRangeList = new RangeSet();
        List<RangeSet.Range> portRanges = new ArrayList<RangeSet.Range>();
        portRangeList.setRanges(portRanges);

        VipVO vipVO = dbf.findByUuid(vipUuid, VipVO.class);
        if (isVirtualRouterManagementNicIp(vipVO)) {
            if (protocol.equalsIgnoreCase(LoadBalancerConstants.LB_PROTOCOL_UDP)){
                portRanges.add(new RangeSet.Range(VyosConstants.DNS_PORT, VyosConstants.DNS_PORT, true));
                portRanges.add(new RangeSet.Range(VyosConstants.NTP_PORT, VyosConstants.NTP_PORT, true));
            }

            if (!protocol.equalsIgnoreCase(LoadBalancerConstants.LB_PROTOCOL_UDP)){
                portRanges.add(new RangeSet.Range(VyosConstants.DNS_PORT, VyosConstants.DNS_PORT, true));

                int sshPort = VirtualRouterGlobalConfig.SSH_PORT.value(Integer.class);
                portRanges.add(new RangeSet.Range(sshPort, sshPort, true));

                int agentPort = VirtualRouterGlobalProperty.AGENT_PORT;
                portRanges.add(new RangeSet.Range(agentPort, agentPort, true));
            }
        }

        return portRangeList;
    }

    private boolean isVirtualRouterManagementNicIp(VipVO vipVO) {
        List<String> nicUuids = new ArrayList<>();
        UsedIpVO vipUsedIp = vipVO.getUsedIpUuid() == null ? null :
                Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, vipVO.getUsedIpUuid()).find();
        if (vipUsedIp != null
                && Objects.equals(vipUsedIp.getIp(), vipVO.getIp())
                && Objects.equals(vipUsedIp.getL3NetworkUuid(), vipVO.getL3NetworkUuid())
                && vipUsedIp.getVmNicUuid() != null) {
            nicUuids.add(vipUsedIp.getVmNicUuid());
        } else {
            nicUuids.addAll(Q.New(UsedIpVO.class)
                    .select(UsedIpVO_.vmNicUuid)
                    .eq(UsedIpVO_.ip, vipVO.getIp())
                    .eq(UsedIpVO_.l3NetworkUuid, vipVO.getL3NetworkUuid())
                    .notNull(UsedIpVO_.vmNicUuid)
                    .listValues());
        }

        if (nicUuids.isEmpty()) {
            return false;
        }

        List<VmNicVO> managementNics = Q.New(VmNicVO.class)
                .in(VmNicVO_.uuid, nicUuids)
                .in(VmNicVO_.metaData, VirtualRouterNicMetaData.MANAGEMENT_NIC_MASK_STRING_LIST)
                .list();

        for (VmNicVO nic : managementNics) {
            if (Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).isExists()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<ApplianceVmVO> filterApplianceVmCascade(List<ApplianceVmVO> applianceVmVOS, CascadeAction action,
                                                        String parentIssuer,
                                                        List<String> parentIssuerUuids,
                                                        List<VmNicInventory> toDeleteNics,
                                                        List<UsedIpInventory> toDeleteIps) {
        logger.debug(String.format("filter appliance vm type %s with parentIssuer [type: %s, uuids: %s]",
                VyosConstants.VYOS_VM_TYPE, parentIssuer, parentIssuerUuids));
        
        if (parentIssuer.equals(L3NetworkVO.class.getSimpleName())) {
            List<ApplianceVmVO> vos = vrMgr.applianceVmsToBeDeleted(applianceVmVOS, parentIssuerUuids);

            applianceVmVOS.removeAll(vos);
            toDeleteNics.addAll(vrMgr.applianceVmsAdditionalPublicNic(applianceVmVOS, parentIssuerUuids));

            return vos;
        } else if (parentIssuer.equals(IpRangeVO.class.getSimpleName())) {
            List<ApplianceVmVO> vos = vrMgr.applianceVmsToBeDeletedByIpRanges(applianceVmVOS, parentIssuerUuids);
            applianceVmVOS.removeAll(vos);
            toDeleteNics.addAll(VmNicInventory.valueOf(vrMgr.applianceVmsToDeleteNicByIpRanges(applianceVmVOS, parentIssuerUuids)));

            return vos;
        } else {
            return applianceVmVOS;
        }
    }
}
