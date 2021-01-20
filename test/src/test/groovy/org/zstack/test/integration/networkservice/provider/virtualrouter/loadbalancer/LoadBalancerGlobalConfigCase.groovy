package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer


import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.network.service.lb.LoadBalancerGlobalConfig
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest


class LoadBalancerGlobalConfigCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
            }
        }
    }

    @Override
    void test() {
        env.create {
            testHttpMode()
            testIpv4LocalPortRange()
        }
    }


    void testHttpMode(){
        expect(AssertionError.class) {
            updateGlobalConfig {
                category = LoadBalancerGlobalConfig.CATEGORY
                name = LoadBalancerGlobalConfig.HTTP_MODE.name
                value = "http-server"
            }
        }

        updateGlobalConfig {
            category = LoadBalancerGlobalConfig.CATEGORY
            name = LoadBalancerGlobalConfig.HTTP_MODE.name
            value = "http-server-close"
        }
    }

    void testIpv4LocalPortRange(){
        expect(AssertionError.class) {
            updateGlobalConfig {
                category = LoadBalancerGlobalConfig.CATEGORY
                name = LoadBalancerGlobalConfig.IPV4_LOCAL_PORT_RANGE.name
                value = "0"
            }
        }

        expect(AssertionError.class) {
            updateGlobalConfig {
                category = LoadBalancerGlobalConfig.CATEGORY
                name = LoadBalancerGlobalConfig.IPV4_LOCAL_PORT_RANGE.name
                value = "20-1"
            }
        }

        expect(AssertionError.class) {
            updateGlobalConfig {
                category = LoadBalancerGlobalConfig.CATEGORY
                name = LoadBalancerGlobalConfig.IPV4_LOCAL_PORT_RANGE.name
                value = "20-70000"
            }
        }

        expect(AssertionError.class) {
            updateGlobalConfig {
                category = LoadBalancerGlobalConfig.CATEGORY
                name = LoadBalancerGlobalConfig.IPV4_LOCAL_PORT_RANGE.name
                value = "1023-2000"
            }
        }

        updateGlobalConfig {
            category = LoadBalancerGlobalConfig.CATEGORY
            name = LoadBalancerGlobalConfig.IPV4_LOCAL_PORT_RANGE.name
            value = "20000-30000"
        }
    }


}
