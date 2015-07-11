package org.zstack.header.volume;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APISyncCallMessage;

/**
 */
@Action(category = VolumeConstant.ACTION_CATEGORY, names = {"read"})
public class APIGetVolumeFormatMsg extends APISyncCallMessage {
}
