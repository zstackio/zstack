package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.APICreateVmNicMsg;
import org.zstack.header.vm.NicManageExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortIpEntity;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortResponse;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;


public class TfNicManageExtensionPointImpl implements NicManageExtensionPoint {
    private static final CLogger logger = Utils.getLogger(TfNicManageExtensionPointImpl.class);

    @Autowired
    private TfPortService tfPortService;

    @Override
    public void beforeCreateNic(VmNicInventory nic, APICreateVmNicMsg msg) {
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if (!SugonSdnControllerConstant.L3_TF_NETWORK_TYPE.equals(l3Network.getType())) {
            return;
        }
        nic.setType(VmInstanceConstant.TF_VIRTUAL_NIC_TYPE);
        TfPortResponse port = null;
        String portUuid = msg.getResourceUuid();
        String tfPortUuid = null;
        if (portUuid != null){
            tfPortUuid = StringDSL.transToTfUuid(portUuid);
            port = tfPortService.getTfPort(tfPortUuid);
            if (port != null){
                String ipAddr = null;
                for (TfPortIpEntity ipEntrty: port.getFixedIps()) {
                    if (ipEntrty.getSubnetId().equals(StringDSL.transToTfUuid(msg.getL3NetworkUuid()))) {
                        ipAddr = ipEntrty.getIpAddress();
                    }
                }
                if (ipAddr == null) {
                    throw new RuntimeException(String.format("Tf port with uuid[%s] exists, " +
                            "but it's subnet id not equal with the l3NetworkUuid in param.", msg.getResourceUuid()));
                }
                msg.setIp(ipAddr);
                nic.setMac(port.getMacAddress());
                logger.debug(String.format("Tf port with uuid[%s] exists, just save ip info to zstack db.",
                        msg.getResourceUuid()));
            }
        }
        if (port == null) {
            String l2NetworkUuid = l3Network.getL2NetworkUuid();
            port = tfPortService.createTfPort(tfPortUuid, l2NetworkUuid, msg.getL3NetworkUuid(),
                    nic.getMac(), msg.getIp());
            logger.debug("Create a new tf port success.");
        }
        nic.setUuid(StringDSL.transToZstackUuid(port.getPortId()));
    }

    @Override
    public void beforeDeleteNic(VmNicInventory nic) {
        if (!VmInstanceConstant.TF_VIRTUAL_NIC_TYPE.equals(nic.getType())) {
            return;
        }
        tfPortService.deleteTfPort(nic.getUuid());
    }

}
