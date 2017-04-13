package org.zstack.network.service.vip;

/**
 * Created by xing5 on 2016/11/19.
 */
public class AcquireVipMsg extends ModifyVipAttributesMsg implements VipMessage {
    private boolean createOnBackend = true;

    public boolean isCreateOnBackend() {
        return createOnBackend;
    }

    public void setCreateOnBackend(boolean createOnBackend) {
        this.createOnBackend = createOnBackend;
    }
}
