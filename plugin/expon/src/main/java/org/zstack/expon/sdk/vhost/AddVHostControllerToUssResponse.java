package org.zstack.expon.sdk.vhost;

import org.zstack.expon.sdk.ExponResponse;

public class AddVHostControllerToUssResponse extends ExponResponse {
    private boolean result;

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }
}
