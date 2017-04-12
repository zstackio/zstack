package org.zstack.test.integration.network.l3network.multil3

import org.springframework.http.HttpEntity
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.BridgeNameFinder
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory

import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.CollectionUtils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.function.Function
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.TimeUnit

/**
 * Created by heathhose on 17-3-25.
 */
class ChangeDefaultL3Case extends SubCase{
    def DOC = """
use:
1. create a vm with 2 flat L3 networks
2. change the vm's default L3 network from one to another
3. confirm the ResetDefaultGatewayCmd sent to the backend
"""
    EnvSpec env
    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
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
                    attachL2Network("l2-1")
                    attachL2Network("l2-2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "l3-1"

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

                l2NoVlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth2"

                    l3Network {
                        name = "l3-2"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.200.10"
                            endIp = "192.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.200.1"
                        }
                    }

                }

                attachBackupStorage("sftp")

            }

            vm {
                name = "vm"
                useImage("image")
                useDefaultL3Network("l3-1")
                useL3Networks("l3-1","l3-2")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            changeDefaultL3NetworkToAnother()
        }
    }

    void changeDefaultL3NetworkToAnother(){
        L3NetworkInventory l31i = env.inventoryByName("l3-1")
        L3NetworkInventory l32i = env.inventoryByName("l3-2")
        VmInstanceInventory vmi = env.inventoryByName("vm")

        VmNicInventory nic1 = CollectionUtils.find(vmi.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(l31i.getUuid()) ? arg : null;
            }
        });
        VmNicInventory nic2 = CollectionUtils.find(vmi.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(l32i.getUuid()) ? arg : null;
            }
        });

        FlatDhcpBackend.ResetDefaultGatewayCmd cmd = null
        env.afterSimulator(FlatDhcpBackend.RESET_DEFAULT_GATEWAY_PATH){ rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body,FlatDhcpBackend.ResetDefaultGatewayCmd.class)
            return rsp
        }
        updateVmInstance {
            uuid = vmi.uuid
            defaultL3NetworkUuid = l32i.uuid
        }

        TimeUnit.SECONDS.sleep(2);
        assert nic1.getMac() == cmd.macOfGatewayToRemove
        assert nic1.getGateway() == cmd.gatewayToRemove
        assert new BridgeNameFinder().findByL3Uuid(l31i.getUuid()) == cmd.bridgeNameOfGatewayToRemove

        assert nic2.getMac() == cmd.macOfGatewayToAdd
        assert nic2.getGateway() == cmd.gatewayToAdd
        assert new BridgeNameFinder().findByL3Uuid(l32i.getUuid()) == cmd.bridgeNameOfGatewayToAdd
    }
    @Override
    void clean() {
        env.delete()
    }
}
