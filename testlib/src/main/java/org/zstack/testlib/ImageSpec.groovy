package org.zstack.testlib

import org.zstack.sdk.ImageInventory
import org.zstack.utils.data.SizeUnit

/**
 * Created by xing5 on 2017/2/15.
 */
class ImageSpec implements Spec, HasSession {
    String name
    String description
    Long size = SizeUnit.GIGABYTE.toByte(10)
    Long actualSize = SizeUnit.GIGABYTE.toByte(1)
    String md5sum
    String url
    String mediaType = "RootVolumeTemplate"
    String platform = "Linux"
    String format = "qcow2"

    ImageInventory inventory

    SpecID create(String uuid, String sessionId) {
        inventory = addImage {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.format = format
            delegate.mediaType = mediaType
            delegate.platform = platform
            delegate.url = url
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.sessionId = sessionId
            delegate.backupStorageUuids = [(parent as BackupStorageSpec).inventory.uuid]
        }

        postCreate {
            inventory = queryImage {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }
}
