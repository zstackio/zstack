package org.zstack.test

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.zone.ZoneVO
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.zql.ZQL

/**
 * Created by xing5 on 2017/2/22.
 */
class OneVmForZQLCase extends SubCase {
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
                    name = "image1"
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
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            long start = 0
            start = System.currentTimeMillis()
            ZQL.fromString("count vip where useFor is null and l3Network.zoneUuid = '0f0ff43535164fe4bf1a09b245389c91' limit 1000")
                    .execute()
            logger.debug("1111111111111111111111111111111111111 ${System.currentTimeMillis()-start}ms")

            ZoneInventory zone = env.inventoryByName("zone")
            start = System.currentTimeMillis()
            def ret = ZQL.fromString("query vminstance where vmNics.l3Network.l2Network.zoneUuid = '${zone.uuid}'" +
                    " restrict by (zone.name = 'zone')" +
                    " return with (total)").execute()
            logger.debug("22222222222222222222222222222222 ${System.currentTimeMillis()-start}ms")
            logger.debug("xxxxxxxxxxxxxxxx ${JSONObjectUtil.toJsonString(ret)}")
            start = System.currentTimeMillis()
            ret = ZQL.fromString("query instanceoffering where memorySize > 1 return with (total)").execute()
            logger.debug("33333333333333333333333333333 ${System.currentTimeMillis()-start}ms")
            logger.debug("xxxxxxxxxxxxxxxx ${JSONObjectUtil.toJsonString(ret)}")
            start = System.currentTimeMillis()
            ret = ZQL.fromString("count instanceoffering where memorySize > 1").execute()
            logger.debug("4444444444444444444444444444 ${System.currentTimeMillis()-start}ms")
            logger.debug("yyyyyyyyyyyy ${JSONObjectUtil.toJsonString(ret)}")

            createUserTag {
                resourceUuid = zone.uuid
                resourceType = ZoneVO.class.simpleName
                tag = "abc"
            }

            ZoneInventory zone1 = queryZone { conditions = ["__userTag__=abc"] }[0]
            assert zone.uuid == zone1.uuid

            VmInstanceInventory vm = env.inventoryByName("vm")
            setVmHostname {
                uuid = vm.uuid
                hostname = "localhost"
            }

            VmInstanceInventory vm1 = queryVmInstance { conditions=["__systemTag__~=%localhost%"] }[0]
            assert vm.uuid == vm1.uuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
