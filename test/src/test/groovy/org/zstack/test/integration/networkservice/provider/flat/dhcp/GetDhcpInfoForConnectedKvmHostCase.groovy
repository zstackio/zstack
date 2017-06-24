package org.zstack.test.integration.networkservice.provider.flat.dhcp

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class GetDhcpInfoForConnectedKvmHostCase extends SubCase {

    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory l3

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
                }

                attachBackupStorage("sftp")
            }

            vm{
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
            runTest()
        }
    }


    void runTest(){
        HostInventory host = env.inventoryByName("kvm")
        VmInstanceInventory vm = env.inventoryByName("vm")

        setVmHostname {
            uuid = vm.uuid
            hostname = vm.vmNics[0].ip.replaceAll("\\.", "-")
        }

        FlatDhcpBackend.ApplyDhcpCmd cmd = null
        env.afterSimulator(FlatDhcpBackend.APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.ApplyDhcpCmd.class)

            assert 1 == cmd.dhcp.size()
            assert cmd.dhcp.get(0).hostname == VmSystemTags.HOSTNAME.getTag(vm.uuid)

            return rsp
        }

        reconnectHost {
            uuid = host.uuid
        }
        assert null != cmd
    }

    @Override
    void clean() {
        env.delete()
    }
}
