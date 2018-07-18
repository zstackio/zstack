package org.zstack.test.integration.ldap

import com.unboundid.ldap.sdk.LDAPInterface
import org.junit.Rule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.ldap.LdapConstant
import org.zstack.ldap.LdapSystemTags
import org.zstack.sdk.*
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.stabilisation.StabilityTestCase
import org.zstack.test.integration.stabilisation.TestCaseStabilityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test

/**
 * Created by lining on 2017-11-09.
 */

class WindowsADCase extends SubCase {
    EnvSpec env

    public static String DOMAIN_DSN = "dc=example,dc=com"

    @Rule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1888).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import.ldif").build()

    String LdapUuid

    @Override
    void setup() {
        spring {
            include("LdapManagerImpl.xml")
            include("captcha.xml")
        }
    }

    @Override
    void environment() {
        env = env{
            zone {
                name = "zone"
                description = "test"
            }
        }
    }

    @Override
    void test() {
        env.create {
            testAddLdapServer()
            testUpdateLdapServerType()
            testDeleteLdapServer()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    LDAPInterface getLdapConn(){
        try{
            return embeddedLdapRule.ldapConnection()
        }catch (Exception e){
        }

        try {
            return ZStackTest.embeddedLdapRule.ldapConnection()
        }catch (Exception e){
        }

        try {
            return StabilityTestCase.embeddedLdapRule.ldapConnection()
        }catch (Exception e){
        }

        try {
            return TestCaseStabilityTest.embeddedLdapRule.ldapConnection()
        }catch (Exception e){
        }
    }

    void testAddLdapServer(){
        LDAPInterface ldapConnection = this.getLdapConn()

        AddLdapServerAction addLdapServerAction = new AddLdapServerAction(
                name : "ldap0",
                description : "test-ldap0",
                base : ZStackTest.DOMAIN_DSN,
                url : "ldap://localhost:1888",
                username : "",
                password : "",
                encryption : "None",
                systemTags : [LdapSystemTags.LDAP_SERVER_TYPE.instantiateTag([(LdapSystemTags.LDAP_SERVER_TYPE_TOKEN): "ErrorType"])],
                sessionId : Test.currentEnvSpec.session.uuid
        )
        ApiResult res = ZSClient.call(addLdapServerAction)
        assert res.error != null

        def result = addLdapServer {
            name = "ldap0"
            description = "test-ldap0"
            base = ZStackTest.DOMAIN_DSN
            url = "ldap://localhost:1888"
            username = ""
            password = ""
            encryption = "None"
            systemTags = [LdapSystemTags.LDAP_SERVER_TYPE.instantiateTag([(LdapSystemTags.LDAP_SERVER_TYPE_TOKEN): LdapConstant.WindowsAD.TYPE])]
        } as LdapServerInventory
        LdapUuid = result.uuid

        assert LdapConstant.WindowsAD.TYPE == LdapSystemTags.LDAP_SERVER_TYPE.getTokenByResourceUuid(LdapUuid, LdapSystemTags.LDAP_SERVER_TYPE_TOKEN)

    }

    void testUpdateLdapServerType(){
        updateLdapServer {
            ldapServerUuid = LdapUuid
            systemTags = [LdapSystemTags.LDAP_SERVER_TYPE.instantiateTag([(LdapSystemTags.LDAP_SERVER_TYPE_TOKEN): LdapConstant.OpenLdap.TYPE])]
        }
        assert LdapConstant.OpenLdap.TYPE == LdapSystemTags.LDAP_SERVER_TYPE.getTokenByResourceUuid(LdapUuid, LdapSystemTags.LDAP_SERVER_TYPE_TOKEN)
    }

    void testDeleteLdapServer(){
        deleteLdapServer {
            uuid = LdapUuid
            sessionId = Test.currentEnvSpec.session.uuid
        }
    }
}
