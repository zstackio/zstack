package org.zstack.header.storage.primary;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * @apiResult api reply for message :ref:`APIGetPrimaryStorageAllocatorStrategiesMsg`
 * @example {
 * "org.zstack.header.storage.primary.APIGetPrimaryStorageAllocatorStrategiesReply": {
 * "primaryStorageAllocatorStrategies": [
 * "DefaultPrimaryStorageAllocationStrategy"
 * ],
 * "success": true
 * }
 * }
 * @since 0.1.0
 */

@RestResponse(fieldsTo = {"strategies=primaryStorageAllocatorStrategies"})
public class APIGetPrimaryStorageAllocatorStrategiesReply extends APIReply {
    /**
     * @desc a list of allocation strategy names supported by zstack
     */
    private List<String> primaryStorageAllocatorStrategies;

    public List<String> getPrimaryStorageAllocatorStrategies() {
        return primaryStorageAllocatorStrategies;
    }

    public void setPrimaryStorageAllocatorStrategies(List<String> primaryStorageAllocatorStrategies) {
        this.primaryStorageAllocatorStrategies = primaryStorageAllocatorStrategies;
    }
}
