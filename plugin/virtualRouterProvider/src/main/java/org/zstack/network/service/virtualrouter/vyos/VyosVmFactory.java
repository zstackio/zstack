package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmType;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by weiwang on 19/09/2017
 */
public class VyosVmFactory extends VyosVmBaseFactory {
    private static final CLogger logger = Utils.getLogger(VyosVmFactory.class);
    public static ApplianceVmType applianceVmType = new ApplianceVmType(VyosConstants.VYOS_VM_TYPE);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public ApplianceVmType getApplianceVmType() {
        return applianceVmType;
    }
}
