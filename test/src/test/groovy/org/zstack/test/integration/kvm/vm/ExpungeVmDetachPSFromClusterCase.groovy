package org.zstack.test.integration.kvm.vm

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ExpungeVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by Administrator on 2017-04-20.
 */

class ExpungeVmDetachPSFromClusterCase extends SubCase {
    EnvSpec env

    def DOC = """
test a VM's start/stop/reboot/destroy/recover operations 
"""

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv{
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
                    name = "image-sftp"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            cephBackupStorage {
                name="ceph-bk"
                description="Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "image-ceph"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster-local"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm-local"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster-smp"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm-smp"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("smp")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster-nfs"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm-nfs"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster-ceph"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm-ceph"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("ceph")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs_root"
                }

                smpPrimaryStorage {
                    name = "smp"
                    url = "/mnt/nfs"
                }

                cephPrimaryStorage {
                    name = "ceph"
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
                attachBackupStorage("ceph-bk")
            }

            vm {
                name = "vm-local"
                useInstanceOffering("instanceOffering")
                useImage("image-sftp")
                useL3Networks("l3")
                useCluster("cluster-local")
            }
            vm {
                name = "vm-smp"
                useInstanceOffering("instanceOffering")
                useImage("image-sftp")
                useL3Networks("l3")
                useCluster("cluster-smp")
            }
            vm {
                name = "vm-nfs"
                useInstanceOffering("instanceOffering")
                useImage("image-sftp")
                useL3Networks("l3")
                useCluster("cluster-nfs")
            }
            vm {
                name = "vm-ceph"
                useInstanceOffering("instanceOffering")
                useImage("image-ceph")
                useL3Networks("l3")
                useCluster("cluster-ceph")
            }

        }
    }

    @Override
    void test() {
        env.create {
            TestExpungeVmInLocal()
            TestExpungeVmInSMP()
            TestExpungeVmInNFS()
            TestExpungeVmInCeph()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void TestExpungeVmInLocal(){
        VmInstanceInventory vmInv = env.inventoryByName("vm-local") as VmInstanceInventory
        PrimaryStorageInventory psInv = env.inventoryByName("local") as PrimaryStorageInventory

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = psInv.uuid
            clusterUuid = psInv.getAttachedClusterUuids().get(0)
            sessionId = currentEnvSpec.session.uuid
        }

        destroyVmInstance {
            uuid = vmInv.uuid
            sessionId = currentEnvSpec.session.uuid
        }

        expungeVmInstance {
            uuid = vmInv.uuid
            sessionId = currentEnvSpec.session.uuid
        }



    }

    void TestExpungeVmInSMP(){
        VmInstanceInventory vmInv = env.inventoryByName("vm-smp") as VmInstanceInventory
        PrimaryStorageInventory psInv = env.inventoryByName("smp") as PrimaryStorageInventory

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = psInv.uuid
            clusterUuid = psInv.getAttachedClusterUuids().get(0)
            sessionId = currentEnvSpec.session.uuid
        }

        destroyVmInstance {
            uuid = vmInv.uuid
            sessionId = currentEnvSpec.session.uuid
        }

        ExpungeVmInstanceAction a = new ExpungeVmInstanceAction()
        a.uuid = vmInv.uuid
        a.sessionId = currentEnvSpec.session.uuid
        assert  a.call().error != null
    }

    void TestExpungeVmInNFS(){
        VmInstanceInventory vmInv = env.inventoryByName("vm-nfs") as VmInstanceInventory
        PrimaryStorageInventory psInv = env.inventoryByName("nfs") as PrimaryStorageInventory

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = psInv.uuid
            clusterUuid = psInv.getAttachedClusterUuids().get(0)
            sessionId = currentEnvSpec.session.uuid
        }

        destroyVmInstance {
            uuid = vmInv.uuid
            sessionId = currentEnvSpec.session.uuid
        }

        ExpungeVmInstanceAction a = new ExpungeVmInstanceAction()
        a.uuid = vmInv.uuid
        a.sessionId = currentEnvSpec.session.uuid
        assert  a.call().error != null
    }

    void TestExpungeVmInCeph(){
        VmInstanceInventory vmInv = env.inventoryByName("vm-ceph") as VmInstanceInventory
        PrimaryStorageInventory psInv = env.inventoryByName("ceph") as PrimaryStorageInventory

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = psInv.uuid
            clusterUuid = psInv.getAttachedClusterUuids().get(0)
            sessionId = currentEnvSpec.session.uuid
        }

        destroyVmInstance {
            uuid = vmInv.uuid
            sessionId = currentEnvSpec.session.uuid
        }

        expungeVmInstance {
            uuid = vmInv.uuid
            sessionId = currentEnvSpec.session.uuid
        }
    }
}