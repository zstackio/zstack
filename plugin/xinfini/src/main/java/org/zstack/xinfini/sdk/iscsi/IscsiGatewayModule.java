package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.BaseSpec;
import org.zstack.xinfini.sdk.BaseStatus;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:29 2024/5/28
 */
public class IscsiGatewayModule extends BaseResource {
    public IscsiGatewayModule(Metadata md, IscsiGatewaySpec spec, IscsiGatewayStatus status) {
        this.spec = spec;
        this.status = status;
        this.setMetadata(md);
    }

    private IscsiGatewaySpec spec;
    private IscsiGatewayStatus status;

    public IscsiGatewaySpec getSpec() {
        return spec;
    }

    public void setSpec(IscsiGatewaySpec spec) {
        this.spec = spec;
    }

    public IscsiGatewayStatus getStatus() {
        return status;
    }

    public void setStatus(IscsiGatewayStatus status) {
        this.status = status;
    }

    public static class IscsiGatewayStatus extends BaseStatus {
        private String configContent;
        private String containerId;
        private long pid;
        private String setupContent;
        private int iscsiClientNum;
        private int iscsiClientGroupNum;
        private int iscsiPathNum;

        public String getConfigContent() {
            return configContent;
        }

        public void setConfigContent(String configContent) {
            this.configContent = configContent;
        }

        public String getContainerId() {
            return containerId;
        }

        public void setContainerId(String containerId) {
            this.containerId = containerId;
        }

        public long getPid() {
            return pid;
        }

        public void setPid(long pid) {
            this.pid = pid;
        }

        public String getSetupContent() {
            return setupContent;
        }

        public void setSetupContent(String setupContent) {
            this.setupContent = setupContent;
        }

        public int getIscsiClientNum() {
            return iscsiClientNum;
        }

        public void setIscsiClientNum(int iscsiClientNum) {
            this.iscsiClientNum = iscsiClientNum;
        }

        public int getIscsiClientGroupNum() {
            return iscsiClientGroupNum;
        }

        public void setIscsiClientGroupNum(int iscsiClientGroupNum) {
            this.iscsiClientGroupNum = iscsiClientGroupNum;
        }

        public int getIscsiPathNum() {
            return iscsiPathNum;
        }

        public void setIscsiPathNum(int iscsiPathNum) {
            this.iscsiPathNum = iscsiPathNum;
        }
    }

    public static class IscsiGatewaySpec extends BaseSpec {

        private int nodeId;
        private List<String> ips;
        private int port;
        private int adminPort;
        private int metricPort;
        private String configContent;
        private String setupContent;
        private int serviceId;
        private int ioRetryCount;

        public int getNodeId() {
            return nodeId;
        }

        public void setNodeId(int nodeId) {
            this.nodeId = nodeId;
        }

        public List<String> getIps() {
            return ips;
        }

        public void setIps(List<String> ips) {
            this.ips = ips;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getAdminPort() {
            return adminPort;
        }

        public void setAdminPort(int adminPort) {
            this.adminPort = adminPort;
        }

        public int getMetricPort() {
            return metricPort;
        }

        public void setMetricPort(int metricPort) {
            this.metricPort = metricPort;
        }

        public String getConfigContent() {
            return configContent;
        }

        public void setConfigContent(String configContent) {
            this.configContent = configContent;
        }

        public String getSetupContent() {
            return setupContent;
        }

        public void setSetupContent(String setupContent) {
            this.setupContent = setupContent;
        }

        public int getServiceId() {
            return serviceId;
        }

        public void setServiceId(int serviceId) {
            this.serviceId = serviceId;
        }

        public int getIoRetryCount() {
            return ioRetryCount;
        }

        public void setIoRetryCount(int ioRetryCount) {
            this.ioRetryCount = ioRetryCount;
        }
    }


}
