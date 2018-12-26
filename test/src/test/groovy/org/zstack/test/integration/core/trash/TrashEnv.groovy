package org.zstack.test.integration.core.trash


import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit
/**
 * Created by mingjian.deng on 2018/12/18.*/
class TrashEnv {
    static EnvSpec psbs() {
        return Test.makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            cephBackupStorage {
                name="ceph-bs"
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)

                image {
                    name = "ceph-image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }


                    kvm {
                        name = "kvm1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("ceph-ps")
                    attachL2Network("l2")
                }

                cephPrimaryStorage {
                    name = "ceph-ps"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777", "root:password@127.0.0.1/?monPort=7777"]
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

                attachBackupStorage("ceph-bs")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("ceph-image")
                useL3Networks("l3")
                useCluster("cluster")
                useDiskOfferings("diskOffering")
            }
        }
    }
}
