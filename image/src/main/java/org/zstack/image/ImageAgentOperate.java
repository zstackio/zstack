package org.zstack.image;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by mingjian.deng on 16/11/15.
 */
public class ImageAgentOperate {
    public String getAgentFlagByImageUuid(String imageUuid) {
        return ImageSystemTags.IMAGE_INJECT_QEMUGA.getTokenByResourceUuid(imageUuid, ImageSystemTags.IMAGE_INJECT_QEMUGA_TOKEN);
    }

    public void attachAgentToImage(String imageUuid, String agentFlag) {
        ImageSystemTags.IMAGE_INJECT_QEMUGA.createTag(imageUuid, map(e(ImageSystemTags.IMAGE_INJECT_QEMUGA_TOKEN, agentFlag)));
    }

    public void detachAgentFromImage(String imageUuid) {
        ImageSystemTags.IMAGE_INJECT_QEMUGA.delete(imageUuid);
    }

    public boolean isAgentAttachedToImage(String imageUuid) {
        return ImageSystemTags.IMAGE_INJECT_QEMUGA.hasTag(imageUuid);
    }
}