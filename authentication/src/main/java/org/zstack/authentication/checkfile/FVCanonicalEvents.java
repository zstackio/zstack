package org.zstack.authentication.checkfile;

import org.zstack.header.message.NeedJsonSchema;

public class FVCanonicalEvents {
    public static final String FILE_STATUS_CHANGED_PATH = "/file/status/change";

    @NeedJsonSchema
    public static class FileStatusChangedData {
        private String node;
        private String path;
        private String currentDigest;
        private String targetDigest;
        private String category;
        private String restore;
        private String reason;
        private String uuid;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getTargetDigest() {
            return targetDigest;
        }

        public void setTargetDigest(String targetDigest) {
            this.targetDigest = targetDigest;
        }

        public String getCurrentDigest() {
            return currentDigest;
        }

        public void setCurrentDigest(String currentDigest) {
            this.currentDigest = currentDigest;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

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

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getRestore() {
            return restore;
        }

        public void setRestore(String restore) {
            this.restore = restore;
        }
    }
}
