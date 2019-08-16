package org.zstack.authentication.checkfile;

import org.zstack.header.message.NeedJsonSchema;

public class FVCanonicalEvents {
    public static final String FILE_STATUS_CHANGED_PATH = "/file/status/change";

    @NeedJsonSchema
    public static class FileStatusChangedData {
        private String hostUuid;
        private String path;

        public String getNode() {
            return hostUuid;
        }

        public void  setNode(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
