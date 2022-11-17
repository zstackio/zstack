package org.zstack.test.integration.kvm.host

import org.zstack.core.db.Q
import org.zstack.core.encrypt.EncryptGlobalConfig
import org.zstack.kvm.KVMHostVO
import org.zstack.kvm.KVMHostVO_
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by kayo on 2018/9/12.
 */
class HostPasswordEncryptCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
        spring {
            include("encrypt.xml")
        }
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"

                cluster {
                    name = "cluster"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }
                }
            }
        }
    }

    void testHostPasswordEncrypt() {
        def host = env.inventoryByName("kvm1") as HostInventory

        def password = Q.New(KVMHostVO.class).select(KVMHostVO_.password).eq(KVMHostVO_.uuid, host.uuid).findValue()

        assert password == "password"

        updateGlobalConfig {
            category = EncryptGlobalConfig.CATEGORY
            name = EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.name
            value = "LocalEncryption"
        }

        retryInSecs {
            password = Q.New(KVMHostVO.class).select(KVMHostVO_.password).eq(KVMHostVO_.uuid, host.uuid).findValue()

            assert password == "password"
        }

        updateGlobalConfig {
            category = EncryptGlobalConfig.CATEGORY
            name = EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.name
            value = "None"
        }

        retryInSecs {
            password = Q.New(KVMHostVO.class).select(KVMHostVO_.password).eq(KVMHostVO_.uuid, host.uuid).findValue()

            assert password == "password"
        }
    }


    @Override
    void test() {
        env.create {
            testHostPasswordEncrypt()
        }
    }
}
