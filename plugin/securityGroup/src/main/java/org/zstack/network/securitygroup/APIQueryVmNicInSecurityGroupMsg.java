package org.zstack.network.securitygroup;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;

@Action(category = SecurityGroupConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryVmNicInSecurityGroupMsg extends APIQueryMessage {

}
