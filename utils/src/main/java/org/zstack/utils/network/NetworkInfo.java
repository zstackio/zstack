package org.zstack.utils.network;

public class NetworkInfo {
    public String ipv4Address;
    public String ipv4Gateway;
    public String ipv4Netmask;
    public String ipv6Address;
    public String ipv6Gateway;
    public String ipv6Prefix;

    public NetworkInfo(String ipv4Address, String ipv4Gateway, String ipv4Netmask, String ipv6Address, String ipv6Gateway, String ipv6Prefix) {
        this.ipv4Address = ipv4Address;
        this.ipv4Gateway = ipv4Gateway;
        this.ipv4Netmask = ipv4Netmask;
        this.ipv6Address = ipv6Address;
        this.ipv6Gateway = ipv6Gateway;
        this.ipv6Prefix = ipv6Prefix;
    }
}
