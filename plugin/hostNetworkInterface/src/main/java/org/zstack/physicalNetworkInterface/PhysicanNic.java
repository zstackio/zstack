package org.zstack.physicalNetworkInterface;

import org.zstack.header.message.Message;

public interface PhysicanNic {
    void handleMessage(Message msg);
}
