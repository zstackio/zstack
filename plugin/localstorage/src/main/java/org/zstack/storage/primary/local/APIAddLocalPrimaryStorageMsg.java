package org.zstack.storage.primary.local;

import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;

/**
 * Created by frank on 7/1/2015.
 */
public class APIAddLocalPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
    @Override
    public String getType() {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE;
    }
}
