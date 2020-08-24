package org.zstack.test.integration.configuration.instanceoffering

import org.zstack.configuration.InstanceOfferingSystemTags
import org.zstack.header.configuration.userconfig.InstanceOfferingAllocateConfig
import org.zstack.header.configuration.userconfig.InstanceOfferingUserConfig
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmCreationStrategy
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.CreateInstanceOfferingAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.ValidateInstanceOfferingUserConfigAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map

/**
 * Created by lining on 2020/8/24.
 */
class InstanceOfferingUserConfigCase extends SubCase{

    EnvSpec env
    InstanceOfferingInventory instanceOfferingCluster1;

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
        env =  env {
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

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "pubL3"

                        service {
                            provider =  provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString()]
                        }

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testCreateInstanceOffering()
            testCreateVm()
        }
    }

    void testCreateInstanceOffering() {
        PrimaryStorageInventory ps = env.inventoryByName("nfs")
        ClusterInventory cluster1 = env.inventoryByName("cluster1")
        ClusterInventory cluster2 = env.inventoryByName("cluster2")

        InstanceOfferingUserConfig userConfig = new InstanceOfferingUserConfig(
                allocate : new InstanceOfferingAllocateConfig(
                        clusterUuid: "errorUuid"
                )
        )
        String configStr = JSONObjectUtil.toJsonString(userConfig)

        expectError {
            createInstanceOffering {
                name = "instanceOffering"
                cpuNum = 1
                memorySize = SizeUnit.GIGABYTE.toByte(1)
                systemTags = [
                        InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG.instantiateTag(map(
                                e(InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG_TOKEN, configStr)
                        ))
                ]
            }
        }

        userConfig = new InstanceOfferingUserConfig(
                allocate : new InstanceOfferingAllocateConfig(
                        clusterUuid: cluster1.uuid
                )
        )
        configStr = JSONObjectUtil.toJsonString(userConfig)
        instanceOfferingCluster1 = createInstanceOffering {
            name = "instanceOffering"
            cpuNum = 1
            memorySize = SizeUnit.GIGABYTE.toByte(1)
            systemTags = [
                    InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG.instantiateTag(map(
                            e(InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG_TOKEN, configStr)
                    ))
            ]
        }
    }

    void testCreateVm() {
        ImageInventory image = env.inventoryByName("image")
        L3NetworkInventory l3 = env.inventoryByName("pubL3")
        ClusterInventory cluster1 = env.inventoryByName("cluster1")

        VmInstanceInventory vmInstanceInventory = createVmInstance {
            name = "newVm1"
            instanceOfferingUuid = instanceOfferingCluster1.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert cluster1.uuid == vmInstanceInventory.clusterUuid
    }
}