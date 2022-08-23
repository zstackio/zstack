package org.zstack.sdnController;

import org.zstack.header.core.Completion;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.sdnController.header.APIAddSdnControllerMsg;
import org.zstack.sdnController.header.APIRemoveSdnControllerMsg;
import org.zstack.sdnController.header.SdnControllerDeletionMsg;
import org.zstack.sdnController.header.SdnControllerVO;

public interface SdnControllerFactory {
    SdnControllerType getVendorType();

    SdnController getSdnController(SdnControllerVO vo);

    void createSdnController(SdnControllerVO vo, APIAddSdnControllerMsg msg, Completion completion);

    void deleteSdnController(SdnControllerVO vo, SdnControllerDeletionMsg msg, Completion completion);
}
