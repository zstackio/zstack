package org.zstack.test.integration.console

import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.console.ConsoleConstants
import org.zstack.header.console.ConsoleProxyAgentState
import org.zstack.header.console.ConsoleProxyAgentStatus
import org.zstack.header.console.ConsoleProxyAgentVO
import org.zstack.sdk.SessionInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class ConsoleProxyCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        dbf = bean(DatabaseFacade.class)
        env = env {
            account {
                name = "test"
                password = "password"
            }
        }
    }

    @Override
    void test() {
        env.create {
            testUpdateConsoleProxyAgent()
        }
    }

    def testUpdateConsoleProxyAgent() {
        // create console proxy agent
        ConsoleProxyAgentVO agent = new ConsoleProxyAgentVO()
        agent.setUuid(Platform.getManagementServerId())
        agent.setManagementIp(Platform.getManagementServerIp())
        agent.setConsoleProxyOverriddenIp(CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP)
        agent.setConsoleProxyPort(CoreGlobalProperty.CONSOLE_PROXY_PORT)
        agent.setState(ConsoleProxyAgentState.Enabled)
        agent.setStatus(ConsoleProxyAgentStatus.Connecting)
        agent.setDescription(String.format("Console proxy agent running on the management node[uuid:%s]", Platform.getManagementServerId()))
        agent.setType(ConsoleConstants.MANAGEMENT_SERVER_CONSOLE_PROXY_TYPE)
        agent = dbf.persistAndRefresh(agent)

        // update console proxy agent
        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = "127.0.0.1"
        }

        assert Platform.getGlobalProperties().get("consoleProxyOverriddenIp") == '127.0.0.1'
        assert CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP == '127.0.0.1'
        //When the console port is 0 (empty), the default CoreGlobalProperty port 4900 is set
        assert Platform.getGlobalProperties().get("consoleProxyPort") == '4900'
        assert CoreGlobalProperty.CONSOLE_PROXY_PORT == 4900
        agent = dbf.reload(agent)
        assert agent.consoleProxyOverriddenIp == "127.0.0.1"
        assert agent.consoleProxyPort == 4900

        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = "127.0.0.1"
            consoleProxyPort = 4789
        }

        assert Platform.getGlobalProperties().get("consoleProxyPort") == '4789'
        assert CoreGlobalProperty.CONSOLE_PROXY_PORT == 4789
        agent = dbf.reload(agent)
        assert agent.consoleProxyPort == 4789
        
        // update console proxy agent by none admin account
        SessionInventory testAccountSession = logInByAccount {
            accountName = "test"
            password = "password"
        } as SessionInventory
        expect(AssertionError) {
            updateConsoleProxyAgent {
                sessionId = testAccountSession.uuid
                uuid = agent.uuid
                consoleProxyOverriddenIp = "127.0.0.1"
            }
        }

        // delete console proxy agent
        dbf.remove(agent)
    }

    @Override
    void clean() {
        env.delete()
    }
}
