package org.zstack.test.integration

import org.junit.ClassRule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by lining on 2017/2/27.
 */
class ZStackTest extends Test {
    static SpringSpec springSpec = makeSpring {
        sftpBackupStorage()
        localStorage()
        virtualRouter()
        securityGroup()
        kvm()
        vyos()
        flatNetwork()
        ceph()
        lb()
        nfsPrimaryStorage()
        eip()
        portForwarding()
        smp()
        console()

        include("LdapManagerImpl.xml")
        include("captcha.xml")
        include("CloudBusAopProxy.xml")
        include("ZoneManager.xml")
        include("webhook.xml")
        include("Progress.xml")
        include("vip.xml")
        include("vxlan.xml")
        include("mediateApiValidator.xml")
        include("LongJobManager.xml")
        include("log.xml")
        include("HostAllocateExtension.xml")
    }

    public static final String DOMAIN_DSN = "dc=example,dc=com"

    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1888).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import.ldif").build()

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        runSubCases()
    }

}
