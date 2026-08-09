package org.zstack.header.network.l3;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.network.l2.NetworkCreateContext;

import java.util.List;

public interface IpRangeFactory {
    IpRangeType getType();

    void createIpRange(List<IpRangeInventory> iprs, APICreateMessage msg, ReturnValueCompletion<List<IpRangeInventory>> completion);

    default void createIpRange(List<IpRangeInventory> iprs, APICreateMessage msg, NetworkCreateContext context,
                               ReturnValueCompletion<List<IpRangeInventory>> completion) {
        createIpRange(iprs, msg, completion);
    }
}
