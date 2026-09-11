package org.zstack.utils.zsha2;

/**
 * Created by mingjian.deng on 2020/4/2.
 */
public class ZSha2Info {
    private String nodeip;
    private String peerip;
    private String dbvip;
    private String nic;
    private int peerport;
    private boolean isMaster;
    private String execUser;
    private HaAddressFamily ipv4;
    private HaAddressFamily ipv6;

    public static class HaAddressFamily {
        private boolean enabled;
        private String virtualIp;
        private String nodeIp;
        private String peerIp;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getVirtualIp() {
            return virtualIp;
        }

        public void setVirtualIp(String virtualIp) {
            this.virtualIp = virtualIp;
        }

        public String getNodeIp() {
            return nodeIp;
        }

        public void setNodeIp(String nodeIp) {
            this.nodeIp = nodeIp;
        }

        public String getPeerIp() {
            return peerIp;
        }

        public void setPeerIp(String peerIp) {
            this.peerIp = peerIp;
        }
    }

    public String getNodeip() {
        return nodeip;
    }

    public void setNodeip(String nodeip) {
        this.nodeip = nodeip;
    }

    public String getPeerip() {
        return peerip;
    }

    public void setPeerip(String peerip) {
        this.peerip = peerip;
    }

    public String getDbvip() {
        return dbvip;
    }

    public void setDbvip(String dbvip) {
        this.dbvip = dbvip;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public void setMaster(boolean master) {
        isMaster = master;
    }

    public int getPeerport() {return peerport;}

    public void setPeerport(int peerport) {this.peerport = peerport;}

    public String getExecUser() {
        return execUser;
    }

    public void setExecUser(String execUser) {
        this.execUser = execUser;
    }

    public HaAddressFamily getIpv4() {
        return ipv4;
    }

    public void setIpv4(HaAddressFamily ipv4) {
        this.ipv4 = ipv4;
    }

    public HaAddressFamily getIpv6() {
        return ipv6;
    }

    public void setIpv6(HaAddressFamily ipv6) {
        this.ipv6 = ipv6;
    }
}
