package org.zstack.core.progress;

/**
 * Created by mingjian.deng on 16/12/10.
 */
public class ProgressCommands {
    public static class AgentResponse {
        private boolean success = true;
        private String error;
        public boolean isSuccess() {
            return success;
        }
        public void setSuccess(boolean success) {
            this.success = success;
        }
        public String getError() {
            return error;
        }
        public void setError(String error) {
            this.error = error;
        }
    }

    public static class ProgressReportCmd {
        private String serverUuid;
        private String serverType;
        private String resourceUuid;
        private String progress;
        private String processType;

        public String getServerUuid() {
            return serverUuid;
        }

        public void setServerUuid(String serverUuid) {
            this.serverUuid = serverUuid;
        }

        public String getServerType() {
            return serverType;
        }

        public void setServerType(String serverType) {
            this.serverType = serverType;
        }

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }

        public String getProgress() {
            return progress;
        }

        public void setProgress(String progress) {
            this.progress = progress;
        }

        public String getProcessType() {
            return processType;
        }

        public void setProcessType(String processType) {
            this.processType = processType;
        }
    }

    public static class ProgressReportResponse extends AgentResponse {
    }
}
