package org.zstack.kvm;

/**
 */
public interface KVMAddons {
    public static class Channel {
        public static final String NAME = "channel";
        public static final String VR_NAME = "channel_vr";

        private String socketPath;
        private String targetName;

        public String getSocketPath() {
            return socketPath;
        }

        public void setSocketPath(String socketPath) {
            this.socketPath = socketPath;
        }

        public String getTargetName() {
            return targetName;
        }

        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }
    }
}
