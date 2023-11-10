package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.BeforeAllocateIpExtensionPoint;
import org.zstack.header.network.l3.AllocateIpMsg;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.sugonSdnController.controller.api.Status;
import org.zstack.sugonSdnController.controller.api.types.KeyValuePairs;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortResponse;
import org.zstack.sugonSdnController.network.TfPortService;

import java.util.Objects;

public class InstanceBeforeAllocateIpExtensionPointImpl implements BeforeAllocateIpExtensionPoint {
    @Autowired
    private TfPortService tfPortService;
    @Autowired
    protected DatabaseFacade dbf;
    @Override
    public String allocateIpBySdn(AllocateIpMsg msg, String bmUuid, String mac, String switchInfo) {
        if (switchInfo == null || switchInfo.length() == 0) {
            return null;
        }
        L3NetworkInventory l3 = L3NetworkInventory.valueOf(dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class));
        if (!Objects.equals(l3.getType(), SugonSdnControllerConstant.L3_TF_NETWORK_TYPE)) {
            return null;
        }
        String portUuid = Platform.getUuid();
        String switchs = "";
        String interfaces = "";
        String[] switchList = switchInfo.split("-");
        for (String swInfo : switchList) {
            String[] swInfoList = swInfo.split(",");
            switchs += (swInfoList[0] + ',');
            interfaces += (swInfoList[1] + ',');
        }
        switchs = switchs.substring(0, switchs.length() - 1);
        interfaces = interfaces.substring(0, interfaces.length() - 1);
        String swInfo = String.format("{\"switch_ip\": \"%s\", \"switch_interface\": \"%s\"}", switchs, interfaces);
        KeyValuePairs bindInfo = new KeyValuePairs();
        bindInfo.addKeyValuePair("vnic_type", "baremetal");

        TfPortResponse port = tfPortService.createTfPort(l3, portUuid, msg.getRequiredIp(), mac, bindInfo);
        if (port.getFixedIps().size() == 0) {
            tfPortService.deleteTfPort(port.getPortId());
            throw new RuntimeException(String.format("Can not allocate ip address from tf for baremetal's nic[mac:%s].", mac));
        }
        msg.setRequiredIp(port.getFixedIps().get(0).getIpAddress());
        try {
            //swInfo example: {"profile", "{\"switch_ip\": \"1.1.1.1,2.2.2.2\", \"switch_interface\":\"0/0/1,0/0/2\"}}
            bindInfo.addKeyValuePair("profile", swInfo);
            Status result = tfPortService.updateTfPort(portUuid, bmUuid, bindInfo);
            if (!result.isSuccess()) {
                throw new RuntimeException(String.format("Update tf port [%s] failed.", portUuid));
            }
        } catch (Exception e) {
            tfPortService.deleteTfPort(portUuid);
            throw new RuntimeException(e.getMessage());
        }

        return portUuid;
    }

    @Override
    public void releaseIpFromSdn(String nicUuid, String l3NetworkUuid) {
        if (l3NetworkUuid == null || l3NetworkUuid.isEmpty()) {
            return;
        }
        L3NetworkInventory l3 = L3NetworkInventory.valueOf(dbf.findByUuid(l3NetworkUuid, L3NetworkVO.class));
        if (!Objects.equals(l3.getType(), SugonSdnControllerConstant.L3_TF_NETWORK_TYPE)) {
            return;
        }
        tfPortService.deleteTfPort(nicUuid);
    }
}