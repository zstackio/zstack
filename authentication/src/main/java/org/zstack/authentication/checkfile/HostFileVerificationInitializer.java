package org.zstack.authentication.checkfile;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostAddExtensionPoint;
import org.zstack.header.host.HostInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by Wenhao.Zhang on 20/10/21
 */
public class HostFileVerificationInitializer implements HostAddExtensionPoint {
    private static CLogger logger = Utils.getLogger(HostFileVerificationInitializer.class);
    
    @Autowired
    FileVerificationInitialService initializer;
    
    @Override
    public void beforeAddHost(HostInventory host, Completion completion) {
        completion.success();
    }
    
    @Override
    public void afterAddHost(HostInventory host, Completion completion) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            completion.success();
            return;
        }
        
        initializer.initHostFileVerificationList(host, completion);
    }
}
