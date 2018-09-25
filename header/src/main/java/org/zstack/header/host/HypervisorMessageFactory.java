package org.zstack.header.host;

import java.util.List;

public interface HypervisorMessageFactory {
    HypervisorType getHypervisorType();
    List<AddHostMsg> buildMessageFromFile(String content);
}
