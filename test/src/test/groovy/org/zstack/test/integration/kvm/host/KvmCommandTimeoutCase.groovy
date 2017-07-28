package org.zstack.test.integration.kvm.host

/**
 * Created by Bryant on 2017/7/20.
 */
import org.springframework.web.util.UriComponentsBuilder
import org.zstack.core.timeout.ApiTimeoutManager
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor
import org.zstack.header.rest.RESTFacade
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMGlobalProperty
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatDnsBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.TimeUnit

// mvn test -Dtest=StabilityTestCase -Dcases=org.zstack.test.integration.kvm.host.KvmCommandTimeoutCase,org.zstack.test.integration.kvm.host.DeleteHostCase
class KvmCommandTimeoutCase extends SubCase {
    EnvSpec env
    RESTFacade restf
    ApiTimeoutManager timeoutMgr

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

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE
                            types = [NetworkServiceType.DHCP.toString()]
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
            restf = bean(RESTFacade.class)
            timeoutMgr = bean(ApiTimeoutManager.class)
            testDeleteVmCmdTimeout()
        }
    }

    private String buildUrl(String path) {
        HostInventory host =  env.inventoryByName("kvm")
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance()
        ub.scheme(KVMGlobalProperty.AGENT_URL_SCHEME)
        ub.host(host.managementIp)
        ub.port(KVMGlobalProperty.AGENT_PORT)
        if (!"".equals(KVMGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(KVMGlobalProperty.AGENT_URL_ROOT_PATH)
        }
        ub.path(path)
        return ub.build().toUriString()
    }

    void testDeleteVmCmdTimeout() {
        long time = 0
        TimeUnit t = null
        long interceptorTimeout = 0
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        ImageInventory image = env.inventoryByName("image1") as ImageInventory
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory

        String url_test=buildUrl(FlatDhcpBackend.APPLY_DHCP_PATH);
        restf.installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            @Override
            void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long timeout) {

            }

            @Override
            void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long timeout) {
                if (url.equals(url_test)) {
                    time = TimeUnit.MILLISECONDS.convert(timeout, unit)
                    t = unit
                    interceptorTimeout = unit.toMillis(timeout)
                }
            }
        })

        createVmInstance {
            name = "vm"
            l3NetworkUuids = [l3.uuid]
            imageUuid = image.uuid
            instanceOfferingUuid = instanceOffering.uuid
        }

        retryInSecs {
            assert time == timeoutMgr.getTimeout(FlatDhcpBackend.ApplyDhcpCmd.getClass(), "5m")
            assert t.toMillis(interceptorTimeout) == time
            assert interceptorTimeout == timeoutMgr.getTimeout(FlatDhcpBackend.ApplyDhcpCmd.getClass(), "5m")
        }
    }
}

