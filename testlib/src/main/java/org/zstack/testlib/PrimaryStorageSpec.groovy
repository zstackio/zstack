package org.zstack.testlib

import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.utils.data.SizeUnit

/**
 * Created by xing5 on 2017/2/13.
 */
abstract class PrimaryStorageSpec implements Spec {
    String name
    String description
    String url
    Long totalCapacity = SizeUnit.TERABYTE.toByte(100)
    Long availableCapacity = SizeUnit.TERABYTE.toByte(100)

    PrimaryStorageInventory inventory
}
