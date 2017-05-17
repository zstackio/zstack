package org.zstack.test.integration.storage.primary.nfs

import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.volume.VolumeConstant
import org.zstack.header.volume.VolumeType
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.StartVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit

/**
 * Created by Administrator on 2017-05-17.
 */
class MultiNfsVmOperationCase extends SubCase{
    EnvSpec env
    HostInventory host
    PrimaryStorageInventory nfs1, nfs2
    VmInstanceInventory vm


    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
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

                    attachPrimaryStorage("nfs1")
                    attachPrimaryStorage("nfs2")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs1"
                    url = "172.20.0.2:/nfs"
                }

                nfsPrimaryStorage {
                    name = "nfs2"
                    url = "172.20.0.3:/nfs"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

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
                useRootDiskOffering("diskOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory
            nfs1 = env.inventoryByName("nfs1") as PrimaryStorageInventory
            host = env.inventoryByName("kvm") as HostInventory
            testStartVmWhenOneNfs(PrimaryStorageStateEvent.disable, null, true)
            testStartVmWhenOneNfs(PrimaryStorageStateEvent.maintain, null, false)
            testStartVmWhenOneNfs(PrimaryStorageStateEvent.enable, null, true)
            testStartVmWhenOneNfs(PrimaryStorageStateEvent.disable, host.uuid, true)
            testStartVmWhenOneNfs(PrimaryStorageStateEvent.maintain, host.uuid, false)
            testStartVmWhenOneNfs(PrimaryStorageStateEvent.enable, host.uuid, true)
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testStartVmWhenOneNfs(PrimaryStorageStateEvent state, String hostUuid, boolean Expect){
        stopVmInstance {
            uuid = vm.uuid
        }

        String nfsUuid = Q.New(VolumeVO.class)
                .select(VolumeVO_.primaryStorageUuid)
                .eq(VolumeVO_.vmInstanceUuid, vm.uuid)
                .eq(VolumeVO_.type, VolumeType.Root)
                .findValue()


        changePrimaryStorageState {
            uuid = nfsUuid
            stateEvent = state
        }

        StartVmInstanceAction a = new StartVmInstanceAction()
        a.uuid = vm.uuid
        a.sessionId = currentEnvSpec.session.uuid
        if(hostUuid != null){
            a.hostUuid = hostUuid
        }

        if(Expect){
            assert a.call().error == null
        }else {
            assert a.call().error != null
        }


    }

}
