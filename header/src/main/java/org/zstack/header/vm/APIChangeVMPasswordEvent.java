package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by mingjian.deng on 16/10/18.
 */
public class APIChangeVMPasswordEvent extends APIEvent {
    private String userAccount;
    private String accountPassword;
    private String vmUuid;

    public APIChangeVMPasswordEvent() {
    }

    public APIChangeVMPasswordEvent(String apiId) {
        super(apiId);
    }

    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    public String getAccountPassword() {
        return accountPassword;
    }

    public void setAccountPassword(String accountPassword) {
        this.accountPassword = accountPassword;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }
}
