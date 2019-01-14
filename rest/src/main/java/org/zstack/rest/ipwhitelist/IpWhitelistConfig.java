package org.zstack.rest.ipwhitelist;

/**
 * Created by lining on 2019/1/14.
 */
public class IpWhitelistConfig {
    private IpAddressRangeType ipAddressRangeType;

    private String ip;

    private IpWhitelistConfigState state;

    public IpAddressRangeType getIpAddressRangeType() {
        return ipAddressRangeType;
    }

    public void setIpAddressRangeType(IpAddressRangeType ipAddressRangeType) {
        this.ipAddressRangeType = ipAddressRangeType;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public IpWhitelistConfigState getState() {
        return state;
    }

    public void setState(IpWhitelistConfigState state) {
        this.state = state;
    }
}
