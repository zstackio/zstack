package org.zstack.storage.fusionstor.primary;

import org.zstack.kvm.KVMAgentCommands.IsoTO;

import java.util.List;

/**
 * Created by frank on 8/19/2015.
 */
public class KvmFusionstorIsoTO extends IsoTO {
    public static class MonInfo {
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

    public KvmFusionstorIsoTO() {
    }

    public KvmFusionstorIsoTO(IsoTO other) {
        super(other);
    }

    private List<MonInfo> monInfo;
    private String secretUuid;

    public List<MonInfo> getMonInfo() {
        return monInfo;
    }

    public void setMonInfo(List<MonInfo> monInfo) {
        this.monInfo = monInfo;
    }

    public String getSecretUuid() {
        return secretUuid;
    }

    public void setSecretUuid(String secretUuid) {
        this.secretUuid = secretUuid;
    }
}
