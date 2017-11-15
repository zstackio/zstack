package org.zstack.test.integration.kvm.vm.migrate

import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by camile on 2017-11-14.
 */
class MigrateTwiceHostCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1-1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.6"
                        username = "root"
                        password = "password"
                    }

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

                    attachPrimaryStorage("local")
                    attachL2Network("l2-1")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-1"

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3-1")
                useCluster("cluster1-1")
                useHost("kvm")
            }
        }
    }

    @Override
    void test() {
        env.create {
            //lastHostUuid must be the host where the last VM stopped, see more ZSTAC-7930
            testLastHostUuid()
        }
    }

    void testLastHostUuid(){
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def host = env.inventoryByName("kvm") as HostInventory
        def host1 = env.inventoryByName("kvm1") as HostInventory
        def host2 = env.inventoryByName("kvm2") as HostInventory

        stopVmInstance {
            uuid = vm.uuid
        }

        localStorageMigrateVolume {
            volumeUuid = vm.rootVolumeUuid
            destHostUuid = host1.uuid
        }

        localStorageMigrateVolume {
            volumeUuid = vm.rootVolumeUuid
            destHostUuid = host2.uuid
        }

        VmInstanceVO vmvo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vmvo.hostUuid == null
        assert vmvo.lastHostUuid == host.uuid

    }

    @Override
    void clean() {
        env.delete()
    }
}
