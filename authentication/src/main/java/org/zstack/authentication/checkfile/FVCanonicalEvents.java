package org.zstack.authentication.checkfile;

import org.zstack.header.message.NeedJsonSchema;

public class FVCanonicalEvents {
    public static final String FILE_STATUS_CHANGED_PATH = "/file/status/change";

    @NeedJsonSchema
    public static class FileStatusChangedData {
        private String node;
        private String path;
        private String catefory;
        private String restore;

        public String getNode() {
            return node;
        }

        public void  setNode(String node) {
            this.node = node;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getCatefory() {
            return catefory;
        }

        public void setCatefory(String catefory) {
            this.catefory = catefory;
        }

        public String getRestore() {
            return restore;
        }

        public void setRestore(String restore) {
            this.restore = restore;
        }
    }
}
