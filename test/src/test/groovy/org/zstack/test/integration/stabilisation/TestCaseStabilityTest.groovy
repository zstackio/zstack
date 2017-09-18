package org.zstack.test.integration.stabilisation

import org.junit.Rule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by lining on 2017/7/15.
 *
 * Example:
 * mvn test -Dtest=TestCaseStabilityTest -Dcases=org.zstack.test.integration.core.MustPassCase -Dtimes=3
 *
 * Q : How do I know which subcase failed ?
 * A : search keyword is 'stability test fails', likes: grep "stability test fails" management-server.log
 */
class TestCaseStabilityTest extends Test {

    public static final String DOMAIN_DSN = "dc=example,dc=com"

    @Rule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1888).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import.ldif").build()

    static SpringSpec springSpec = makeSpring {
        sftpBackupStorage()
        localStorage()
        nfsPrimaryStorage()
        virtualRouter()
        flatNetwork()
        securityGroup()
        kvm()
        ceph()
        smp()
        vyos()
        portForwarding()
        eip()
        lb()
        include("vip.xml")
        include("vxlan.xml")
        include("LdapManagerImpl.xml")
        include("mediateApiValidator.xml")
        include("webhook.xml")
        include("CloudBusAopProxy.xml")
        include("vpc.xml")
    }

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
