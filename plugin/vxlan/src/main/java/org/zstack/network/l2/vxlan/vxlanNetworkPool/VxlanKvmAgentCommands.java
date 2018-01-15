package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.core.validation.ConditionalValidation;

import java.util.List;

/**
 * Created by weiwang on 10/05/2017.
 */
public class VxlanKvmAgentCommands {

    public static class AgentCommand {
    }

    public static class AgentResponse implements ConditionalValidation {
        private boolean success = true;
        private String error;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        @Override
        public boolean needValidation() {
            return success;
        }
    }

    public static class CreateBridgeResponse extends AgentResponse {
    }

    public static class CreateVxlanBridgeCmd extends AgentCommand {
        private String bridgeName;
        private String vtepIp;
        private Integer vni;
        private String l2NetworkUuid;
        private List<String> peers;

        public String getL2NetworkUuid() {
            return l2NetworkUuid;
        }

        public void setL2NetworkUuid(String l2NetworkUuid) {
            this.l2NetworkUuid = l2NetworkUuid;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }

        public String getVtepIp() {
            return vtepIp;
        }

        public void setVtepIp(String vtepIp) {
            this.vtepIp = vtepIp;
        }

        public Integer getVni() {
            return vni;
        }

        public void setVni(Integer vni) {
            this.vni = vni;
        }

        public List<String> getPeers() {
            return peers;
        }

        public void setPeers(List<String> peers) {
            this.peers = peers;
        }
    }

    public static class CreateVxlanBridgeResponse extends CreateBridgeResponse {
    }

    public static class PopulateVxlanFdbCmd extends AgentCommand {
        private Integer vni;
        private List<String> peers;

        public Integer getVni() {
            return vni;
        }

        public void setVni(Integer vni) {
            this.vni = vni;
        }

        public List<String> getPeers() {
            return peers;
        }

        public void setPeers(List<String> peers) {
            this.peers = peers;
        }
    }

    public static class PopulateVxlanFdbResponse extends AgentResponse {
    }

    public static class PopulateVxlanNetworksFdbCmd extends AgentCommand {
        private List<String> networkUuids;
        private List<String> peers;

        public List<String> getNetworkUuids() {
            return networkUuids;
        }

        public void setNetworkUuids(List<String> networkUuids) {
            this.networkUuids = networkUuids;
        }

        public List<String> getPeers() {
            return peers;
        }

        public void setPeers(List<String> peers) {
            this.peers = peers;
        }
    }

    public static class PopulateVxlanNetworksFdbResponse extends AgentResponse {
    }

    public static class CheckVxlanCidrCmd extends AgentCommand {
        private String cidr;

        public String getPhysicalInterfaceName() {
            return physicalInterfaceName;
        }

        public void setPhysicalInterfaceName(String physicalInterfaceName) {
            this.physicalInterfaceName = physicalInterfaceName;
        }

        private String physicalInterfaceName;

        public String getCidr() {
            return cidr;
        }

        public void setCidr(String cidr) {
            this.cidr = cidr;
        }
    }

    public static class CheckVxlanCidrResponse extends AgentResponse {
        private String vtepIp;

        public String getVtepIp() {
            return vtepIp;
        }

        public void setVtepIp(String vtepIp) {
            this.vtepIp = vtepIp;
        }
    }

    public static class NicTO {
        private String mac;
        private String ip;
        private String bridgeName;
        private String uuid;
        private String nicInternalName;
        private int deviceId;
        private String metaData;
        private Boolean useVirtio;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public Boolean getUseVirtio() {
            return useVirtio;
        }

        public void setUseVirtio(Boolean useVirtio) {
            this.useVirtio = useVirtio;
        }

        public String getMac() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac = mac;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }

        public int getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(int deviceId) {
            this.deviceId = deviceId;
        }

        public String getMetaData() {
            return metaData;
        }

        public void setMetaData(String metaData) {
            this.metaData = metaData;
        }

        public String getNicInternalName() {
            return nicInternalName;
        }

        public void setNicInternalName(String nicInternalName) {
            this.nicInternalName = nicInternalName;
        }

    }
}
