package org.zstack.storage.zbs;

public class ZbsKvmAgentCommands {
    public static class CheckHostStorageConnectionCmd {
        public String hostUuid;
        private String path;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class UpdateHostDependencyCmd {
        public String updatePackages;
        public String zstackRepo;
    }

    public static class VhostResizeCmd {
        public String bdevName;
        public long sizeMib;
        public String controlSock;
    }
}
