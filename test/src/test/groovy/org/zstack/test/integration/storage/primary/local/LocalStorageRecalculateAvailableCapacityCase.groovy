package org.zstack.test.integration.storage.primary.local

import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStatus
import org.zstack.sdk.CreateDataVolumeAction
import org.zstack.sdk.DeleteDataVolumeAction
import org.zstack.sdk.ExpungeDataVolumeAction
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.ReconnectPrimaryStorageAction
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.primary.local.LocalStorageSystemTags
import org.zstack.test.integration.kvm.Env
import org.zstack.testlib.DiskOfferingSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.KVMHostSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit
import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map

/**
 * Created by lining on 2017/3/3.
 */
class LocalStorageRecalculateAvailableCapacityCase extends SubCase{
    EnvSpec env

    long volumeBitSize = SizeUnit.GIGABYTE.toByte(10)

    @Override
    void setup() {
        spring {
            sftpBackupStorage()
            localStorage()
            virtualRouter()
            securityGroup()
            kvm()
        }
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()

        env.diskOffering {
            name = 'diskOffering'
            diskSize = volumeBitSize
        }

    }

    @Override
    void test() {
        env.create {
            testPSAvailableCapacity()
        }
    }

    /**
     * Test Case: test the effect of creating (deleted) cloud disk on available capacity of primary storage
     *
     * Test action:
     * 1.Build PS, host, VM
     * 2.Create a 10GB cloud disk
     * 3.Check primary storage available capacity
     * 4.Delete cloud disk
     * 5.Check primary storage available capacity
     * 6.Reconnect primary storage
     * 7.Check primary storage available capacity
     * 8.Completely remove the cloud disk
     * 9.Check primary storage available capacity
     */
    void testPSAvailableCapacity(){

        // 1: Build PS, host, VM by default
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        KVMHostSpec kvmHostSpec = env.specByName("kvm")


        // 2: Create a 10GB cloud disk
        String localStorageSystemTag = LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.instantiateTag(
                map(e(LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN, kvmHostSpec.inventory.uuid))
        )
        DiskOfferingSpec diskOfferingSpec = env.specByName("diskOffering")
        CreateDataVolumeAction action = new CreateDataVolumeAction(
                sessionId: Test.currentEnvSpec.session.uuid,
                primaryStorageUuid: primaryStorageSpec.inventory.uuid,
                name: "dataVolume",
                systemTags:[localStorageSystemTag],
                diskOfferingUuid: diskOfferingSpec.inventory.uuid
        )
        CreateDataVolumeAction.Result createDataVolumeActionResult = action.call()
        VolumeInventory volumeInventory = createDataVolumeActionResult.value.inventory
        assert createDataVolumeActionResult.error == null
        assert volumeInventory.uuid != null
        assert volumeInventory.size == volumeBitSize


        // step_3: Check primary storage available capacity
        PrimaryStorageInventory primaryStorageInventory =  queryPrimaryStorage {
                conditions=["uuid=${primaryStorageSpec.inventory.uuid}".toString()]
        }[0]
        assert volumeBitSize == primaryStorageInventory.totalCapacity - primaryStorageInventory.availableCapacity


        // 4.Delete cloud disk
        DeleteDataVolumeAction deleteDataVolumeAction = new DeleteDataVolumeAction(
                uuid: volumeInventory.uuid,
                sessionId: Test.currentEnvSpec.session.uuid,
        )
        DeleteDataVolumeAction.Result deleteDataVolumeActionResult= deleteDataVolumeAction.call()
        assert deleteDataVolumeActionResult.error == null


        // 5.Check primary storage available capacity
        primaryStorageInventory =  queryPrimaryStorage {
                conditions=["uuid=${primaryStorageSpec.inventory.uuid}".toString()]
        }[0]
        assert volumeBitSize == primaryStorageInventory.totalCapacity - primaryStorageInventory.availableCapacity


        // 6.Reconnect primary storage
        ReconnectPrimaryStorageAction reconnectPrimaryStorageAction = new ReconnectPrimaryStorageAction(
                uuid: primaryStorageInventory.uuid,
                sessionId: Test.currentEnvSpec.session.uuid
        )
        ReconnectPrimaryStorageAction.Result reconnectPrimaryStorageActionResult = reconnectPrimaryStorageAction.call()
        assert null == reconnectPrimaryStorageActionResult.error
        assert PrimaryStorageState.Enabled.name() == reconnectPrimaryStorageActionResult.value.inventory.state
        assert PrimaryStorageStatus.Connected.name() == reconnectPrimaryStorageActionResult.value.inventory.status


        // 7.Check primary storage available capacity
        primaryStorageInventory =  queryPrimaryStorage {
                conditions=["uuid=${primaryStorageSpec.inventory.uuid}".toString()]
        }[0]
        assert volumeBitSize == primaryStorageInventory.totalCapacity - primaryStorageInventory.availableCapacity


        // 8.Completely remove the cloud disk
        ExpungeDataVolumeAction expungeDataVolumeAction = new ExpungeDataVolumeAction(
                uuid: createDataVolumeActionResult.value.inventory.uuid,
                sessionId: Test.currentEnvSpec.session.uuid
        )
        ExpungeDataVolumeAction.Result expungeDataVolumeActionResult = expungeDataVolumeAction.call()
        assert null == expungeDataVolumeActionResult.error


        // 9.Check primary storage available capacity
        primaryStorageInventory =  queryPrimaryStorage {
                conditions=["uuid=${primaryStorageSpec.inventory.uuid}".toString()]
        }[0]
        assert primaryStorageInventory.totalCapacity == primaryStorageInventory.availableCapacity
    }

    @Override
    void clean() {
        env.delete()
    }
}
