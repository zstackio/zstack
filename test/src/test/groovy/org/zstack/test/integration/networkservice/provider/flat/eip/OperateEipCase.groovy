package org.zstack.test.integration.networkservice.provider.flat.eip

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.eip.EipVO
import org.zstack.network.service.flat.FlatEipBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.vip.VipVO
import org.zstack.network.service.vip.VipVO_
import org.zstack.sdk.EipInventory
import org.zstack.sdk.GetEipAttachableVmNicsAction
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by heathhose on 17-5-15.
 */
class OperateEipCase extends SubCase {
    EipInventory eip
    VmInstanceInventory vm
    L3NetworkInventory pub_l3
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
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
            eip = env.inventoryByName("eip") as EipInventory
            vm = env.inventoryByName("vm") as VmInstanceInventory
            pub_l3 = env.inventoryByName("pubL3") as L3NetworkInventory

            testAttachVmNicToEip()
            testDetachEipWhenVmStopped()
            testReconnectHostBatchApplyEips()
            testDeleteEip()
        }
    }

    void testAttachVmNicToEip() {
        FlatEipBackend.ApplyEipCmd cmd = new FlatEipBackend.ApplyEipCmd()
        env.afterSimulator(FlatEipBackend.APPLY_EIP_PATH) { rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), FlatEipBackend.ApplyEipCmd.class)
            return rsp

        }
        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.getVmNics().get(0).getUuid()
        }

        assert cmd.eip.eipUuid == eip.uuid
        assert dbFindByUuid(eip.uuid, EipVO.class).guestIp == vm.getVmNics().get(0).getIp()


        String vipUuid = Q.New(VipVO.class).select(VipVO_.uuid).eq(VipVO_.l3NetworkUuid, pub_l3.uuid).findValue()
        GetEipAttachableVmNicsAction getEipAttachableVmNicsAction = new GetEipAttachableVmNicsAction()
        getEipAttachableVmNicsAction.eipUuid = eip.uuid
        getEipAttachableVmNicsAction.vipUuid = vipUuid
        getEipAttachableVmNicsAction.sessionId = adminSession()
        GetEipAttachableVmNicsAction.Result res = getEipAttachableVmNicsAction.call()
        assert res.error == null
        assert res.value.inventories != null
        assert res.value.inventories.size() == 0
    }

    void testDetachEipWhenVmStopped(){
        stopVmInstance {
            uuid = vm.uuid
        }

        detachEip {
            uuid = eip.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.getVmNics().get(0).getUuid()
        }
    }


    void testDeleteEip() {
        FlatEipBackend.DeleteEipCmd cmd = new FlatEipBackend.DeleteEipCmd()
        env.afterSimulator(FlatEipBackend.DELETE_EIP_PATH) { rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), FlatEipBackend.DeleteEipCmd.class)
            return rsp
        }

        deleteEip {
            uuid = eip.uuid
        }

        assert cmd.eip.eipUuid == eip.uuid
        assert dbFindByUuid(eip.uuid, EipVO.class) == null
    }

    void testReconnectHostBatchApplyEips() {
        def host = env.inventoryByName("kvm") as HostInventory
        FlatEipBackend.BatchApplyEipCmd cmd = new FlatEipBackend.BatchApplyEipCmd()

        env.afterSimulator(FlatEipBackend.BATCH_APPLY_EIP_PATH) { rsp, HttpEntity<String> entity ->
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
