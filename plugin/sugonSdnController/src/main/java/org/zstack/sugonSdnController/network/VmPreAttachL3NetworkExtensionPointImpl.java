package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.CustomNicOperator;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmPreAttachL3NetworkExtensionPoint;
import org.zstack.identity.AccountManager;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortResponse;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/*
   request tf sdn controller to create a port and set the returned ip & mac & portId to system tag for later use
 */
public class VmPreAttachL3NetworkExtensionPointImpl implements VmPreAttachL3NetworkExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmPreAttachL3NetworkExtensionPointImpl.class);
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    private TfPortService tfPortService;

    @Override
    public void vmPreAttachL3Network(VmInstanceInventory vm, L3NetworkInventory l3) {
        if (!SugonSdnControllerConstant.L3_TF_NETWORK_TYPE.equals(l3.getType())) {
            return;
        }
        TfPortResponse port = tfPortService.createTfPort(null, vm, l3);

        String finalMac = port.getMacAddress();
        String finalIp = port.getFixedIps().get(0).getIpAddress();
        String nicUuid = StringDSL.transToZstackUuid(port.getPortId());

        CustomNicOperator nicOperator = new CustomNicOperator(vm.getUuid(), l3.getUuid());
        nicOperator.updateNicTags(finalMac,finalIp,nicUuid);
    }

}
