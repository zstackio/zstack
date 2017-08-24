package org.zstack.test.integration.storage.ceph

import org.zstack.sdk.AddMonToCephPrimaryStorageAction
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by camile on 2017/8/24.
 */
class CephOperationCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
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
            zone {
                name = "zone"
                cluster {
                    name = "test-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "ceph-mon"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }
                    attachPrimaryStorage("ceph-pri")
                    attachL2Network("l2")
                }
                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"
                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                cephPrimaryStorage {
                    name = "ceph-pri"
                    description = "Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    url = "ceph://pri"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777"]
                }


                attachBackupStorage("ceph-bk")
            }

            cephBackupStorage {
                name = "ceph-bk"
                description = "Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "test-iso"
                    url = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image"
                    url = "http://zstack.org/download/image.qcow2"
                }
            }

            vm {
                name = "test-vm"
                useCluster("test-cluster")
                useHost("ceph-mon")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image")

            }
        }
    }

    @Override
    void test() {
        env.create {
            testAddReplaceMon()
        }
    }

    private void testAddReplaceMon() {
        PrimaryStorageSpec primaryStorageSpec = env.specByName("ceph-pri")
        AddMonToCephPrimaryStorageAction action = new AddMonToCephPrimaryStorageAction()
        action.uuid = primaryStorageSpec.inventory.uuid
        action.monUrls = ["root:password@localhost/?monPort=7777"]
        action.sessionId = adminSession()
        assert action.call().error != null
        assert action.call().error.code == "SYS.1007"
        assert action.call().error.description == "One or more API argument is invalid"
    }

    @Override
    void clean() {
        env.delete()
    }
}
