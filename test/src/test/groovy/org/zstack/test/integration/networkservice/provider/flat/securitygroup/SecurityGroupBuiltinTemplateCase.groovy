package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.zstack.core.db.Q
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SessionInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Scenario: An account reads builtin templates, creates its own groups, and edits a template before creation.
 * Expected: The submitted rules and order are preserved without changing templates or previously created groups.
 */
class SecurityGroupBuiltinTemplateCase extends SubCase {
    EnvSpec env
    AccountInventory account
    SessionInventory session
    SessionInventory userSession
    List templates
    Map<String, SecurityGroupInventory> groups = [:]

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {}
    }

    @Override
    void test() {
        env.create {}
        testLoadBuiltinTemplates()
        testCreateGroupsFromTemplates()
        testEditTemplateBeforeCreate()
    }

    @Override
    void clean() {
        env.delete()
    }

    void testLoadBuiltinTemplates() {
        account = createAccount {
            name = "template-account"
            password = "password"
        }
        session = logInByAccount {
            accountName = account.name
            password = "password"
        }
        createUser {
            name = "template-user"
            password = "password"
            sessionId = session.uuid
        }
        userSession = logInByUser {
            accountName = account.name
            userName = "template-user"
            password = "password"
        }

        templates = getSecurityGroupTemplates {}.templates

        assert templates*.id == ["Custom", "CommonPorts", "AllowAllIpv4"] : "builtin template IDs mismatch: actual=${templates*.id}"
        assert templates.findAll { it.defaultSelected }*.id == ["Custom"] : "only Custom must be selected by default"
        assert templates.find { it.id == "Custom" }.rules.empty : "Custom must not add user rules"

        def accountTemplates = getSecurityGroupTemplates { sessionId = session.uuid }.templates

        assert JSONObjectUtil.toJsonString(accountTemplates) == JSONObjectUtil.toJsonString(templates) : "account must see the same builtin templates"

        def subaccountTemplates = getSecurityGroupTemplates { sessionId = userSession.uuid }.templates

        assert JSONObjectUtil.toJsonString(subaccountTemplates) == JSONObjectUtil.toJsonString(templates) : "subaccount must see the same builtin templates"

        def common = templates.find { it.id == "CommonPorts" }.rules

        assert common.size() == 5 && common.every { it.type == "Ingress" && it.ipVersion == 4 } : "common ports must contain five IPv4 ingress rules"
        assert common.findAll { it.protocol == "TCP" }*.dstPortRange == ["22", "80", "443", "3389"] : "common TCP ports mismatch"
        assert common.count { it.protocol == "ICMP" } == 1 : "common ports must include ICMP"

        def all = templates.find { it.id == "AllowAllIpv4" }.rules

        assert all*.type == ["Ingress", "Egress"] && all.every { it.protocol == "ALL" && it.ipVersion == 4 } : "allow-all must contain both IPv4 directions"
        assert templates.every { it.rules.every { rule -> rule.state == "Enabled" && rule.action == "ACCEPT" && !rule.remoteSecurityGroupUuid && !rule.srcIpRange && !rule.dstIpRange } } : "templates must contain only enabled wildcard user rules"
    }

    void testCreateGroupsFromTemplates() {
        templates.each { template ->
            List<SecurityGroupRuleAO> rules = JSONObjectUtil.toCollection(JSONObjectUtil.toJsonString(template.rules), ArrayList.class, SecurityGroupRuleAO.class)
            SecurityGroupInventory group = createSecurityGroup {
                name = "from-${template.id}"
                sessionId = session.uuid
            }
            if (!rules.empty) {
                group = addSecurityGroupRule {
                    securityGroupUuid = group.uuid
                    delegate.rules = rules
                    sessionId = session.uuid
                }
            }
            groups[template.id] = group
            def systemRules = group.rules.findAll { it.priority == 0 }
            def userRules = group.rules.findAll { it.priority > 0 }

            assert systemRules.size() == 4 : "creation must generate four system rules exactly once: template=${template.id}, actual=${systemRules.size()}"
            assert systemRules.collect { [it.type, it.ipVersion, it.remoteSecurityGroupUuid] }.toSet().size() == 4 : "system rules must not be duplicated"
            assert systemRules.every { it.remoteSecurityGroupUuid == group.uuid } : "system rules must reference the created group"
            assert userRules.size() == rules.size() : "user rule count mismatch: template=${template.id}, expected=${rules.size()}, actual=${userRules.size()}"

            ["Ingress", "Egress"].each { direction ->
                def expected = rules.findAll { it.type == direction }
                def actual = userRules.findAll { it.type == direction }.sort { it.priority }

                assert actual*.priority == (expected.empty ? [] : (1..expected.size()).toList()) : "user priorities must follow submitted order: template=${template.id}, direction=${direction}"
                assert actual.collect { [it.protocol, it.dstPortRange, it.ipVersion, it.state, it.action] } == expected.collect { [it.protocol, it.dstPortRange, it.ipVersion, it.state, it.action] } : "created rules must match the confirmed template: template=${template.id}, direction=${direction}"
            }

            assert Q.New(AccountResourceRefVO.class).eq(AccountResourceRefVO_.resourceUuid, group.uuid).eq(AccountResourceRefVO_.ownerAccountUuid, account.uuid).isExists() : "created group must belong to the account: groupUuid=${group.uuid}"
        }
    }

    void testEditTemplateBeforeCreate() {
        String originalTemplates = JSONObjectUtil.toJsonString(getSecurityGroupTemplates {}.templates)
        def oldGroup = querySecurityGroup {
            conditions = ["uuid=${groups.CommonPorts.uuid}"]
            sessionId = session.uuid
        }[0]
        String originalRules = JSONObjectUtil.toJsonString(oldGroup.rules.sort { it.uuid })
        def common = templates.find { it.id == "CommonPorts" }
        List<SecurityGroupRuleAO> editedRules = JSONObjectUtil.toCollection(JSONObjectUtil.toJsonString(common.rules), ArrayList.class, SecurityGroupRuleAO.class)
        editedRules = editedRules.reverse()
        editedRules.each { it.srcIpRange = "192.168.20.0/24" }
        SecurityGroupInventory group = createSecurityGroup {
            name = "edited-common-ports"
            sessionId = session.uuid
        }
        group = addSecurityGroupRule {
            securityGroupUuid = group.uuid
            rules = editedRules
            sessionId = session.uuid
        }
        def created = group.rules.findAll { it.priority > 0 }.sort { it.priority }

        assert created*.srcIpRange.unique() == ["192.168.20.0/24"] : "edited source must be preserved: actual=${created*.srcIpRange}"
        assert created.collect { [it.protocol, it.dstPortRange] } == editedRules.collect { [it.protocol, it.dstPortRange] } : "creation must preserve edited rule order"

        def currentTemplates = getSecurityGroupTemplates {}.templates

        assert JSONObjectUtil.toJsonString(currentTemplates) == originalTemplates : "editing a template must not change its builtin definition"

        def unchanged = querySecurityGroup {
            conditions = ["uuid=${oldGroup.uuid}"]
            sessionId = session.uuid
        }[0]

        assert JSONObjectUtil.toJsonString(unchanged.rules.sort { it.uuid }) == originalRules : "editing must not change a previously created group"
    }
}
