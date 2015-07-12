package org.zstack.header.network.l3;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APISyncCallMessage;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:38 PM
 * To change this template use File | Settings | File Templates.
 */
@Action(category = L3NetworkConstant.ACTION_CATEGORY, names = {"read"})
public class APIGetL3NetworkTypesMsg extends APISyncCallMessage {
}
