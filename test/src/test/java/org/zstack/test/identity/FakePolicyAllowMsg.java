package org.zstack.test.identity;

import org.zstack.header.configuration.NoPython;
import org.zstack.header.message.APIMessage;

@NeedRoles(roles = {FakeAuthorizationServiceForRoleTest.ALLOW_POLICY_ROLE})
@NoPython
public class FakePolicyAllowMsg extends APIMessage {

}
