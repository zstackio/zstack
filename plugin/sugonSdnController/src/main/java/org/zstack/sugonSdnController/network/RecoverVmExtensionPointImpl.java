package org.zstack.sugonSdnController.network;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.CustomNicOperator;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.RecoverVmExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.sugonSdnController.controller.SugonSdnController;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortResponse;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.StringDSL;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class RecoverVmExtensionPointImpl implements RecoverVmExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private TfPortService tfPortService;

    @Override
    public void preRecoverVm(VmInstanceInventory vm) {
    }

    @Override
    public void afterRecoverVm(VmInstanceInventory vm) {

        for (VmNicInventory vmNic : vm.getVmNics()) {
            L3NetworkVO l3NetworkVO = dbf.findByUuid(vmNic.getL3NetworkUuid(), L3NetworkVO.class);
            if (!l3NetworkVO.getType().equals(SugonSdnControllerConstant.L3_TF_NETWORK_TYPE)) {
                continue;
            }

            // recover mac tag
            String mac = vmNic.getMac();
            if (StringUtils.isNotEmpty(mac)) {
                SystemTagCreator macCreator = VmSystemTags.CUSTOM_MAC.newSystemTagCreator(vm.getUuid());
                macCreator.ignoreIfExisting = true;
                macCreator.inherent = false;
                macCreator.setTagByTokens(map(
                        e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, vmNic.getL3NetworkUuid()),
                        e(VmSystemTags.MAC_TOKEN, mac)
                ));
                macCreator.create();
            }
            CustomNicOperator customNicOperator = new CustomNicOperator(vm.getUuid(), vmNic.getL3NetworkUuid());
            TfPortResponse port = tfPortService.createTfPort(StringDSL.transToTfUuid(customNicOperator.getCustomNicId()), vm, L3NetworkInventory.valueOf(l3NetworkVO));
            String finalMac = port.getMacAddress();
            String finalIp = port.getFixedIps().get(0).getIpAddress();
            String nicUuid = StringDSL.transToZstackUuid(port.getPortId());
            customNicOperator.updateNicTags(finalMac, finalIp, nicUuid);
        }
    }

    @Override
    public void beforeRecoverVm(VmInstanceInventory vm) {

    }
}
