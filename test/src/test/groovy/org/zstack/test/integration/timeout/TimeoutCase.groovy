package org.zstack.test.integration.timeout

import org.zstack.core.timeout.ApiTimeout
import org.zstack.core.timeout.ApiTimeoutManager
import org.zstack.header.image.APIAddImageMsg
import org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeMsg
import org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg
import org.zstack.header.image.APICreateRootVolumeTemplateFromVolumeSnapshotMsg
import org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg
import org.zstack.header.vm.APICreateVmInstanceMsg
import org.zstack.header.vm.APIExpungeVmInstanceMsg
import org.zstack.header.vm.APIMigrateVmMsg
import org.zstack.header.volume.APICreateDataVolumeFromVolumeSnapshotMsg
import org.zstack.header.volume.APICreateDataVolumeFromVolumeTemplateMsg
import org.zstack.header.volume.APICreateVolumeSnapshotMsg
import org.zstack.network.service.virtualrouter.APICreateVirtualRouterVmMsg
import org.zstack.storage.primary.local.APILocalStorageMigrateVolumeMsg
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.SubCase
/**
 * Created by mingjian.deng on 2017/12/28.
 */
class TimeoutCase extends SubCase {
    ApiTimeoutManager timeoutMgr

    @Override
    void clean() {
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        timeoutMgr = bean(ApiTimeoutManager.class)
        testTimeout()
    }

    void testTimeout() {
        for(Map.Entry<Class, ApiTimeout> tt: timeoutMgr.getAllTimeout().entrySet()) {
            logger.debug("class: " + tt.key + ", timeout: " + tt.value.timeout)
        }

        def ignore = new ArrayList<Class>()
        for(Map.Entry<Class, ApiTimeout> tt: timeoutMgr.getAllTimeout().entrySet()) {
            switch (tt.key) {
                case APICreateRootVolumeTemplateFromRootVolumeMsg.class:
                    assert 24 * 60 * 60 * 1000L == tt.value.timeout
                    break
                case APICreateDataVolumeTemplateFromVolumeMsg.class:
                    assert 24 * 60 * 60 * 1000L == tt.value.timeout
                    break
                case APICreateVolumeSnapshotMsg.class:
                    assert 3 * 60 * 60 * 1000L == tt.value.timeout
                    break
                case APICreateRootVolumeTemplateFromVolumeSnapshotMsg.class:
                    assert 3 * 60 * 60 * 1000L == tt.value.timeout
                    break
                case APICreateDataVolumeFromVolumeSnapshotMsg.class:
                    assert 3 * 60 * 60 * 1000L == tt.value.timeout
                    break
                case APILocalStorageMigrateVolumeMsg.class:
                    assert 24 * 60 * 60 * 1000L == tt.value.timeout
                    break
                case APIDeleteVolumeSnapshotMsg.class:
                    assert 3 * 60 * 60 * 1000L == tt.value.timeout
                    break
                case APIExpungeVmInstanceMsg.class:
                    assert 3 * 60 * 60 * 1000L == tt.value.timeout
                    break
                case APICreateVirtualRouterVmMsg.class:
                    assert 3 * 60 * 60 * 1000L == tt.value.timeout
                    break
                case APIAddImageMsg.class:
                    assert 3 * 60 * 60 * 1000L == tt.value.timeout
                    break
                case APICreateVmInstanceMsg.class:
                    assert 3 * 60 * 60 * 1000L == tt.value.timeout
                    break
                case APIMigrateVmMsg.class:
                    assert 1 * 60 * 60 * 1000L == tt.value.timeout
                    break
                case APICreateDataVolumeFromVolumeTemplateMsg.class:
                    assert 3 * 60 * 60 * 1000L == tt.value.timeout
                    break
                default:
                    ignore.add(tt.key)
            }
        }
        if (ignore.size() > 0) {
            logger.debug("the bellow is ignored: ")
            ignore.forEach({i -> logger.debug(i.simpleName)
            })
        }
        assert ignore.size() == 0
    }
}
