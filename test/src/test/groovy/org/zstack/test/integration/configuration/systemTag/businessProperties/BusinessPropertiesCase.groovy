package org.zstack.test.integration.configuration.systemTag.businessProperties

import org.zstack.configuration.BusinessProperties
import org.zstack.kvm.KVMGlobalProperty
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.SubCase

/**
 * Created by mingjian.deng on 2020/1/19.*/
class BusinessPropertiesCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    static SpringSpec springSpec = makeSpring {
        include("ConfigurationManager.xml")
    }

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {
        env = env{

        }
    }

    @Override
    void test() {
        env.create {
            testBusinessProperties()
        }
    }

    void testBusinessProperties() {
        assert BusinessProperties.getPropertiesAsList("KvmHost.iptables.rule").size() > 0
        assert KVMGlobalProperty.IPTABLES_RULES.size() > 0
    }
}
