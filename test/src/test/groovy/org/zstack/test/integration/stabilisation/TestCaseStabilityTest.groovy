package org.zstack.test.integration.stabilisation

import org.junit.ClassRule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.Case
import org.zstack.testlib.PreStabilityTest
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.StabilityTest
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

    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1888).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import.ldif").build()

    static SpringSpec springSpec = ZStackTest.springSpec

    @Override
    void setup() {
        String targetCaseList = System.getProperty(StabilityTest.targetSubCaseParamKey)
        String[] caseClassNameList = targetCaseList.split(",")
        PreStabilityTest c = Class.forName(caseClassNameList[0]).newInstance() as PreStabilityTest
        setupByMode(c)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases()
    }
}
