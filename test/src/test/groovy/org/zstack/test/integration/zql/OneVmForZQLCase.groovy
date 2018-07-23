package org.zstack.test.integration.zql

import org.zstack.core.db.Q
import org.zstack.header.network.l3.L3NetworkVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.search.Inventory
import org.zstack.header.zone.ZoneVO
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.BeanUtils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.zql.ZQL
import org.zstack.zql.ZQLQueryReturn

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

                l2VlanNetwork {
                    name = "l2-vlan"
                    physicalInterface = "eth0"
                    vlan = 100
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

    void testCheckInventoryConstructors() {
        List<String> errors = []
        BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class).findAll { it.isAnnotationPresent(Inventory.class) }.each {
            try {
                it.getConstructor().newInstance()
            } catch (Exception e) {
                errors.add("inventory class[${it}] must have a non-arg constructor, ${e.message}")
            }
        }

        assert errors.isEmpty() : "${errors.join("\n")}"
    }

    @Override
    void test() {
        env.create {
            testCheckInventoryConstructors()

            queryVmInstance { conditions=["hostUuid not null"] }

            ZQL.fromString("count l2network where zone.uuid = '13eb449d4f144ab7a613321cfee9da6a' and uuid not in ('') and type != 'VxlanNetworkPool' limit 1000")
                    .getSingleResult()

            long start = System.currentTimeMillis()


            for (int i=0; i<3; i++) {
                start = System.currentTimeMillis()
                ZQL.fromString("query l3network where category = 'Private' and type = 'L3VpcNetwork' and vmNic.uuid != 'null' and vmNic.vmInstance.state = 'Running' and vmNic.vmInstance.type = 'ApplianceVm' and zoneUuid = 'dd2c85dcdb2e4bfcbad1d626174d5424' and system = 'false' and l2Network.cluster.type = 'zstack' and uuid not in ('') and uuid in ('2db791a4ceb54c02b6dde024479468b7','8cbf93a975bd4435ad8483b625fec6b1','a44c255d746142ea9b9140dfc23be379') restrict by (zone.uuid = 'abcd') return with (total) order by createDate desc limit 20")
                        .getSingleResult()
                logger.debug("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx ${System.currentTimeMillis() - start}ms")
            }

            ZQL.fromString("count vip where useFor is null and l3Network.zoneUuid = '0f0ff43535164fe4bf1a09b245389c91' limit 1000")
                    .getSingleResult()

            ZoneInventory zone = env.inventoryByName("zone")
            start = System.currentTimeMillis()
            def ret = ZQL.fromString("query vminstance where vmNics.l3Network.l2Network.zoneUuid = '${zone.uuid}'" +
                    " restrict by (zone.name = 'zone')" +
                    " return with (total)").getSingleResult()
            start = System.currentTimeMillis()
            ret = ZQL.fromString("query instanceoffering where memorySize > 1 return with (total)").getSingleResult()
            start = System.currentTimeMillis()
            ret = ZQL.fromString("count instanceoffering where memorySize > 1").getSingleResult()

            createUserTag {
                resourceUuid = zone.uuid
                resourceType = ZoneVO.class.simpleName
                tag = "abc"
            }

            ZoneInventory zone1 = queryZone { conditions = ["__userTag__=abc"] }[0]
            assert zone.uuid == zone1.uuid
            
            zone1 = queryZone {
                fields = ["name", "uuid"]
                conditions = ["name=zone"]
            }[0]

            assert zone1
            assert zone1.uuid
            assert zone1.name
            assert zone1.createDate == null
            assert zone1.lastOpDate == null

            zone1 = queryZone {
                fields = ["name"]
                conditions = ["name=zone"]
            }[0]

            assert zone1
            assert zone1.uuid == null
            assert zone1.name

            VmInstanceInventory vm = env.inventoryByName("vm")
            setVmHostname {
                uuid = vm.uuid
                hostname = "localhost"
            }

            VmInstanceInventory vm1 = queryVmInstance { conditions=["__systemTag__~=%localhost%"] }[0]
            assert vm.uuid == vm1.uuid

            BackupStorageInventory bs = env.inventoryByName("sftp")
            BackupStorageInventory bss = queryBackupStorage { conditions = ["attachedZoneUuids=${zone.uuid}"] }[0]
            assert bss
            assert bss.uuid == bs.uuid

            List images = queryImage { conditions = ["backupStorage.__systemTag__!=remote"] }
            assert !images.isEmpty()

            bss = queryBackupStorage { conditions = ["zone.backupStorage.attachedZoneUuids=${zone.uuid}"] }[0]
            assert bss
            assert bss.uuid == bs.uuid

            queryL3Network { conditions = ["networkServices.networkServiceType=Eip"] }

            L2NetworkInventory l2Vlan = env.inventoryByName("l2-vlan")
            def l2 = queryL2Network { conditions=["vlan=100", "type=L2VlanNetwork"] }[0]
            assert l2
            assert l2.uuid == l2Vlan.uuid
            
            HostInventory kvm = queryHost { conditions= ["hypervisorType=KVM"] }[0]
            assert kvm
            assert kvm.username == "root"

            kvm = queryHost {
                fields = ["username"]
                conditions= ["hypervisorType=KVM", "username=root"]
            }[0]
            assert kvm
            assert kvm.username == "root"

            List l2s = queryL2Network { conditions=["type!=VxlanNetworkPool"] }
            assert !l2s.isEmpty()

            queryVmInstance { conditions=["hostUuid is null"] }

            images = queryImage { conditions=["uuid!?="] }
            assert !images.isEmpty()

            ZoneInventory z = env.inventoryByName("zone")
            ZQL.fromString("query image where state = 'Enabled' and type = 'zstack' and mediaType != 'DataVolumeTemplate' and status = 'Ready' and system = 'false' and backupStorage.zone.uuid = '1a2280f6bebe4651ba8560bf81845633' restrict by (__systemTag__ != 'remote',__systemTag__ != 'remote', zone.uuid = '${z.uuid}') return with (total) order by createDate desc limit 20").getSingleResult()

            queryVmInstance { conditions = ["rootVolume.localStorageHostRef.hostUuid=abc"] }

            List<ZQLQueryReturn> fs = ZQL.fromString("""query host named as 'host';
query zone return with (total) named as 'zone';

""").getResultList()

            assert fs.size() == 2
            logger.debug(JSONObjectUtil.toJsonString(fs))

            fs = ZQL.fromString("""query host named as 'host';
query zone return with (total) named as 'zone'
""").getResultList()

            fs = ZQL.fromString("sum vminstance.cpuNum,memorySize by uuid where cpuNum > 0 named as 'sum'").getResultList()
            logger.debug(JSONObjectUtil.toJsonString(fs))

            testQueryField()

            def l3s = zqlQuery("query l3network group by uuid, name")
            assert l3s.size() == Q.New(L3NetworkVO.class).count()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testQueryField(){
        List vmInstances = queryVmInstance {}

        List names = queryVmInstance {
            fields = ["name"]
        }
        assert names.size() == vmInstances.size()

        List cpus = queryVmInstance {
            fields = ["cpuNum"]
        }
        assert cpus.size() == vmInstances.size()

        List memorySizes = queryVmInstance {
            fields = ["memorySize"]
        }
        assert memorySizes.size() == vmInstances.size()

        List createDates = queryVmInstance {
            fields = ["createDate"]
        }
        assert createDates.size() == vmInstances.size()

        List states = queryVmInstance {
            fields = ["state"]
        }
        assert states.size() == vmInstances.size()

        List nameAndCpus = queryVmInstance {
            fields = ["name", "cpuNum"]
        }
        assert nameAndCpus.size() == vmInstances.size()

    }
}
