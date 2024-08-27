package org.zstack.sugonSdnController.account;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.identity.AfterCreateAccountExtensionPoint;
import org.zstack.sugonSdnController.controller.SugonSdnController;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.core.db.Q;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.BeforeDeleteAccountExtensionPoint;
import org.zstack.header.identity.AfterUpdateAccountExtensionPoint;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerManager;
import org.zstack.sdnController.header.SdnControllerVO;
import org.zstack.sdnController.header.SdnControllerVO_;

/**
 * @description:
 * @author: liupt@sugon.com
 * @create: 2022-10-09
 **/
public class AccountSync implements AfterCreateAccountExtensionPoint, AfterUpdateAccountExtensionPoint, BeforeDeleteAccountExtensionPoint {
    @Autowired
    SdnControllerManager sdnControllerManager;

    @Override
    public void afterCreateAccount(AccountInventory account)  {
        SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
        if (sdn == null) {
            return;
        }
        SdnController sdnController = sdnControllerManager.getSdnController(sdn);
        SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
        sugonSdnController.createAccount(account);
    }

    @Override
    public void beforeDeleteAccount(AccountInventory account) {
        SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
        if (sdn == null) {
            return;
        }
        SdnController sdnController = sdnControllerManager.getSdnController(sdn);
        SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
        sugonSdnController.deleteAccount(account);
    }

    @Override
    public void afterUpdateAccount(AccountInventory account)  {
        SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
        if (sdn == null) {
            return;
        }
        SdnController sdnController = sdnControllerManager.getSdnController(sdn);
        SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
        sugonSdnController.updateAccount(account);
    }
}
