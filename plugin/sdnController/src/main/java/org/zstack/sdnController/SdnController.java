package org.zstack.sdnController;

import org.zstack.header.core.Completion;
import org.zstack.header.network.sdncontroller.*;
import org.zstack.sdnController.header.*;

import java.util.List;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_SDNCONTROLLER_10035;


public interface SdnController {

    void handleMessage(SdnControllerMessage msg);
    /*
    有关sdn控制器的前置检查: pre-event
    对sdn控制器的控制: event
    有关sdn控制器的后置处理: post-event
     */
    void preInitSdnController(AddSdnControllerMsg msg, Completion completion);
    void createSdnControllerDb(AddSdnControllerMsg msg, SdnControllerVO vo, Completion completion);
    void deleteSdnControllerDb(SdnControllerVO vo);
    void initSdnController(AddSdnControllerMsg msg, Completion completion);
    void postInitSdnController(SdnControllerVO vo, Completion completion);

    void deleteSdnController(SdnControllerDeletionMsg msg, SdnControllerInventory sdn, Completion completion);

    default void reconnectSdnController(Completion completion) {completion.success();};

    default void addHost(APISdnControllerAddHostMsg msg, Completion completion) {completion.success();};
    default void removeHost(SdnControllerRemoveHostMsg msg, Completion completion) {completion.success();};

    default void changeHost(SdnControllerHostRefVO oldRef, SdnControllerHostRefVO newRef, Completion completion) {completion.success();};

    default void pullResources(String sdnControllerUuid, String resourceType,
                               List<String> resourceUuids, Completion completion) {
        completion.fail(org.zstack.core.Platform.operr(
                ORG_ZSTACK_SDNCONTROLLER_10035,
                "Resource pull is not supported by sdn controller[%s]", sdnControllerUuid));
    }

    /**
     * Returns whether this controller can change its endpoint through the common API.
     *
     * Implementations returning {@code true} must override {@link #changeIp(APIChangeSdnControllerMsg, Completion)}.
     */
    default boolean supportsIpChange() { return false; }

    /**
     * Changes this controller's endpoint.
     *
     * This method is called only when {@link #supportsIpChange()} returns {@code true}.
     */
    default void changeIp(APIChangeSdnControllerMsg msg, Completion completion) { completion.success(); }
}
