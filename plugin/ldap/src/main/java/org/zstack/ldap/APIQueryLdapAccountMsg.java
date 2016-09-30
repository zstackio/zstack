package org.zstack.ldap;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryLdapAccountReply.class, inventoryClass = LdapAccountRefInventory.class)
public class APIQueryLdapAccountMsg extends APIQueryMessage {
}
