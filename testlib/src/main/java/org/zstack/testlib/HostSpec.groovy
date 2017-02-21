package org.zstack.testlib

import org.zstack.sdk.HostInventory
import org.zstack.utils.data.SizeUnit

/**
 * Created by xing5 on 2017/2/12.
 */
abstract class HostSpec implements Spec {
    String name
    String description
    String managementIp = "127.0.0.1"
    Long totalMem = SizeUnit.GIGABYTE.toByte(32)
    Long usedMem = 0
    Integer totalCpu = 32
    Integer usedCpu = 0

    HostInventory inventory

    HostSpec() {
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteHost {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
