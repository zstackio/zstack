package org.zstack.test.integration.ldap

import org.junit.Rule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by Administrator on 2017-03-22.
 */


class LdapTest  extends Test {
    static SpringSpec springSpec = makeSpring {
        virtualRouter()
        vyos()
        kvm()
        localStorage()
        sftpBackupStorage()
        include("LdapManagerImpl.xml")
    }
    public static final String DOMAIN_DSN = "dc=example,dc=com"
    @Rule
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
