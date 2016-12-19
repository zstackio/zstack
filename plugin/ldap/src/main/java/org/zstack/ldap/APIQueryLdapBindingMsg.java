package org.zstack.ldap;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryLdapBindingReply.class, inventoryClass = LdapAccountRefInventory.class)
public class APIQueryLdapBindingMsg extends APIQueryMessage {
}
