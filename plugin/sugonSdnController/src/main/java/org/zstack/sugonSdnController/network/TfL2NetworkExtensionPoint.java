package org.zstack.sugonSdnController.network;

import org.zstack.header.core.Completion;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkVO;

public interface TfL2NetworkExtensionPoint {
    void createTfL2NetworkOnSdnController(L2NetworkVO vo, APICreateL2NetworkMsg msg, Completion completion);
    void deleteTfL2NetworkOnSdnController(L2NetworkVO vo, Completion completion);
    void updateTfL2NetworkOnSdnController(L2NetworkVO vo, Completion completion);
}
