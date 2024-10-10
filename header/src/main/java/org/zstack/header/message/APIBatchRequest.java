package org.zstack.header.message;

import org.zstack.header.errorcode.ErrorCode;

public interface APIBatchRequest {
    class BatchOperationResult {
        private String uuid;
        private boolean success;
        private ErrorCode error;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public ErrorCode getError() {
            return error;
        }

        public void setError(ErrorCode error) {
            this.error = error;
        }
    }

    class Result {
        private int totalCount;
        private int successCount;

        public Result() {
        }

        public Result(int totalCount, int successCount) {
            this.totalCount = totalCount;
            this.successCount = successCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }
    }

    APIBatchRequest.Result collectResult(APIMessage message, APIEvent rsp);
}
