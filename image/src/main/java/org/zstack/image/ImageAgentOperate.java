package org.zstack.image;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by mingjian.deng on 16/11/15.
 */
public class ImageAgentOperate {
    public String getAgentFlagByImageUuid(String imageUuid) {
        return ImageSystemTags.IMAGE_INJECT_QEMUGA.getTokenByResourceUuid(imageUuid, ImageSystemTags.IMAGE_NAME_TOKEN);
    }

    public void attachAgentToImage(String imageUuid, String agentFlag) {
        ImageSystemTags.IMAGE_INJECT_QEMUGA.createInherentTag(imageUuid, map(e(ImageSystemTags.IMAGE_NAME_TOKEN, agentFlag)));
    }

    public void detachAgentFromImage(String imageUuid) {
        ImageSystemTags.IMAGE_INJECT_QEMUGA.deleteInherentTag(imageUuid);
    }

    public boolean isAgentAttachedToImage(String imageUuid) {
        return ImageSystemTags.IMAGE_INJECT_QEMUGA.hasTag(imageUuid);
    }
}