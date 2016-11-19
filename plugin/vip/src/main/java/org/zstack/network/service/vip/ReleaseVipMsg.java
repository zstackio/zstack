package org.zstack.network.service.vip;

/**
 * Created by xing5 on 2016/11/20.
 */
public class ReleaseVipMsg extends ModifyVipAttributesMsg implements VipMessage {
    private boolean deleteOnBackend;

    public boolean isDeleteOnBackend() {
        return deleteOnBackend;
    }

    public void setDeleteOnBackend(boolean deleteOnBackend) {
        this.deleteOnBackend = deleteOnBackend;
    }

    public ReleaseVipMsg() {
        setServiceProvider(null);
    }
}
