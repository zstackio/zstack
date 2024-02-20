package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.core.upgrade.GrayVersion;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands;

import java.util.List;

public class VyosKeepalivedCommands {
    public static String VYOS_HA_ENABLE_PATH = "/enableVyosha";
    public static String SYNC_VPC_ROUTER_HA_PATH = "/syncVpcRouterHa";
    public static String RESTART_KEEPALIVED_PATH = "/restartKeepalived";

    static public class VyosHaVip{
        @GrayVersion(value = "5.0.0")
        public String nicMac;
        @GrayVersion(value = "5.0.0")
        public String nicVip;
        @GrayVersion(value = "5.0.0")
        public String netmask;
        @GrayVersion(value = "5.0.0")
        public String category;
        @GrayVersion(value = "5.1.0")
        public Integer prefixLen;
    }

    public static class VyosHaEnableCmd extends VirtualRouterCommands.AgentCommand {
        @GrayVersion(value = "5.0.0")
        public Integer keepalive;
        @GrayVersion(value = "5.0.0")
        public String heartbeatNic;
        @GrayVersion(value = "5.0.0")
        public String peerIp;
        @GrayVersion(value = "5.1.0")
        public String peerIpV6;
        @GrayVersion(value = "5.0.0")
        public String localIp;
        @GrayVersion(value = "5.1.0")
        public String localIpV6;
        @GrayVersion(value = "5.0.0")
        public List<String> monitors;
        @GrayVersion(value = "5.0.0")
        public List<VyosHaVip> vips;
        @GrayVersion(value = "5.0.0")
        public String callbackUrl;

        public Integer getKeepalive() {
            return keepalive;
        }

        public void setKeepalive(Integer keepalive) {
            this.keepalive = keepalive;
        }

        public String getHeartbeatNic() {
            return heartbeatNic;
        }

        public void setHeartbeatNic(String heartbeatNic) {
            this.heartbeatNic = heartbeatNic;
        }

        public String getPeerIp() {
            return peerIp;
        }

        public void setPeerIp(String peerIp) {
            this.peerIp = peerIp;
        }

        public String getPeerIpV6 () {
            return peerIpV6;
        }

        public void setPeerIpV6(String peerIpV6) {
            this.peerIpV6 = peerIpV6;
        }

        public List<String> getMonitors() {
            return monitors;
        }

        public void setMonitors(List<String> monitors) {
            this.monitors = monitors;
        }

        public List<VyosHaVip> getVips() {
            return vips;
        }

        public void setVips(List<VyosHaVip> vips) {
            this.vips = vips;
        }

        public String getLocalIp() {
            return localIp;
        }

        public void setLocalIp(String localIp) {
            this.localIp = localIp;
        }

        public String getLocalIpV6() {
            return localIpV6;
        }

        public void setLocalIpV6(String localIpV6) {
            this.localIpV6 = localIpV6;
        }

        public String getCallbackUrl() {
            return callbackUrl;
        }

        public void setCallbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
        }
    }

    public static class VpcRouterHaStatusCmd {
        public String virtualRouterUuid;
        public String haStatus;
    }

    public static class SyncVpcRouterHaCmd extends VirtualRouterCommands.AgentCommand {
    }

    public static class RestartKeepalivedCmd extends VirtualRouterCommands.AgentCommand {
    }

    public static class VyosHaEnableRsp extends VirtualRouterCommands.AgentResponse {
    }

    public static class SyncVpcRouterHaRsp extends VirtualRouterCommands.AgentResponse {
        @GrayVersion(value = "5.0.0")
        private String haStatus;

        public String getHaStatus() {
            return haStatus;
        }

        public void setHaStatus(String haStatus) {
            this.haStatus = haStatus;
        }
    }

    public static class RestartKeepalivedRsp extends VirtualRouterCommands.AgentResponse {
    }
}
