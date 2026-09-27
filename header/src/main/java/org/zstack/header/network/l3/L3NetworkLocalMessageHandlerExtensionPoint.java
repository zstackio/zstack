package org.zstack.header.network.l3;

import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;

public interface L3NetworkLocalMessageHandlerExtensionPoint {
    Class<? extends NeedReplyMessage> getMessageClass();

    // Runs synchronously in the L3 operation queue; the caller owns reply and queue release.
    MessageReply handle(NeedReplyMessage message, L3NetworkInventory l3);
}
