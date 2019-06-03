package org.zstack.test.integration.networkservice.provider.flat.eip

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.EipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by weiwang on 04/12/2017
 */
class StartFlatNetworkVmWithEipCase extends SubCase {

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
                eip {
                    name = "eip-1"
                    useVip("pubL3")
                }
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
            vm {
                name = "vm-1"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testStartVmWithEip()
            testRecoverVmWithEip()
        }
    }

    void testStartVmWithEip(){
        def eip = env.inventoryByName("eip") as EipInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }
    }

    void testRecoverVmWithEip(){
        def eip = env.inventoryByName("eip-1") as EipInventory
        def vm = env.inventoryByName("vm-1") as VmInstanceInventory

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        EipInventory eip1 = queryEip { conditions=["name=eip-1"] }[0]
        assert eip1.vmNicUuid == null
        assert eip1.guestIp == null

        recoverVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
