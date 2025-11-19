package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmType;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.appliancevm.ApvmCascadeFilterExtensionPoint;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.lb.LoadBalancerConstants;
import org.zstack.network.service.vip.VipGetUsedPortRangeExtensionPoint;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalConfig;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalProperty;
import org.zstack.network.service.virtualrouter.VirtualRouterManagerImpl;
import org.zstack.utils.RangeSet;
import org.zstack.utils.Utils;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

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
        /* system vip is the vip of public ip of vpc or vpc ha group */
        boolean hasSnat = vipVO.getServicesTypes().contains(NetworkServiceType.SNAT.toString());
        if (vipVO.isSystem()) {
            if (protocol.equalsIgnoreCase(LoadBalancerConstants.LB_PROTOCOL_UDP) && hasSnat){
                portRanges.add(new RangeSet.Range(VyosConstants.DNS_PORT, VyosConstants.DNS_PORT, true));
                portRanges.add(new RangeSet.Range(VyosConstants.NTP_PORT, VyosConstants.NTP_PORT, true));
            }

            if (protocol.equalsIgnoreCase(LoadBalancerConstants.LB_PROTOCOL_TCP) && hasSnat){
                portRanges.add(new RangeSet.Range(VyosConstants.DNS_PORT, VyosConstants.DNS_PORT, true));

                int sshPort = VirtualRouterGlobalConfig.SSH_PORT.value(Integer.class);
                portRanges.add(new RangeSet.Range(sshPort, sshPort, true));

                int agentPort = VirtualRouterGlobalProperty.AGENT_PORT;
                portRanges.add(new RangeSet.Range(agentPort, agentPort, true));
            }
        }

        return portRangeList;
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
