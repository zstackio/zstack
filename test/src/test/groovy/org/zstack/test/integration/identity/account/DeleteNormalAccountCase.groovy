package org.zstack.test.integration.identity.account

import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceDeletionPolicyManager
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.identity.IdentityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by miao on 17-6-7.
 */
class DeleteNormalAccountCase extends SubCase {
    EnvSpec env
    AccountInventory accountInventory

    def DOC = """
1. create an environment with admin account and normal account and other resources
2. delete all AccountResourceRefVO purposely
3. delete normal account to trigger admin adopting all orphaned resources
4. check
"""

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(IdentityTest.springSpec)
    }

    @Override
    void environment() {
        // one base vm, with a data volume
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
                useDiskOfferings("diskOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            accountInventory = createAccount {
                name = "test"
                password = "password"
            } as AccountInventory

            VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Delay.toString())

            testAdminAdoptOrphanedResourceAfterDeletedNormalAccount()
        }
    }

    void testAdminAdoptOrphanedResourceAfterDeletedNormalAccount() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        VolumeInventory vol = vm.getAllVolumes().find { i -> i.getUuid() != vm.getRootVolumeUuid() }

        // gather resources which should be referenced in AccountResourceRefVO and belong to admin
        // these should later belong to admin again
        List<String> resourceUuids = Q.New(AccountResourceRefVO.class)
                .select(AccountResourceRefVO_.resourceUuid)
                .eq(AccountResourceRefVO_.accountUuid, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
                .listValues()

        // change one vm's owner to normal account, leave an appliance vm to admin
        changeResourceOwner {
            accountUuid = accountInventory.getUuid()
            resourceUuid = vm.getUuid()
        }

        // remove all AccountResourceRefVO records purposely
        Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.accountUuid, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
                .list()
                .stream()
                .forEach { it ->
            logger.debug(String.format("resourceUuid:%s, resourceType:%s",
                    (it as AccountResourceRefVO).getResourceUuid(),
                    (it as AccountResourceRefVO).getResourceType()))
        }

        SQL.New(AccountResourceRefVO.class).hardDelete()

        // delete normal account to trigger adoption
        deleteAccount {
            uuid = accountInventory.getUuid()
        }

        // check admin adopt all
        retryInSecs {
            def size = Q.New(AccountResourceRefVO.class)
                    .in(AccountResourceRefVO_.resourceUuid, resourceUuids)
                    .eq(AccountResourceRefVO_.accountUuid, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
                    .count()
            assert size == resourceUuids.size() as Long
        }
    }
}
