package org.zstack.test.integration.networkservice.provider.flat.dhcp

import org.zstack.compute.host.HostGlobalConfig
import org.zstack.core.asyncbatch.While
import org.zstack.core.thread.AsyncThread
import org.zstack.header.core.FutureCompletion
import org.zstack.header.core.WhileCompletion
import org.zstack.header.core.WhileDoneCompletion
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.TimeUnit


/**
 * Created by heathhose on 17-4-1.
 */
class TestEnableFor800HostsCase extends SubCase{

    EnvSpec env
    FlatDhcpBackend dhcpBackend
    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        dhcpBackend = bean(FlatDhcpBackend.class)
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
                        name = "host-1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2-1")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth0"

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

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm-1"
                useImage("image")
                useL3Networks("l3-1")
                useInstanceOffering("instanceOffering")
                useHost("host-1")
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepareEnv()
            testDisableEnableDhcp()
            //testWhile800()
        }
    }

    void testWhile800() {
        List<Integer> lst = []
        FutureCompletion fc = new FutureCompletion(null)

        int count = 0
        for (int i = 0; i < 800; i++) {
            lst.add(i)
        }

        new While<>(lst, "ruanshixin").step(new While.Do() {
            @Override
            @AsyncThread
            void accept(Object item, WhileCompletion completion) {
                count ++
                completion.done()
            }
        } ,10).run(new WhileDoneCompletion(fc) {
            @Override
            void done(org.zstack.header.errorcode.ErrorCodeList errorCodeList) {
                fc.success()
            }
        })

        fc.await(TimeUnit.SECONDS.toMillis(100))
        assert count == 800
    }

    void prepareEnv() {
        ClusterInventory cluster = env.inventoryByName("cluster")
        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(Integer.MAX_VALUE)

        for (int i = 1; i < 4; i++) {
            for (int j = 1; j < 200; j++) {
                addKVMHost {
                    name = "kvm-2"
                    managementIp = String.format("127.0.%s.%s", i, j)
                    username = "root"
                    password = "password"
                    clusterUuid = cluster.uuid
                }
            }
        }
    }

    void testDisableEnableDhcp() {
        final L3NetworkInventory l3 = env.inventoryByName("l3-1")

        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l3.uuid
            service = 'DHCP'
        }

        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = ['Flat':['DHCP']]
        }

        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l3.uuid
            service = 'DHCP'
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
