package org.zstack.header.securitymachine;

import org.zstack.header.message.NeedJsonSchema;

/**
 * Created by LiangHanYu on 2021/11/18 16:40
 */
public class SecurityMachineCanonicalEvents {
    public static final String SECURITY_MACHINE_STATE_CHANGE_PATH = "security-machine/state/change";

    @NeedJsonSchema
    public static class SecurityMachineStateChangedData {
        private String securityMachineUuid;
        private String oldState;
        private String newState;
        private String reason;

        public String getSecurityMachineUuid() {
            return securityMachineUuid;
        }

        public void setSecurityMachineUuid(String securityMachineUuid) {
            this.securityMachineUuid = securityMachineUuid;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getNewState() {
            return newState;
        }

        public void setNewState(String newState) {
            this.newState = newState;
        }

        public String getOldState() {
            return oldState;
        }

        public void setOldState(String oldState) {
            this.oldState = oldState;
        }
    }
}
