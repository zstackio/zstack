package org.zstack.test.integration.network.sdnController

import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit

/**
 * Created by shixin on 09/26/2019.
 */
class SdnControllerEnv {
    static EnvSpec SdnControllerBasicEnv() {
        return Test.makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

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

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"
                }

                sdnController {
                    vendorType = "H3C VCFC"
                    name = "h3c"
                    ip = "127.0.0.1"
                    userName = "user"
                    password = "password"
                    systemTags = ["vdsUuid::234567"]
                }
            }
        }
    }
}
