package org.zstack.test.integration.storage.primary.multipleprimarystorageinzone

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import static java.util.Arrays.asList

/**
 * Created by Camile on 2017/6
 */
class CreateVmOnTwoPSInOneClusterCase extends SubCase{
    EnvSpec env


    @Override
    void clean() {
        env.delete()
    }

    static SpringSpec springSpec = makeSpring {
        localStorage()
        nfsPrimaryStorage()
        sftpBackupStorage()
        smp()
        ceph()
        virtualRouter()
        vyos()
        kvm()
        securityGroup()
        flatNetwork()
        portForwarding()
        eip()
        lb()
    }

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering-1G"
                diskSize = SizeUnit.GIGABYTE.toByte(1)
            }

            diskOffering {
                name = "diskOffering-89G"
                diskSize = SizeUnit.GIGABYTE.toByte(89)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

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
                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(178)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(178)
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "172.20.0.3:/nfs"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(81)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(81)
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-1"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "l3-2"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.200.10"
                            endIp = "192.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.200.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }

        }
    }

    @Override
    void test() {
        env.create {
            testCreateVmSuccess()
        }
    }

    void testCreateVmSuccess() {
        BackupStorageInventory bs = env.inventoryByName("sftp")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3-1")
        DiskOfferingInventory _1G = env.inventoryByName("diskOffering-1G")
        DiskOfferingInventory _89G = env.inventoryByName("diskOffering-89G")
        ImageInventory image =  env.inventoryByName("image1")
        PrimaryStorageInventory local_ps = env.inventoryByName("local")
        ClusterInventory cluster1 = env.inventoryByName("cluster")

        CreateVmInstanceAction createVmInstanceAction = new CreateVmInstanceAction()
        createVmInstanceAction.name="test"
        createVmInstanceAction.instanceOfferingUuid = instanceOffering.uuid
        createVmInstanceAction.imageUuid = image.uuid
        createVmInstanceAction.l3NetworkUuids = asList(l3.uuid)
        createVmInstanceAction.rootDiskOfferingUuid = _1G.uuid
        createVmInstanceAction.dataDiskOfferingUuids = asList(_89G.uuid)
        createVmInstanceAction.primaryStorageUuidForRootVolume = local_ps.uuid
        createVmInstanceAction.clusterUuid = cluster1.uuid
        createVmInstanceAction.sessionId = adminSession()

        assert createVmInstanceAction.call().error == null
    }
}
