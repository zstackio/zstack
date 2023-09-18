package org.zstack.header.managementnode;

import org.zstack.header.message.NeedJsonSchema;

    public class MNCanonicalEvents {
    public static final String MN_DB_STATUS_PATH = "/mn/db/status";

    @NeedJsonSchema
    public static class MNDbStatusData {
        private String managementNodeIP;
        private String dbErrorDetail;

        public String getManagementNodeIP() {
            return managementNodeIP;
        }

        public void setManagementNodeIP(String managementNodeIP) {
            this.managementNodeIP = managementNodeIP;
        }

        public String getDbErrorDetail() {
            return dbErrorDetail;
        }

        public void setDbErrorDetail(String dbErrorDetail) {
            this.dbErrorDetail = dbErrorDetail;
        }
    }
}
