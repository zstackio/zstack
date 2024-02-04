package org.zstack.smStorageEncrypt;

import org.zstack.kvm.KVMAgentCommands;


public class smBackendCommands {
    public static class setSecretCmd extends KVMAgentCommands.AgentCommand {
        public String volumeEncrypt;
        public String volumeUuid;

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public String getKeyValue() {
            return volumeEncrypt;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public void setKeyValue(String volumeEncrypt) {
            this.volumeEncrypt = volumeEncrypt;
        }
    }

    public static class delSecretCmd extends KVMAgentCommands.AgentCommand {
        public String volumeEncrypt;
        public String volumeUuid;

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public String getKeyValue() {
            return volumeEncrypt;
        }

        public void setVolumeUuid(String volumeEncryptKeyId) {
            this.volumeUuid = volumeEncryptKeyId;
        }

        public void setKeyValue(String volumeEncrypt) {
            this.volumeEncrypt = volumeEncrypt;
        }
    }
}
