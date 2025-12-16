package org.zstack.storage.zbs;

import java.util.List;

/**
 * example:
 * {
 *   "error": {
 *     "code": 0,
 *     "message": "success"
 *   },
 *   "result": [
 *     {
 *       "statusCode": 0,
 *       "logicalPoolInfos": [
 *         {
 *           "logicalPoolID": 6,
 *           "logicalPoolName": "pool-4676f61e5c4c456891cc3ec22ef118c3",
 *           "physicalPoolID": 6,
 *           "physicalPoolName": "pool-4676f61e5c4c456891cc3ec22ef118c3_physical",
 *           "type": 0,
 *           "createTime": 1765855781,
 *           "redundanceAndPlaceMentPolicy": "eyJjb3B5c2V0TnVtIjoyMDAsInJlcGxpY2FOdW0iOjMsInpvbmVOdW0iOjN9Cg==",
 *           "userPolicy": "eyJwb2xpY3kiIDogMX0=",
 *           "allocateStatus": 0,
 *           "capacity": 390607142912,
 *           "usedSize": 2617245696,
 *           "allocatedSize": 4294967296,
 *           "rawUsedSize": 7851737088,
 *           "rawWalUsedSize": 533196800,
 *           "quota": 0,
 *           "ioPriority": 0
 *         }
 *       ]
 *     },
 *     {
 *       "statusCode": 0,
 *       "logicalPoolInfos": [
 *         {
 *           "logicalPoolID": 5,
 *           "logicalPoolName": "pool-a65b5d3a96f44dfc95971a4fa8032a4f",
 *           "physicalPoolID": 5,
 *           "physicalPoolName": "pool-a65b5d3a96f44dfc95971a4fa8032a4f_physical",
 *           "type": 0,
 *           "createTime": 1765855755,
 *           "redundanceAndPlaceMentPolicy": "eyJjb3B5c2V0TnVtIjoyMDAsInJlcGxpY2FOdW0iOjMsInpvbmVOdW0iOjN9Cg==",
 *           "userPolicy": "eyJwb2xpY3kiIDogMX0=",
 *           "allocateStatus": 0,
 *           "capacity": 390607142912,
 *           "usedSize": 3162505216,
 *           "allocatedSize": 9663676416,
 *           "rawUsedSize": 9487515648,
 *           "rawWalUsedSize": 608768000,
 *           "quota": 0,
 *           "ioPriority": 0
 *         }
 *       ]
 *     }
 *   ]
 * }
 */


public class ZbsListPoolResult {
    private ErrorInfo error;
    private List<Result> result;

    public ErrorInfo getError() {
        return error;
    }

    public void setError(ErrorInfo error) {
        this.error = error;
    }

    public List<Result> getResult() {
        return result;
    }

    public void setResult(List<Result> result) {
        this.result = result;
    }

    public static class ErrorInfo {
        private int code;
        private String message;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class Result {
        private int statusCode;
        private List<LogicalPoolInfo> logicalPoolInfos;

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public List<LogicalPoolInfo> getLogicalPoolInfos() {
            return logicalPoolInfos;
        }

        public void setLogicalPoolInfos(List<LogicalPoolInfo> logicalPoolInfos) {
            this.logicalPoolInfos = logicalPoolInfos;
        }
    }

    public boolean isSuccess() {
        return error == null || error.getCode() == 0;
    }

    public static class LogicalPoolInfo {
        private int logicalPoolID;
        private String logicalPoolName;
        private int physicalPoolID;
        private String physicalPoolName;
        private int type;
        private long createTime;
        private String redundanceAndPlaceMentPolicy;
        private String userPolicy;
        private int allocateStatus;
        private long capacity;
        private long usedSize;
        private long allocatedSize;
        private long rawUsedSize;
        private long rawWalUsedSize;
        private long quota;
        private int ioPriority;

        public int getLogicalPoolID() {
            return logicalPoolID;
        }

        public void setLogicalPoolID(int logicalPoolID) {
            this.logicalPoolID = logicalPoolID;
        }

        public String getLogicalPoolName() {
            return logicalPoolName;
        }

        public void setLogicalPoolName(String logicalPoolName) {
            this.logicalPoolName = logicalPoolName;
        }

        public int getPhysicalPoolID() {
            return physicalPoolID;
        }

        public void setPhysicalPoolID(int physicalPoolID) {
            this.physicalPoolID = physicalPoolID;
        }


        public String getPhysicalPoolName() {
            return physicalPoolName;
        }

        public void setPhysicalPoolName(String physicalPoolName) {
            this.physicalPoolName = physicalPoolName;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public long getCreateTime() {
            return createTime;
        }

        public void setCreateTime(long createTime) {
            this.createTime = createTime;
        }

        public String getRedundanceAndPlaceMentPolicy() {
            return redundanceAndPlaceMentPolicy;
        }

        public void setRedundanceAndPlaceMentPolicy(String redundanceAndPlaceMentPolicy) {
            this.redundanceAndPlaceMentPolicy = redundanceAndPlaceMentPolicy;
        }

        public String getUserPolicy() {
            return userPolicy;
        }

        public void setUserPolicy(String userPolicy) {
            this.userPolicy = userPolicy;
        }

        public int getAllocateStatus() {
            return allocateStatus;
        }

        public void setAllocateStatus(int allocateStatus) {
            this.allocateStatus = allocateStatus;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public long getUsedSize() {
            return usedSize;
        }

        public void setUsedSize(long usedSize) {
            this.usedSize = usedSize;
        }

        public long getAllocatedSize() {
            return allocatedSize;
        }

        public void setAllocatedSize(long allocatedSize) {
            this.allocatedSize = allocatedSize;
        }

        public long getRawUsedSize() {
            return rawUsedSize;
        }

        public void setRawUsedSize(long rawUsedSize) {
            this.rawUsedSize = rawUsedSize;
        }

        public long getRawWalUsedSize() {
            return rawWalUsedSize;
        }

        public void setRawWalUsedSize(long rawWalUsedSize) {
            this.rawWalUsedSize = rawWalUsedSize;
        }

        public long getQuota() {
            return quota;
        }

        public void setQuota(long quota) {
            this.quota = quota;
        }

        public int getIoPriority() {
            return ioPriority;
        }

        public void setIoPriority(int ioPriority) {
            this.ioPriority = ioPriority;
        }
    }
}
