package org.zstack.test.integration.kvm.vm.timeout

import org.apache.commons.lang.StringUtils
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusGlobalConfig
import org.zstack.core.timeout.ApiTimeoutGlobalProperty
import org.zstack.core.timeout.ApiTimeoutManagerImpl
import org.zstack.header.apimediator.APIIsReadyToGoMsg
import org.zstack.header.apimediator.APIIsReadyToGoReply
import org.zstack.header.host.ConnectHostMsg
import org.zstack.header.host.ConnectHostReply
import org.zstack.header.host.HostConstant
import org.zstack.header.image.APIAddImageMsg
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor
import org.zstack.header.rest.RESTFacade
import org.zstack.header.vm.APICreateVmInstanceMsg
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.GlobalConfigInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.TimeUtils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by xing5 on 2017/2/22.
 */
class VmTimeoutCase extends SubCase {
    EnvSpec env
    RESTFacade restf

    def DOC = """
test a VM's start/stop/reboot/destroy/recover operations 
"""

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        CloudBusGlobalConfig.STATISTICS_ON.updateValue(true)

        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 4
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
                        totalCpu = 1024
                        totalMem = SizeUnit.TERABYTE.toByte(100)
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
                            startIp = "192.168.0.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.0.0"
                            gateway = "192.168.0.1"
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
        }
    }

    void test100VMTimeout() {
        InstanceOfferingInventory iof = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        ImageInventory image = env.inventoryByName("image1")

        ConcurrentHashMap actual = [:]
        ConcurrentHashMap expect = [:]
        long timeout = TimeUnit.SECONDS.toMillis(300L)

        Runnable close = restf.installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            @Override
            void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long t) {
            }

            @Override
            void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long t) {
                if (url.contains(KVMConstant.KVM_START_VM_PATH)) {
                    KVMAgentCommands.StartVmCmd cmd = JSONObjectUtil.toObject(body, KVMAgentCommands.StartVmCmd.class)
                    actual[cmd.vmInstanceUuid] = t
                }
            }
        })

        int num = 100
        CountDownLatch latch = new CountDownLatch(num)
        (0..num-1).each {
            String vmName = "vm-${it}"
            String uuid = Platform.uuid
            long t = timeout + it
            expect[uuid] = TimeUnit.SECONDS.toMillis(t)

            Thread.start {
                try {
                    createVmInstance {
                        resourceUuid = uuid
                        apiTimeout = t
                        name = vmName
                        instanceOfferingUuid = iof.uuid
                        imageUuid = image.uuid
                        l3NetworkUuids = [l3.uuid]
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assert latch.await(5L, TimeUnit.MINUTES) : "timeout"

        expect.each { uuid, time ->
            Long t = actual[uuid]
            assert t : "VM[uuid:${uuid}] timeout not found"
            assert time == t : "VM[uuid:${uuid}] expect timeout[${time}] but got ${t}"
        }

        close.run()
    }

    void testChangeGlobalConfig() {
        InstanceOfferingInventory iof = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        ImageInventory image = env.inventoryByName("image1")

        long timeout = 0
        String uuid = Platform.uuid
        Runnable close = restf.installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            @Override
            void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long t) {
            }

            @Override
            void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long t) {
                if (url.contains(KVMConstant.KVM_START_VM_PATH)) {
                    KVMAgentCommands.StartVmCmd cmd = JSONObjectUtil.toObject(body, KVMAgentCommands.StartVmCmd.class)
                    if (cmd.vmInstanceUuid == uuid) {
                        timeout = t
                    }
                }
            }
        })

        long expect = TimeUnit.MINUTES.toMillis(100L)
        updateGlobalConfig {
            category = ApiTimeoutManagerImpl.APITIMEOUT_GLOBAL_CONFIG_TYPE
            name = APICreateVmInstanceMsg.class.getName()
            value = "${expect}"
        }

        createVmInstance {
            resourceUuid = uuid
            name = "vm"
            instanceOfferingUuid = iof.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }

        assert expect == timeout

        close.run()
    }

    void testMinimalTimeoutGlobalConfig() {
        expectError {
            updateGlobalConfig {
                category = ApiTimeoutManagerImpl.APITIMEOUT_GLOBAL_CONFIG_TYPE
                name = APICreateVmInstanceMsg.class.getName()
                value = "100"
            }
        }
    }

    void testLegacyTimeout() {
        long defaultTimeout = TimeUtils.parseTimeInMillis(StringUtils.removeStart(ApiTimeoutGlobalProperty.APIAddImageMsg, "timeout::"))

        GlobalConfigInventory gc = queryGlobalConfig {
            conditions = ["name=${APIAddImageMsg.class.getName()}"]
        }[0]

        assert gc
        assert gc.value == String.valueOf(defaultTimeout)
    }

    void testInternalMessageTimeout() {
        HostInventory host = env.inventoryByName("kvm")
        CloudBus bus = bean(CloudBus.class)

        long timeout = 0
        env.message(ConnectHostMsg.class) { ConnectHostMsg msg ->
            timeout = msg.getTimeout()
            bus.reply(msg, new ConnectHostReply())
        }

        ConnectHostMsg cmsg = new ConnectHostMsg(uuid: host.uuid)
        bus.makeLocalServiceId(cmsg, HostConstant.SERVICE_ID)
        bus.call(cmsg)

        assert timeout == TimeUtils.parseTimeInMillis(ApiTimeoutGlobalProperty.INTERNAL_MESSAGE_TIMEOUT)
    }

    void testSyncAPITimeout() {
        long timeout = 0

        CloudBus bus = bean(CloudBus.class)
        env.message(APIIsReadyToGoMsg.class) { APIIsReadyToGoMsg msg ->
            timeout = msg.getTimeout()
            bus.reply(msg, new APIIsReadyToGoReply(managementNodeId: Platform.managementServerId))
        }

        bus.call(new APIIsReadyToGoMsg(managementNodeId: Platform.managementServerId, serviceId: "api.portal"))

        assert timeout == TimeUtils.parseTimeInMillis(ApiTimeoutGlobalProperty.SYNCCALL_API_TIMEOUT)
    }

    @Override
    void test() {
        restf = bean(RESTFacade.class)
        env.create {
            testLegacyTimeout()
            testMinimalTimeoutGlobalConfig()
            testChangeGlobalConfig()
            test100VMTimeout()
            testInternalMessageTimeout()
            testSyncAPITimeout()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
