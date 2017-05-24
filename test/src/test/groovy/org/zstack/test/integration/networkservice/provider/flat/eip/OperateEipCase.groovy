package org.zstack.test.integration.networkservice.provider.flat.eip

import org.springframework.http.HttpEntity
import org.zstack.header.host.HostState
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.kvm.KVMConstant
import org.zstack.network.service.eip.EipBackend
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.eip.EipVO
import org.zstack.network.service.flat.FlatEipBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.EipInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by heathhose on 17-5-15.
 */
class OperateEipCase extends SubCase{

    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
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
                    url  = "http://zstack.org/download/test.qcow2"
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

                attachBackupStorage("sftp")

                eip {
                    name = "eip"

                    useVip("pubL3")
                }
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testAttachVmNicToEip()
            testReconnectHostBatchApplyEips()
            testDeleteEip()
        }
    }

    void testAttachVmNicToEip(){
        def eip = env.inventoryByName("eip") as EipInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        FlatEipBackend.ApplyEipCmd cmd = new FlatEipBackend.ApplyEipCmd()
        env.afterSimulator(FlatEipBackend.APPLY_EIP_PATH){ rsp,HttpEntity<String> entity ->
            cmd = json(entity.getBody(),FlatEipBackend.ApplyEipCmd.class)
            return rsp

        }
        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.getVmNics().get(0).getUuid()
        }

        assert cmd.eip.eipUuid == eip.uuid
        assert dbFindByUuid(eip.uuid,EipVO.class).guestIp == vm.getVmNics().get(0).getIp()
    }



    void testDeleteEip(){
        def eip = env.inventoryByName("eip") as EipInventory
        FlatEipBackend.DeleteEipCmd cmd = new FlatEipBackend.DeleteEipCmd()
        env.afterSimulator(FlatEipBackend.DELETE_EIP_PATH){ rsp,HttpEntity<String> entity ->
            cmd = json(entity.getBody(),FlatEipBackend.DeleteEipCmd.class)
            return rsp
        }

        deleteEip {
            uuid = eip.uuid
        }

        assert cmd.eip.eipUuid == eip.uuid
        assert dbFindByUuid(eip.uuid,EipVO.class) == null
    }

    void testReconnectHostBatchApplyEips(){
        def host = env.inventoryByName("kvm") as HostInventory
        def eip = env.inventoryByName("eip") as EipInventory
        FlatEipBackend.BatchApplyEipCmd cmd = new FlatEipBackend.BatchApplyEipCmd()

        env.afterSimulator(FlatEipBackend.BATCH_APPLY_EIP_PATH){rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), FlatEipBackend.BatchApplyEipCmd)
            return rsp
        }
        reconnectHost {
            uuid = host.uuid
        }

        assert cmd.eips.size() == 1
        assert cmd.eips.get(0).eipUuid == eip.uuid
        assert dbFindByUuid(host.uuid, HostVO.class).getStatus() == HostStatus.Connected
    }
    
    @Override
    void clean() {
       env.delete()
    }
}
