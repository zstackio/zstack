package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.VmDetachNicExtensionPoint;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.identity.AccountManager;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortClient;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class VmDetachNicExtensionPointImpl implements VmDetachNicExtensionPoint {
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
        String accountId = accountMgr.getOwnerAccountUuidOfResource(l3nw.getUuid());
        if(SugonSdnControllerConstant.L3_TF_NETWORK_TYPE.equals(l3nw.getType())) {
            TfPortClient client = new TfPortClient();
            client.deletePort(StringDSL.transToTfUuid(nic.getUuid()), accountId);
        }
    }

    @Override
    public void failedToDetachNic(VmNicInventory nic, ErrorCode error) {
    }
}
