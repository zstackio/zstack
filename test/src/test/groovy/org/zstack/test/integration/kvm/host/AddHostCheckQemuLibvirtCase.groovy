package org.zstack.test.integration.kvm.host

import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.host.HostVO
import org.zstack.kvm.KVMSystemTags
import org.zstack.sdk.AddKVMHostAction
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map

class AddHostCheckQemuLibvirtCase extends SubCase {
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
    void environment() {
        env = env {
            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster-1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm11"
                        managementIp = "127.0.0.11"
                        username = "root"
                        password = "password"
                        systemTags = [
                                KVMSystemTags.QEMU_IMG_VERSION.instantiateTag(map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, "2.6.0"))),
                                KVMSystemTags.LIBVIRT_VERSION.instantiateTag(map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, "1.3.3")))
                        ]
                    }

                    kvm {
                        name = "kvm12"
                        managementIp = "127.0.0.12"
                        username = "root"
                        password = "password"
                        systemTags = [
                                KVMSystemTags.QEMU_IMG_VERSION.instantiateTag(map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, "2.6.0"))),
                                KVMSystemTags.LIBVIRT_VERSION.instantiateTag(map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, "1.3.3")))
                        ]
                    }
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testAddHostWithDifferentQemuVerion()
            testAddHostWithSameQemuVerion()
        }
    }

    void testAddHostWithDifferentQemuVerion() {
        ClusterInventory cluster = env.inventoryByName("cluster-1") as ClusterInventory

        AddKVMHostAction action = new AddKVMHostAction()
        action.resourceUuid = Platform.getUuid()
        action.sessionId = adminSession()
        action.clusterUuid = cluster.uuid
        action.name = "kvm13"
        action.managementIp = "127.0.0.13"
        action.username = "root"
        action.password = "password"
        action.systemTags = [
                KVMSystemTags.QEMU_IMG_VERSION.instantiateTag(map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, "2.9.0"))),
                KVMSystemTags.LIBVIRT_VERSION.instantiateTag(map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, "1.3.3")))
        ]
        def res = action.call()
        assert res.error != null
    }

    void testAddHostWithSameQemuVerion() {
        ClusterInventory cluster = env.inventoryByName("cluster-1") as ClusterInventory

        KVMHostInventory host = addKVMHost {
            resourceUuid = Platform.getUuid()
            sessionId = adminSession()
            clusterUuid = cluster.uuid
            name = "kvm13"
            managementIp = "127.0.0.13"
            username = "root"
            password = "password"
            systemTags = [
                    KVMSystemTags.QEMU_IMG_VERSION.instantiateTag(map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, "2.6.0"))),
                    KVMSystemTags.LIBVIRT_VERSION.instantiateTag(map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, "1.3.3")))
            ]
        } as KVMHostInventory

        assert 3 == Q.New(HostVO.class).count()

        deleteHost {
            sessionId = adminSession()
            uuid = host.uuid
        }
    }
}
