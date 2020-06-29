package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.message.AbstractBeforeSendMessageInterceptor
import org.zstack.header.message.Message
import org.zstack.header.network.l3.IpRangeEO
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.vm.DetachIpAddressFromVmNicMsg
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.sdk.*
import org.zstack.test.integration.network.l3network.Env
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/11/30.
 */
class IPv6ConcurrentDeleteL3NPECase extends SubCase {
    EnvSpec env
    CloudBus bus

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }
    @Override
    void environment() {
        env = Env.Ipv6FlatL3Network()
    }

    @Override
    void test() {
        env.create {
            bus = bean(CloudBus.class)
            testDeleteL3WithoutEip()
        }
    }

    void testDeleteL3WithoutEip() {
        /* this test case is for http://jira.zstack.io/browse/ZSTAC-16865 */
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        addIpRangeByNetworkCidr {
            name = "ipr4-1"
            l3NetworkUuid = l3_statefull.getUuid()
            networkCidr = "192.168.110.0/24"
        }
        VmInstanceInventory vm = createVmInstance {
            name = "test-vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_statefull.uuid)
        }
        VmNicInventory nic = vm.getVmNics()[0]

        /* when detach l3, wait until deletion of l3_statefull is done */
        long ipRangeCount = Q.New(IpRangeEO.class).count()
        bus.installBeforeSendMessageInterceptor(new AbstractBeforeSendMessageInterceptor() {
            @Override
            public void beforeSendMessage(Message msg) {
                if (!(msg instanceof DetachIpAddressFromVmNicMsg)) {
                    return
                }

                DetachIpAddressFromVmNicMsg ipmsg = (DetachIpAddressFromVmNicMsg) msg
                UsedIpVO ip = Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, ipmsg.usedIpUuid).find()
                if (ip == null) {
                    return
                }
                if (ip.l3NetworkUuid == l3.uuid) {
                    retryInSecs {
                        assert Q.New(IpRangeEO.class).count() <= ipRangeCount - 1
                    }
                }
            }
        })

        def thread1 = Thread.start {
            deleteL3Network {
                uuid = l3_statefull.uuid
            }
        }

        def thread2 = Thread.start {
            deleteL3Network {
                uuid = l3.uuid
            }
        }
        [thread1, thread2].each {it.join()}

        assert !Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nic.uuid).exists
        for (UsedIpInventory ip : nic.getUsedIps()) {
            assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, ip.uuid).exists
        }
    }

}

