package org.zstack.test.integration.kvm.vm

import org.zstack.compute.vm.VmNicManager
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.SQL
import org.zstack.header.image.ImageVO
import org.zstack.header.image.ImageVO_
import org.zstack.header.identity.AccountConstant
import org.zstack.image.ImageSystemTags
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.CreateSystemTagAction
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class CreateVmInPubL3Case extends SubCase {
    EnvSpec env
    VmNicManager nicMgr

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
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

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"
                        category = "Public"

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
        }
    }

    @Override
    void test() {
        nicMgr = bean(VmNicManager.class)
        env.create {
            createVmWithAL3NetworkWhichNotAttachedToTheCluster()
            testMarketplaceZnsOnSystemNetwork()
        }
    }

    /* We do not allow create vm in network which system = true,
     * but in network which system = false, category = Public is allowed
     */
    void createVmWithAL3NetworkWhichNotAttachedToTheCluster() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")

        /* change image virtio flag */
        SQL.New(ImageVO).eq(ImageVO_.uuid, image.uuid).set(ImageVO_.virtio, Boolean.TRUE).update()
        VmInstanceInventory vm = createVmInstance {
            name = "test"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [pubL3.uuid]
        }

        VmNicInventory nic = vm.vmNics.get(0)
        assert nic.driverType == nicMgr.getDefaultPVNicDriver()
        
        destroyVmInstance {
            uuid = vm.uuid
        }
    }

    void testMarketplaceZnsOnSystemNetwork() {
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        L3NetworkInventory publicL3 = env.inventoryByName("pubL3")
        L3NetworkInventory managementL3 = createL3Network {
            name = "system-management"
            l2NetworkUuid = publicL3.l2NetworkUuid
            category = "System"
            system = true
        }
        addIpRange {
            name = "system-management-range"
            l3NetworkUuid = managementL3.uuid
            startIp = "192.168.200.10"
            endIp = "192.168.200.20"
            netmask = "255.255.255.0"
            gateway = "192.168.200.1"
        }

        def create = { boolean marketplace ->
            createVmInstance {
                name = "marketplace-zns"
                instanceOfferingUuid = offering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [managementL3.uuid]
                systemTags = marketplace ? [VmSystemTags.MARKET_PLACE_TOKEN] : []
            }
        }

        // Neither an ordinary VM nor a generic Marketplace app can use system L3.
        expect(AssertionError) { create(false) }
        expect(AssertionError) { create(true) }
        testZnsImageTagRequiresAdmin(image)
        createSystemTag {
            resourceUuid = image.uuid
            resourceType = ImageVO.simpleName
            tag = ImageSystemTags.MARKETPLACE_ZNS_IMAGE.tagFormat
        }
        // A ZNS image alone does not bypass the network constraint.
        expect(AssertionError) { create(false) }
        VmInstanceInventory vm = create(true)
        assert vm.type == "UserVm"
        assert vm.vmNics.size() == 1
        assert vm.vmNics[0].l3NetworkUuid == managementL3.uuid
        destroyVmInstance { uuid = vm.uuid }

        changeL3NetworkState {
            uuid = managementL3.uuid
            stateEvent = "disable"
        }
        expect(AssertionError) { create(true) }
        deleteL3Network { uuid = managementL3.uuid }
    }

    void testZnsImageTagRequiresAdmin(ImageInventory image) {
        def tenant = createAccount {
            name = "zns-image-tag-tenant"
            password = "password"
        }
        changeResourceOwner {
            resourceUuid = image.uuid
            accountUuid = tenant.uuid
        }
        try {
            def tenantSession = logInByAccount {
                accountName = tenant.name
                password = "password"
            }
            def action = new CreateSystemTagAction()
            action.resourceUuid = image.uuid
            action.resourceType = ImageVO.simpleName
            action.sessionId = tenantSession.uuid
            action.tag = ImageSystemTags.CREATED_BY_MARKETPLACE.tagFormat
            assert action.call().error == null
            action.tag = ImageSystemTags.MARKETPLACE_ZNS_IMAGE.tagFormat
            def result = action.call()
            assert result.error != null
            assert result.error.globalErrorCode == "ORG_ZSTACK_TAG_10008"
        } finally {
            changeResourceOwner {
                resourceUuid = image.uuid
                accountUuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
            }
            deleteAccount { uuid = tenant.uuid }
        }
    }
}
