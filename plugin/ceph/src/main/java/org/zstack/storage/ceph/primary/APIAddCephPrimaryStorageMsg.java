package org.zstack.storage.ceph.primary;

import org.zstack.header.message.APIParam;
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;
import org.zstack.storage.ceph.CephConstants;

import java.util.List;

/**
 * Created by frank on 7/28/2015.
 */
public class APIAddCephPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
    @APIParam(nonempty = false)
    private List<String> monUrls;

    @Override
    public String getType() {
        return CephConstants.CEPH_PRIMARY_STORAGE_TYPE;
    }

    public List<String> getMonUrls() {
        return monUrls;
    }

    public void setMonUrls(List<String> monUrls) {
        this.monUrls = monUrls;
    }
}
