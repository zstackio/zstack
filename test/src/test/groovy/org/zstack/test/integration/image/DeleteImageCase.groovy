package org.zstack.test.integration.image

import org.zstack.header.image.ImageEO
import org.zstack.header.image.ImageStatus
import org.zstack.header.image.ImageVO
import org.zstack.sdk.ImageInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017/5/5.
 */
class DeleteImageCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(ImageTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
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

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            runTest()
        }
    }

    void runTest(){
        ImageInventory image = env.inventoryByName("image")

        deleteImage {
            uuid = image.uuid
        }
        ImageVO imageVO = dbFindByUuid(image.uuid,ImageVO.class)
        assert ImageStatus.Deleted == imageVO.status

        expungeImage {
            imageUuid = image.uuid
        }
        assert null == dbFindByUuid(image.uuid,ImageVO.class)
        ImageEO imageEO = dbFindByUuid(image.uuid,ImageEO.class)
        assert null != imageEO.deleted

        deleteImage {
            uuid = image.uuid
        }
        imageEO = dbFindByUuid(image.uuid,ImageEO.class)
        // image used by vm, imageEO can't be cleaned
        assert null != imageEO
        assert null != imageEO.deleted

    }

    @Override
    void clean() {
        env.delete()
    }
}