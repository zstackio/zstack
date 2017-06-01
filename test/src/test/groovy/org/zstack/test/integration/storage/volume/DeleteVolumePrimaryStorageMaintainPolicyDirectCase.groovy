package org.zstack.test.integration.storage.volume

import org.springframework.http.HttpEntity
import org.zstack.core.config.GlobalConfigVO
import org.zstack.core.config.GlobalConfigVO_
import org.zstack.core.db.Q
import org.zstack.core.gc.GCStatus
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.core.gc.GarbageCollectorVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.volume.VolumeStatus
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import javax.persistence.metamodel.SingularAttribute

/**
 * Created by heathhose on 17-6-1.
 */
class DeleteVolumePrimaryStorageMaintainPolicyDirectCase extends SubCase{
    def DOC = """
1.delete volume when primary storage is maintain and policy is direct
2.save a gc about deleting volume
3. when primary storage state is changed, the gc works.

"""
    EnvSpec env
    def volume
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
                    name = "test-iso"
                    url  = "http://zstack.org/download/test.iso"
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
                        name = "l3_2"

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
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
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDeleteVolumeDirectlyPrimaryStorageMaintain()
            testChangePrimaryStorgeEnableGCWork()
        }
    }

    void testDeleteVolumeDirectlyPrimaryStorageMaintain(){
        def ps = env.inventoryByName("local") as PrimaryStorageInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def diskoffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        volume = createDataVolume {
            name = "test"
            diskOfferingUuid = diskoffering.uuid

        } as VolumeInventory

        KVMAgentCommands.AttachDataVolumeCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME){ rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), KVMAgentCommands.AttachDataVolumeCmd)
            return rsp
        }
        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = volume.uuid
        }
        assert dbFindByUuid(volume.uuid, VolumeVO.class).getStatus() == VolumeStatus.Ready
        changePrimaryStorageState {
            uuid = ps.uuid
            stateEvent = PrimaryStorageStateEvent.maintain.toString()
        }
        assert dbFindByUuid(ps.uuid, PrimaryStorageVO.class).getState() == PrimaryStorageState.Maintenance
        assert cmd.vmUuid == vm.uuid
        assert cmd.volume.getVolumeUuid() == volume.uuid

        LocalStorageKvmBackend.DeleteBitsCmd bitsCmd = null
        env.afterSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH){rsp, HttpEntity<String> entity ->
            bitsCmd = json(entity.getBody(), LocalStorageKvmBackend.DeleteBitsCmd)
            return rsp
        }
        updateGlobalConfig {
            category = "volume"
            name = "deletionPolicy"
            value = "Direct"
        }
        assert Q.New(GlobalConfigVO.class).eq(GlobalConfigVO_.category, "volume")
                .eq(GlobalConfigVO_.name as SingularAttribute, "deletionPolicy")
                .select(GlobalConfigVO_.value).findValue() == "Direct"
        deleteDataVolume {
            uuid = volume.uuid
        }
        assert bitsCmd == null
        GarbageCollectorVO gc = Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.name as SingularAttribute, String.format("gc-volume-%s-on-primary-storage-%s", volume.uuid, ps.getUuid())).find()
        assert gc != null
        assert gc.status == GCStatus.Idle

    }

    void testChangePrimaryStorgeEnableGCWork(){
        def ps = env.inventoryByName("local") as PrimaryStorageInventory
        def host = env.inventoryByName("kvm") as HostInventory
        def status = null
        LocalStorageKvmBackend.DeleteBitsCmd bitsCmd = null
        env.afterSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH){rsp, HttpEntity<String> entity ->
            bitsCmd = json(entity.getBody(), LocalStorageKvmBackend.DeleteBitsCmd)
            return rsp
        }
        changePrimaryStorageState {
            uuid = ps.uuid
            stateEvent = PrimaryStorageStateEvent.enable
        }
        retryInSecs{
            status = Q.New(GarbageCollectorVO.class)
                    .eq(GarbageCollectorVO_.name as SingularAttribute, String.format("gc-volume-%s-on-primary-storage-%s", volume.uuid, ps.getUuid()))
                    .select(GarbageCollectorVO_.status).findValue()
            assert status == GCStatus.Done
            assert bitsCmd != null
            assert bitsCmd.hostUuid == host.uuid
        }

    }

    @Override
    void clean() {
        env.delete()
    }
}
