package org.zstack.sdnController;

import org.zstack.header.core.Completion;
import org.zstack.sdnController.header.*;


public interface SdnController {

    void handleMessage(SdnControllerMessage msg);
    /*
    有关sdn控制器的前置检查: pre-event
    对sdn控制器的控制: event
    有关sdn控制器的后置处理: post-event
     */
    void preInitSdnController(APIAddSdnControllerMsg msg, Completion completion);
    void createSdnControllerDb(APIAddSdnControllerMsg msg, SdnControllerVO vo, Completion completion);
    void deleteSdnControllerDb(SdnControllerVO vo);
    void initSdnController(APIAddSdnControllerMsg msg, Completion completion);
    void postInitSdnController(SdnControllerVO vo, Completion completion);

    void deleteSdnController(SdnControllerDeletionMsg msg, SdnControllerInventory sdn, Completion completion);

    default void addHost(APISdnControllerAddHostMsg msg, Completion completion) {completion.success();};
    default void removeHost(SdnControllerRemoveHostMsg msg, Completion completion) {completion.success();};

    default void changeHost(SdnControllerHostRefVO oldRef, SdnControllerHostRefVO newRef, Completion completion) {completion.success();};
}
