package org.zstack.test.integration.ldap

import org.junit.ClassRule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.ldap.LdapSystemTags
import org.zstack.sdk.identity.ldap.entity.LdapServerInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * @author shenjin
 * @date 2022/10/24 16:45
 */
class LdapCleanBindingFilterCase extends SubCase {
    EnvSpec env

    LdapServerInventory ldapServer

    public static String DOMAIN_DSN = "dc=example,dc=com"
    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance()
            .bindingToPort(1888)
            .usingDomainDsn(DOMAIN_DSN)
            .importingLdifs("users-import.ldif")
            .build()

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testUpdateFiler()
            testAddLdapWithFilter()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void prepare() {
        ldapServer = addLdapServer {
            name = "ldap0"
            description = "test-ldap0"
            base = DOMAIN_DSN
            url = "ldap://localhost:1888"
            username = ""
            password = ""
            encryption = "None"
        } as LdapServerInventory

        assert null == LdapSystemTags.LDAP_ALLOW_LIST_FILTER.getTag(ldapServer.uuid)
        assert null == LdapSystemTags.LDAP_CLEAN_BINDING_FILTER.getTag(ldapServer.uuid)
    }

    void testUpdateFiler(){
        String filter = "(age=3)"

        updateLdapServer {
            ldapServerUuid = ldapServer.uuid
            systemTags = [LdapSystemTags.LDAP_ALLOW_LIST_FILTER.instantiateTag([(LdapSystemTags.LDAP_ALLOW_LIST_FILTER_TOKEN): filter])]
        }

        assert filter == LdapSystemTags.LDAP_ALLOW_LIST_FILTER.getTokenByResourceUuid(ldapServer.uuid, LdapSystemTags.LDAP_ALLOW_LIST_FILTER_TOKEN)

        updateLdapServer {
            ldapServerUuid = ldapServer.uuid
            systemTags = [LdapSystemTags.LDAP_CLEAN_BINDING_FILTER.instantiateTag([(LdapSystemTags.LDAP_CLEAN_BINDING_FILTER_TOKEN): filter])]
        }

        assert filter == LdapSystemTags.LDAP_CLEAN_BINDING_FILTER.getTokenByResourceUuid(ldapServer.uuid, LdapSystemTags.LDAP_CLEAN_BINDING_FILTER_TOKEN)

        deleteLdapServer {
            uuid = ldapServer.uuid
        }

        assert null == LdapSystemTags.LDAP_ALLOW_LIST_FILTER.getTag(ldapServer.uuid)
        assert null == LdapSystemTags.LDAP_CLEAN_BINDING_FILTER.getTag(ldapServer.uuid)
    }

    void testAddLdapWithFilter(){
        String filter = "(cn=Micha Kops)"
        String filter2 = "(name=zstack)"

        def newLdapServerInventory = addLdapServer {
            name = "ldap1"
            description = "test-ldap0"
            base = DOMAIN_DSN
            url = "ldap://localhost:1888"
            username = ""
            password = ""
            encryption = "None"
            systemTags = [
                LdapSystemTags.LDAP_ALLOW_LIST_FILTER.instantiateTag([(LdapSystemTags.LDAP_ALLOW_LIST_FILTER_TOKEN): filter]),
                LdapSystemTags.LDAP_CLEAN_BINDING_FILTER.instantiateTag([(LdapSystemTags.LDAP_CLEAN_BINDING_FILTER_TOKEN): filter2])
            ]
        } as LdapServerInventory

        assert filter == LdapSystemTags.LDAP_ALLOW_LIST_FILTER.getTokenByResourceUuid(newLdapServerInventory.uuid, LdapSystemTags.LDAP_ALLOW_LIST_FILTER_TOKEN)
        assert filter2 == LdapSystemTags.LDAP_CLEAN_BINDING_FILTER.getTokenByResourceUuid(newLdapServerInventory.uuid, LdapSystemTags.LDAP_CLEAN_BINDING_FILTER_TOKEN)
    }
}
