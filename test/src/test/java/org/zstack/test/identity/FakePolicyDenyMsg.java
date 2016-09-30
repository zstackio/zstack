package org.zstack.test.identity;

import org.zstack.header.configuration.NoPython;
import org.zstack.header.identity.NeedRoles;
import org.zstack.header.message.APIMessage;

@NeedRoles(roles = {FakeAuthorizationServiceForRoleTest.DENY_POLICY_ROLE})
@NoPython
public class FakePolicyDenyMsg extends APIMessage {

}
