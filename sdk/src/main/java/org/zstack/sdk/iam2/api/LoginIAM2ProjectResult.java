package org.zstack.sdk.iam2.api;

import org.zstack.sdk.SessionInventory;

public class LoginIAM2ProjectResult {
    public SessionInventory session;
    public void setSession(SessionInventory session) {
        this.session = session;
    }
    public SessionInventory getSession() {
        return this.session;
    }

}
