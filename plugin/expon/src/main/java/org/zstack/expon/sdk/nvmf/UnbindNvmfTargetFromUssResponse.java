package org.zstack.expon.sdk.nvmf;

import org.zstack.expon.sdk.ExponResponse;

public class UnbindNvmfTargetFromUssResponse extends ExponResponse {
    private boolean result;

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }
}
