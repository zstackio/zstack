package org.zstack.identity;

public class IllegalVmOperationHelper {
    public static String ILLEGAL_VM_OPERATION_PATH = "illegal/vm/operations";

    public static class IllegalVmOperation {
        public String apiName;
        public String userUuid;
        public String accountUuid;
        public String sessionUuid;
    }
}
