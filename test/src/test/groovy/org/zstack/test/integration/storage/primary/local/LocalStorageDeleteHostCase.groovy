package org.zstack.test.integration.storage.primary.local


import org.zstack.compute.host.HostGlobalConfig
import org.zstack.core.db.DatabaseFacade
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class LocalStorageDeleteHostCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

    @Override
    void clean() {
        env.delete()
    }

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
            testDeleteLocalStorageHost()
        }
    }

    void testDeleteLocalStorageHost() {
        def host = env.inventoryByName("kvm")
        HostGlobalConfig.DELETION_POLICY.updateValue("Permissive")

        expect(AssertionError.class) {
            deleteHost {
                uuid = host.uuid
            }
        }

        HostGlobalConfig.DELETION_POLICY.updateValue("Force")

        deleteHost {
            uuid = host.uuid
        }
    }
}

