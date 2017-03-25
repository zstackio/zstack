package org.zstack.test.integration.storage.primary.ceph

import groovy.transform.TypeChecked
import junit.framework.Assert
import org.zstack.header.volume.VolumeConstant
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.simulator.kvm.KVMSimulatorConfig
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by heathhose on 17-3-22.
 */
class CephStorageOneVmAndImage extends SubCase{
    def description = """
        1. use ceph for primary storage and backup storage
        2. create a vm
        3. create an image from the vm's root volume
        confirm the volume created successfully
    """

    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = CephEnv.CephStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            createImageFromRootVolume()
        }
    }

    void createImageFromRootVolume(){
        VmInstanceInventory vm = env.inventoryByName("test-vm")
        stopVmInstance {
            uuid = vm.uuid
            sessionId = loginAsAdmin().uuid
        }

        ImageInventory img = createRootVolumeTemplateFromRootVolume {
            name = "template"
            rootVolumeUuid = vm.getRootVolumeUuid()
            sessionId = loginAsAdmin().uuid
        }

        Assert.assertEquals(VolumeConstant.VOLUME_FORMAT_RAW, img.getFormat());
    }
    
    @Override
    void clean() {
        env.delete()
    }
}