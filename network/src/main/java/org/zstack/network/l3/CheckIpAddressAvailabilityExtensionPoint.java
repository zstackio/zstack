package org.zstack.network.l3;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.network.l3.CheckIpAvailabilityMsg;
import org.zstack.header.network.l3.CheckIpAvailabilityReply;

public interface CheckIpAddressAvailabilityExtensionPoint {
    public void check(CheckIpAvailabilityMsg msg,  ReturnValueCompletion<CheckIpAvailabilityReply> completion);
}
