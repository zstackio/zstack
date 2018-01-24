package org.zstack.storage.surfs.primary;

import org.zstack.kvm.KVMAgentCommands.IsoTO;

import java.util.List;

/**
 * Created by zhouhaiping 2017-09-14
 */
public class KvmSurfsIsoTO extends IsoTO {
    public static class NodeInfo {
        String hostname;
        int port;

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public KvmSurfsIsoTO() {
    }

    public KvmSurfsIsoTO(IsoTO other) {
        super(other);
    }

    private List<NodeInfo> nodeInfo;
    private String secretUuid;

    public List<NodeInfo> getNodeInfo() {
        return nodeInfo;
    }

    public void setNodeInfo(List<NodeInfo> nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    public String getSecretUuid() {
        return secretUuid;
    }

    public void setSecretUuid(String secretUuid) {
        this.secretUuid = secretUuid;
    }
}
