package org.zstack.sdk;

public class GetVipQosResult {
    public long outboundBandwidth;
    public void setOutboundBandwidth(long outboundBandwidth) {
        this.outboundBandwidth = outboundBandwidth;
    }
    public long getOutboundBandwidth() {
        return this.outboundBandwidth;
    }

    public long inboundBandwidth;
    public void setInboundBandwidth(long inboundBandwidth) {
        this.inboundBandwidth = inboundBandwidth;
    }
    public long getInboundBandwidth() {
        return this.inboundBandwidth;
    }

}
