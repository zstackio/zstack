package org.zstack.header.network.l3;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class ReplaceProjectedIpRangesReply extends MessageReply {
    private List<IpRangeInventory> ranges;

    public List<IpRangeInventory> getRanges() { return ranges; }
    public void setRanges(List<IpRangeInventory> value) { ranges = value; }
}
