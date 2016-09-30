package org.zstack.storage.fusionstor.primary;

import org.zstack.kvm.KVMAgentCommands.VolumeTO;

import java.util.List;

/**
 * Created by frank on 7/30/2015.
 */
public class KVMFusionstorVolumeTO extends VolumeTO {
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

    public KVMFusionstorVolumeTO() {
    }

    public KVMFusionstorVolumeTO(VolumeTO other) {
        super(other);
    }

    private List<MonInfo> monInfo;
    private String secretUuid;

    public String getSecretUuid() {
        return secretUuid;
    }

    public void setSecretUuid(String secretUuid) {
        this.secretUuid = secretUuid;
    }

    public List<MonInfo> getMonInfo() {
        return monInfo;
    }

    public void setMonInfo(List<MonInfo> monInfo) {
        this.monInfo = monInfo;
    }
}
