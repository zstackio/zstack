package org.zstack.ldap;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryLdapServerReply.class, inventoryClass = LdapServerInventory.class)
public class APIQueryLdapServerMsg extends APIQueryMessage {
}
