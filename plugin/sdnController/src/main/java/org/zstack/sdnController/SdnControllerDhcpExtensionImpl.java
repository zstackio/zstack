package org.zstack.sdnController;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.sdnController.header.SdnControllerVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public class SdnControllerDhcpExtensionImpl implements IpRangeBackendExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SdnControllerDhcpExtensionImpl.class);

    @Autowired
    DatabaseFacade dbf;
    @Autowired
    SdnControllerManager sdnMgr;


    @Override
    public void addIpRange(List<IpRangeInventory> iprs, Completion completion) {
        L3NetworkVO l3Vo = dbf.findByUuid(iprs.get(0).getL3NetworkUuid(), L3NetworkVO.class);
        L2NetworkVO l2Vo = dbf.findByUuid(l3Vo.getL2NetworkUuid(), L2NetworkVO.class);

        String sdnControllerUuid = SdnControllerHelper.getSdnControllerUuidFromL2Uuid(l2Vo.getUuid());
        if (sdnControllerUuid == null) {
            completion.success();
            return;
        }

        SdnControllerVO vo = dbf.findByUuid(sdnControllerUuid, SdnControllerVO.class);
        SdnControllerFactory factory = sdnMgr.getSdnControllerFactory(vo.getVendorType());
        SdnControllerDhcp dhcp = factory.getSdnControllerDhcp(vo);
        if (dhcp == null) {
            completion.success();
            return;
        }

        dhcp.addIpRange(iprs, false, completion);
    }

    @Override
    public void removeIpRange(List<IpRangeInventory> iprs, Completion completion) {
        L3NetworkVO l3Vo = dbf.findByUuid(iprs.get(0).getL3NetworkUuid(), L3NetworkVO.class);
        L2NetworkVO l2Vo = dbf.findByUuid(l3Vo.getL2NetworkUuid(), L2NetworkVO.class);

        String sdnControllerUuid = SdnControllerHelper.getSdnControllerUuidFromL2Uuid(l2Vo.getUuid());
        if (sdnControllerUuid == null) {
            completion.success();
            return;
        }

        SdnControllerVO vo = dbf.findByUuid(sdnControllerUuid, SdnControllerVO.class);
        SdnControllerFactory factory = sdnMgr.getSdnControllerFactory(vo.getVendorType());
        SdnControllerDhcp dhcp = factory.getSdnControllerDhcp(vo);
        if (dhcp == null) {
            completion.success();
            return;
        }

        dhcp.removeIpRange(iprs, completion);
    }
}
