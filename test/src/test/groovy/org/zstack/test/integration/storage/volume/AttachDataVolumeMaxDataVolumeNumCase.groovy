package org.zstack.test.integration.storage.volume

import org.zstack.core.db.Q
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.header.volume.Volume
import org.zstack.header.volume.VolumeType
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by ads6 on 2018/1/12.
 */

class AttachDataVolumeMaxDataVolumeNumCase extends SubCase {
    EnvSpec env
    private static final int MAX_DATA_VOLUME_NUMBER = 24

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            attachMultiDataVolumestoVm()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void attachMultiDataVolumestoVm() {
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory

        for (int i = 0; i < MAX_DATA_VOLUME_NUMBER; i++){
            VolumeInventory dataVolume = createDataVolume {
                name = 'test-vol'
                diskOfferingUuid = diskOffering.uuid
            } as VolumeInventory

            attachDataVolumeToVm {
                vmInstanceUuid = vm.uuid
                volumeUuid = dataVolume.uuid
            }
        }

        assert Q.New(VolumeVO.class).eq(VolumeVO_.type, VolumeType.Data).eq(VolumeVO_.vmInstanceUuid, vm.uuid).count() == 24

        VolumeInventory newDataVol = createDataVolume {
            name = '25thVol'
            diskOfferingUuid = diskOffering.uuid
        } as VolumeInventory

        expect(AssertionError.class) {
            attachDataVolumeToVm {
                vmInstanceUuid = vm.uuid
                volumeUuid = newDataVol.uuid
            }
        }

        assert Q.New(VolumeVO.class).eq(VolumeVO_.type, VolumeType.Data).eq(VolumeVO_.vmInstanceUuid, vm.uuid).count() == 24
    }
}
