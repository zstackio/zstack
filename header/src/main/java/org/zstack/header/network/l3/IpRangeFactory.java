package org.zstack.header.network.l3;

import org.zstack.header.core.ReturnValueCompletion;

import java.util.List;

public interface IpRangeFactory {
    IpRangeType getType();

    void createIpRange(List<IpRangeInventory> iprs, AddIpRangeMsg msg, ReturnValueCompletion<List<IpRangeInventory>> completion);
}
