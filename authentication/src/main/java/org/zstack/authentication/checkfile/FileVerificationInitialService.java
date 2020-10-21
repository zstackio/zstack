package org.zstack.authentication.checkfile;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HostInventory;

public interface FileVerificationInitialService {
    
    void initManagementNodeFileVerificationList();
    
    void initHostFileVerificationList(HostInventory host, Completion completion);
    
}
