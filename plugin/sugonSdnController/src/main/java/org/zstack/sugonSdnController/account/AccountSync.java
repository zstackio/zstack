package org.zstack.sugonSdnController.account;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.sugonSdnController.controller.SugonSdnController;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.core.db.Q;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.BeforeCreateAccountExtensionPoint;
import org.zstack.header.identity.BeforeDeleteAccountExtensionPoint;
import org.zstack.header.identity.BeforeUpdateAccountExtensionPoint;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerManager;
import org.zstack.sdnController.header.SdnControllerVO;
import org.zstack.sdnController.header.SdnControllerVO_;

/**
 * @description:
 * @author: liupt@sugon.com
 * @create: 2022-10-09
 **/
public class AccountSync implements BeforeCreateAccountExtensionPoint, BeforeUpdateAccountExtensionPoint, BeforeDeleteAccountExtensionPoint {
    @Autowired
    SdnControllerManager sdnControllerManager;

    @Override
    public void beforeCreateAccount(AccountInventory account)  {
        SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
        SdnController sdnController = sdnControllerManager.getSdnController(sdn);
        SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
        sugonSdnController.createAccount(account);
    }

    @Override
    public void beforeDeleteAccount(AccountInventory account) {
        SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
        SdnController sdnController = sdnControllerManager.getSdnController(sdn);
        SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
        sugonSdnController.deleteAccount(account);
    }

    @Override
    public void beforeUpdateAccount(AccountInventory account)  {
        SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
        SdnController sdnController = sdnControllerManager.getSdnController(sdn);
        SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
        sugonSdnController.updateAccount(account);
    }
}
