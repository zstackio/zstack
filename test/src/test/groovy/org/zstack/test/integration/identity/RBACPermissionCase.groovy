package org.zstack.test.integration.identity

import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class RBACPermissionCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {}
    }

    void testAccountPermission() {
        def senv = senv {
            zone {
                name = "zone"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"
                }
            }
        }

        senv.delete()
    }

    @Override
    void test() {
        env.create {
            testAccountPermission()
        }
    }
}
