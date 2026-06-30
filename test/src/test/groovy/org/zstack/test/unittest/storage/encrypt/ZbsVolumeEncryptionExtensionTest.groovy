package org.zstack.test.unittest.storage.encrypt

import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory
import org.zstack.header.volume.VolumeVO
import org.zstack.storage.encrypt.VolumeSnapshotEncryptionHelper
import org.zstack.storage.encrypt.ZbsVolumeEncryptionExtension

import java.lang.reflect.Field

class ZbsVolumeEncryptionExtensionTest {
    private ZbsVolumeEncryptionExtension extension
    private VolumeSnapshotEncryptionHelper snapshotEncryptionHelper

    @Before
    void setUp() {
        extension = new ZbsVolumeEncryptionExtension()
        snapshotEncryptionHelper = Mockito.mock(VolumeSnapshotEncryptionHelper.class)
        setField("snapshotEncryptionHelper", snapshotEncryptionHelper)
    }

    @Test
    void testSnapshotHooksDelegateForEncryptedVolume() {
        VolumeVO volume = new VolumeVO()
        volume.uuid = "volume-uuid"
        volume.encrypted = true

        VolumeSnapshotInventory snapshot = new VolumeSnapshotInventory()
        snapshot.uuid = "snapshot-uuid"

        extension.beforeTakeSnapshot("ps-uuid", volume, snapshot)
        extension.afterTakeSnapshot("ps-uuid", volume, snapshot)

        Mockito.verify(snapshotEncryptionHelper).inheritVolumeKeyToSnapshot(volume, snapshot)
        Mockito.verify(snapshotEncryptionHelper).completeTakeSnapshot(volume, snapshot)
    }

    @Test
    void testSnapshotHooksSkipPlainVolume() {
        VolumeVO volume = new VolumeVO()
        volume.uuid = "volume-uuid"
        volume.encrypted = false

        VolumeSnapshotInventory snapshot = new VolumeSnapshotInventory()
        snapshot.uuid = "snapshot-uuid"

        extension.beforeTakeSnapshot("ps-uuid", volume, snapshot)
        extension.afterTakeSnapshot("ps-uuid", volume, snapshot)

        Mockito.verify(snapshotEncryptionHelper, Mockito.never()).inheritVolumeKeyToSnapshot(volume, snapshot)
        Mockito.verify(snapshotEncryptionHelper, Mockito.never()).completeTakeSnapshot(volume, snapshot)
    }

    private void setField(String name, Object value) {
        Field field = ZbsVolumeEncryptionExtension.class.getDeclaredField(name)
        field.accessible = true
        field.set(extension, value)
    }
}
