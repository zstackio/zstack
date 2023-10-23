package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;

/**
 * Created by boce.wang on 11/10/2023.
 */
public interface AfterL2NetworkRealizationExtensionPoint {
    void afterRealize(L2NetworkInventory l2Network, String hostUuid, Completion completion);

}
