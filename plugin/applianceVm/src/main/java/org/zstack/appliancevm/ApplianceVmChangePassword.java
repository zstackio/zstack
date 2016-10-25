package org.zstack.appliancevm;

import org.zstack.header.vm.VmBeforeStartOnHypervisorExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by mingjian.deng on 16/10/25.
 */
public class ApplianceVmChangePassword implements VmBeforeStartOnHypervisorExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ApplianceVmChangePassword.class);

    private void injectPassword(VmInstanceSpec spec){
        logger.debug("test injectPassword beforeStart!");
    }

    @Override
    public void beforeStartVmOnHypervisor(VmInstanceSpec spec) {
        injectPassword(spec);
    }
}
