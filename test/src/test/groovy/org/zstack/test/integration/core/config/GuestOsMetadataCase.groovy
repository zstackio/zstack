package org.zstack.test.integration.core.config

import org.zstack.core.Platform
import org.zstack.core.config.RefreshGuestOsMetadataMsg
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.managementnode.ManagementNodeInventory
import org.zstack.header.managementnode.ManagementNodeState
import org.zstack.header.managementnode.ManagementNodeVO
import org.zstack.portal.managementnode.ManagementNodeManagerImpl
import org.zstack.sdk.GuestOsCharacterInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class GuestOsMetadataCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void test() {
        env.create {
            testGuestOsMetadataApi()
            testMultiNodeFailure()
        }
    }

    @Override
    void environment() {
        env = env {}
    }

    Closure mockAManagementNode(String mgmtUuid) {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        ManagementNodeVO vo = new ManagementNodeVO(
                hostName: "127.0.0.10",
                // mock a future heartbeat
                heartBeat: new Date(System.currentTimeMillis()).toTimestamp(),
                uuid: mgmtUuid,
                port: 8989,
                state: ManagementNodeState.RUNNING
        )
        dbf.persist(vo)

        // directly call the nodeJoin interface, mocking a canonical event will not work because the listener will check if the
        // event is from current node
        bean(ManagementNodeManagerImpl.class).nodeLifeCycle.nodeJoin(ManagementNodeInventory.valueOf(vo))

        return {
            dbf.removeByPrimaryKey(vo.getUuid(), ManagementNodeVO.class)
            bean(ManagementNodeManagerImpl.class).nodeLifeCycle.nodeLeft(ManagementNodeInventory.valueOf(vo))
        }
    }

    void testMultiNodeFailure() {
        def cleanup1 = mockAManagementNode(Platform.uuid)

        def cleanup2 = notifyWhenReceivedMessage(RefreshGuestOsMetadataMsg.class) { RefreshGuestOsMetadataMsg msg ->
            throw new RuntimeException("Simulate management node failure")
        }

        expect(AssertionError.class) {
            refreshGuestOsMetadata {}
        }

        cleanup2()
        cleanup1()

        refreshGuestOsMetadata {}
        getGuestOsMetadata {}
    }

    void testGuestOsMetadataApi() {
        def metadata1 = getGuestOsMetadata {}

        refreshGuestOsMetadata {}

        def metadata2 = getGuestOsMetadata {}
        for (i in 0..metadata1.size() - 1) {
            GuestOsCharacterInventory m1 = metadata1.get(i)
            GuestOsCharacterInventory m2 = metadata2.get(i)

            assert m1.architecture == m2.architecture
            assert m1.platform == m2.platform
            assert m1.osRelease == m2.osRelease
            assert m1.x2apic == m2.x2apic
            assert m1.hygonTag == m2.hygonTag
            assert m1.cpuModel == m2.cpuModel
            assert m1.nicDriver == m2.nicDriver
        }
    }
}
