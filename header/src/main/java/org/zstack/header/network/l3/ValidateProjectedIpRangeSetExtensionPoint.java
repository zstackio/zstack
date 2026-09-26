package org.zstack.header.network.l3;

public interface ValidateProjectedIpRangeSetExtensionPoint {
    void validate(ReplaceProjectedIpRangesMsg message, L3NetworkVO l3Network);
}
