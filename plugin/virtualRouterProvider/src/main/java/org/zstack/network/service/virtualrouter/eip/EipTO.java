package org.zstack.network.service.virtualrouter.eip;

import java.io.Serializable;

/**
 */
public class EipTO implements Serializable {
    private String vipIp;
    private String privateMac;
    private String guestIp;
    private boolean snatInboundTraffic;

    public boolean isSnatInboundTraffic() {
        return snatInboundTraffic;
    }

    public void setSnatInboundTraffic(boolean snatInboundTraffic) {
        this.snatInboundTraffic = snatInboundTraffic;
    }

    public String getGuestIp() {
        return guestIp;
    }

    public void setGuestIp(String guestIp) {
        this.guestIp = guestIp;
    }

    public String getVipIp() {
        return vipIp;
    }

    public void setVipIp(String vipIp) {
        this.vipIp = vipIp;
    }

    public String getPrivateMac() {
        return privateMac;
    }

    public void setPrivateMac(String privateMac) {
        this.privateMac = privateMac;
    }
}
