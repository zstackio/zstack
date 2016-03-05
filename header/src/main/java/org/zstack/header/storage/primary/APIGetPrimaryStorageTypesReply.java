package org.zstack.header.storage.primary;

import org.zstack.header.message.APIReply;

import java.util.List;

/**
 *@apiResult
 *
 * api reply for message :ref:`APIGetPrimaryStorageTypesMsg`
 *
 *@since 0.1.0
 *
 *@example
 *{
"org.zstack.header.storage.primary.APIGetPrimaryStorageTypesReply": {
"primaryStorageTypes": [
"SimulatorPrimaryStorage",
"NFS"
],
"success": true
}
}
 *
 */
public class APIGetPrimaryStorageTypesReply extends APIReply {
    /**
     * @desc
     * a list of primary storage types supported by zstack
     */
    private List<String> primaryStorageTypes;

    public List<String> getPrimaryStorageTypes() {
        return primaryStorageTypes;
    }

    public void setPrimaryStorageTypes(List<String> primaryStorageTypes) {
        this.primaryStorageTypes = primaryStorageTypes;
    }
}
