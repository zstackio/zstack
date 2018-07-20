package org.zstack.test.integration.ldap

import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchScope
import org.junit.Rule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.core.db.Q
import org.zstack.identity.Account
import org.zstack.ldap.LdapAccountRefVO
import org.zstack.ldap.LdapConstant
import org.zstack.ldap.LdapSystemTags
import org.zstack.sdk.AddLdapServerAction
import org.zstack.sdk.AddLdapServerResult
import org.zstack.sdk.ApiResult
import org.zstack.sdk.LdapServerInventory
import org.zstack.sdk.LogInByLdapAction
import org.zstack.sdk.ZSClient
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.stabilisation.StabilityTestCase
import org.zstack.test.integration.stabilisation.TestCaseStabilityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test

/**
 * Created by Administrator on 2017-03-22.
 */

//base on TestLdapConn
class LdapBasicCase extends SubCase {
    EnvSpec env

    public static String DOMAIN_DSN = "dc=example,dc=com"

    @Rule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1888).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import.ldif").build()

    String LdapUuid

    @Override
    void setup() {
        spring {
            kvm()
            localStorage()
            sftpBackupStorage()
            include("LdapManagerImpl.xml")
            include("captcha.xml")
        }
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testAddLdapServer()
            testRepeatToAddLdapServer()

            testGetLdapEntry()

            testGetCandidateLdapEntryForBinding()

            testCreateLdapBinding()

            testLoginByLdap()

            testCleanInvalidLdapBinding()

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

    void testLoginByLdap(){

        String notExistCn = "nobody"
        String cn = "Micha Kops"
        String wrongPassword = "error"
        String rightPassword = "password"

        LogInByLdapAction action = new LogInByLdapAction(
                uid: notExistCn,
                password: rightPassword
        )
        assert null != action.call().error

        action = new LogInByLdapAction(
                uid: cn,
                password: wrongPassword
        )
        assert null != action.call().error


        logInByLdap {
            uid = cn
            password = rightPassword
        }
    }

    void testCleanInvalidLdapBinding(){
        assert Q.New(LdapAccountRefVO.class).exists

        cleanInvalidLdapBinding {
        }

        assert !Q.New(LdapAccountRefVO.class).exists
    }

    void testGetLdapEntry(){

        List result = getLdapEntry {
            ldapFilter = "(cn=nobody)"
        }
        assert 0 == result.size()

        result = getLdapEntry {
            ldapFilter = "(objectClass=person)"
        }
        assert 3 == result.size()

        result = getLdapEntry {
            ldapFilter = "(objectClass=person)"
            limit = 1
        }
        assert 1 == result.size()

        String cn = "Micha Kops"
        result = getLdapEntry {
            ldapFilter = "(cn=${cn})"
            limit = 2
        }
        assert 1 == result.size()

        List<Map> attributes =  result.get(0).get("attributes")
        for(Map map : attributes){
            if(map.get("cn") == null){
                continue
            }
            assert 1 == map.get("values").size()
            assert cn == map.get("values").get(0)
        }
    }

    void testGetCandidateLdapEntryForBinding(){

        List result = getCandidateLdapEntryForBinding {
            ldapFilter = "(cn=nobody)"
        }
        assert 0 == result.size()

        result = getCandidateLdapEntryForBinding {
            ldapFilter = "(objectClass=person)"
        }
        assert 3 == result.size()

        result = getCandidateLdapEntryForBinding {
            ldapFilter = "(objectClass=person)"
            limit = 2
        }
        assert 2 == result.size()

        String cn = "Micha Kops"
        result = getCandidateLdapEntryForBinding {
            ldapFilter = "(cn=${cn})"
        }
        assert 1 == result.size()
    }

    void testCreateLdapBinding(){
        String notExistCn = "nobody"
        expect (AssertionError.class) {
            createLdapBinding {
                accountUuid = Test.currentEnvSpec.session.accountUuid
                ldapUid = notExistCn
            }
        }

        List result = getCandidateLdapEntryForBinding {
            ldapFilter = "(objectClass=person)"
        }
        assert 3 == result.size()

        String dn = "cn=Micha Kops,ou=Users,dc=example,dc=com"
        createLdapBinding {
            accountUuid = Test.currentEnvSpec.session.accountUuid
            ldapUid = dn
        }

        result = getCandidateLdapEntryForBinding {
            ldapFilter = "(objectClass=person)"
            limit = 10000
        }
        assert 2 == result.size()

        result = getCandidateLdapEntryForBinding {
            ldapFilter = "(objectClass=person)"
        }
        assert 2 == result.size()
        for(Map map : result){
            assert dn != map.get(LdapConstant.LDAP_DN_KEY)
        }

        result = getCandidateLdapEntryForBinding {
            ldapFilter = "(cn=Micha Kop)"
        }
        assert 0 == result.size()

        result = getCandidateLdapEntryForBinding {
            ldapFilter = "(cn=Micha Kop)"
            limit = 1
        }
        assert 0 == result.size()

        testGetLdapEntry()
    }

    void testAddLdapServer(){
        LDAPInterface ldapConnection = this.getLdapConn()
        final SearchResult searchResult = ldapConnection.search(ZStackTest.DOMAIN_DSN, SearchScope.SUB, "(objectClass=person)")
        assert searchResult.getEntryCount() == 3

        def result = addLdapServer {
            name = "ldap0"
            description = "test-ldap0"
            base = ZStackTest.DOMAIN_DSN
            url = "ldap://localhost:1888"
            username = ""
            password = ""
            encryption = "None"
            systemTags = [LdapSystemTags.LDAP_SERVER_TYPE.instantiateTag([(LdapSystemTags.LDAP_SERVER_TYPE_TOKEN): LdapConstant.OpenLdap.TYPE]),
                          LdapSystemTags.LDAP_CLEAN_BINDING_FILTER.instantiateTag([(LdapSystemTags.LDAP_CLEAN_BINDING_FILTER_TOKEN): "(cn=Micha Kops)"])]
            sessionId = Test.currentEnvSpec.session.uuid
        } as LdapServerInventory
        LdapUuid = result.uuid

        AddLdapServerAction addLdapServerAction = new AddLdapServerAction(
                name : "ldap0",
                description : "test-ldap0",
                base : ZStackTest.DOMAIN_DSN,
                url : "ldap://localhost:1888",
                username : "",
                password : "",
                encryption : "None",
                systemTags : ["ephemeral::validationOnly"],
                sessionId : Test.currentEnvSpec.session.uuid
        )
        ApiResult res = ZSClient.call(addLdapServerAction)
        assert res.error == null
        assert null == res.getResult(AddLdapServerResult.class).inventory
    }

    void testRepeatToAddLdapServer(){

        AddLdapServerAction addLdapServerAction = new AddLdapServerAction(
                name : "ldap0",
                description : "test-ldap0",
                base : ZStackTest.DOMAIN_DSN,
                url : "ldap://localhost:1888",
                username : "",
                password : "",
                encryption : "None",
                sessionId : Test.currentEnvSpec.session.uuid
        )
        assert null != addLdapServerAction.call().error

        addLdapServerAction = new AddLdapServerAction(
                name : "ldap0",
                description : "test-ldap1",
                base : ZStackTest.DOMAIN_DSN,
                url : "ldap://172.20.11.200:1888",
                username : "uid=admin,cn=users,cn=accounts,dc=mevoco,dc=com",
                password : "password",
                encryption : "TLS",
                systemTags : ["ephemeral::validationOnly"],
                sessionId : Test.currentEnvSpec.session.uuid
        )
        assert null != addLdapServerAction.call().error

        addLdapServerAction = new AddLdapServerAction(
                name : "ldap0",
                description : "test-ldap1",
                base : ZStackTest.DOMAIN_DSN,
                url : "ldap://172.20.11.200:1888",
                username : "uid=admin,cn=users,cn=accounts,dc=mevoco,dc=com",
                password : "password",
                encryption : "TLS",
                sessionId : Test.currentEnvSpec.session.uuid
        )
        assert null != addLdapServerAction.call().error
    }

    void testDeleteLdapServer(){
        deleteLdapServer {
            uuid = LdapUuid
            sessionId = Test.currentEnvSpec.session.uuid
        }
    }
}
