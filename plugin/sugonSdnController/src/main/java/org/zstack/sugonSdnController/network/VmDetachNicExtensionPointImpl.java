package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.zstack.compute.vm.CustomNicOperator;
import org.zstack.compute.vm.MacOperator;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmFailToAttachL3NetworkExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.VmDetachNicExtensionPoint;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.identity.AccountManager;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortClient;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortResponse;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class VmDetachNicExtensionPointImpl implements VmDetachNicExtensionPoint, VmFailToAttachL3NetworkExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmDetachNicExtensionPointImpl.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager accountMgr;

    @Override
    public void preDetachNic(VmNicInventory nic) {
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic) {
    }

    @Override
    public void afterDetachNic(VmNicInventory nic) {
        L3NetworkVO l3nw = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
        if (SugonSdnControllerConstant.L3_TF_NETWORK_TYPE.equals(l3nw.getType())) {
            CustomNicOperator nicOperator = new CustomNicOperator();
            nicOperator.deleteNicTags(nic.getVmInstanceUuid(), l3nw.getUuid());
            deleteTfPort(nic.getUuid(), nic.getL3NetworkUuid());
        }
    }

    @Override
    public void failedToDetachNic(VmNicInventory nic, ErrorCode error) {
    }

    @Override
    public void vmFailToAttachL3Network(VmInstanceInventory vm, L3NetworkInventory l3, ErrorCode error) {
        if (SugonSdnControllerConstant.L3_TF_NETWORK_TYPE.equals(l3.getType())) {
            CustomNicOperator nicOperator = new CustomNicOperator();
            String customNicId = nicOperator.getCustomNicId(vm.getUuid(), l3.getUuid());
            nicOperator.deleteNicTags(vm.getUuid(), l3.getUuid());
            deleteTfPort(customNicId, l3.getUuid());
        }
    }



    private void deleteTfPort(String nicUuid, String l3NetworkUuid) {
        TfPortClient client = new TfPortClient();
        String accountId = accountMgr.getOwnerAccountUuidOfResource(l3NetworkUuid);
        TfPortResponse response = client.deletePort(StringDSL.transToTfUuid(nicUuid), accountId);
        if (response.getCode() != HttpStatus.OK.value()) {
            throw new RuntimeException("failed to invoke deleting tf port: " + response);
        }
    }
}
