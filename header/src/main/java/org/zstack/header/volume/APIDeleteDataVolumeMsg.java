package org.zstack.header.volume;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;

/**
 * @api delete a data volume
 * @category volume
 * @cli
 * @httpMsg {
 * "org.zstack.header.volume.APIDeleteDataVolumeMsg": {
 * "uuid": "5bfd7ec8e90d498495cbc533cdd9fd5b",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "950f72f78acd4b1fb79dd9f831e5f6d7"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.volume.APIDeleteDataVolumeMsg": {
 * "uuid": "5bfd7ec8e90d498495cbc533cdd9fd5b",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "950f72f78acd4b1fb79dd9f831e5f6d7"
 * },
 * "timeout": 1800000,
 * "id": "ab12310f65a74fa3b56770e147a315e9",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIDeleteDataVolumeEvent`
 * @since 0.1.0
 */
@Action(category = VolumeConstant.ACTION_CATEGORY)
public class APIDeleteDataVolumeMsg extends APIDeleteMessage implements VolumeMessage {
    /**
     * @desc data volume uuid
     */
    @APIParam(checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }
}
