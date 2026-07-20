package org.zstack.core.agent

import org.junit.Test
import org.zstack.header.rest.RESTFacade

class AgentManagerCommandUrlCase {
    @Test
    void testSelectedManagementNodeBuildsCommandUrl() {
        RESTFacade restf = [
                getSendCommandUrl: { "http://192.168.1.10:8080/zstack/asyncrest/sendcommand" },
                buildSendCommandUrl: { String host ->
                    assert host == "2001:db8::10"
                    "http://[2001:db8::10]:8080/zstack/asyncrest/sendcommand"
                }
        ] as RESTFacade

        assert AgentManagerImpl.buildCommandUrl(restf, "2001:db8::10") ==
                "http://[2001:db8::10]:8080/zstack/asyncrest/sendcommand"
        assert AgentManagerImpl.buildCommandUrl(restf, null) ==
                "http://192.168.1.10:8080/zstack/asyncrest/sendcommand"
    }
}
