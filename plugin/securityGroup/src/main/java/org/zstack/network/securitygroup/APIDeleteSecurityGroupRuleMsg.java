package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @api
 * api event for :ref:`APIDeleteSecurityGroupRuleEvent`
 *
 * @category security group
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.securitygroup.APIDeleteSecurityGroupRuleMsg": {
"ruleUuids": [
"3f8e32673f2a429dbed7ea3e1041dd43"
],
"session": {
"uuid": "cdfd07ea63c043c998299decdf03ea58"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.securitygroup.APIDeleteSecurityGroupRuleMsg": {
"ruleUuids": [
"3f8e32673f2a429dbed7ea3e1041dd43"
],
"session": {
"uuid": "cdfd07ea63c043c998299decdf03ea58"
},
"timeout": 1800000,
"id": "c320ad5f3c2f40879ad188ec65784f00",
"serviceId": "api.portal"
}
}
 *
 * @result
 * see :ref:`APIDeleteSecurityGroupRuleEvent`
 */
@Action(category = SecurityGroupConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/security-groups/rules",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteSecurityGroupRuleEvent.class
)
public class APIDeleteSecurityGroupRuleMsg extends APIMessage {
    /**
     * @desc a list of rule uuid
     */
    @APIParam(nonempty = true, checkAccount = true, operationTarget = true)
    private List<String> ruleUuids;

    public List<String> getRuleUuids() {
        return ruleUuids;
    }

    public void setRuleUuids(List<String> ruleUuids) {
        this.ruleUuids = ruleUuids;
    }
 
    public static APIDeleteSecurityGroupRuleMsg __example__() {
        APIDeleteSecurityGroupRuleMsg msg = new APIDeleteSecurityGroupRuleMsg();
        msg.setRuleUuids(asList(uuid()));
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;
        StringBuilder str = new StringBuilder();
        for (String s:ruleUuids){
            str.append(String.format("ruleUuid:%s ",s));
        }

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Deleted").resource(str.toString(),SecurityGroupRuleVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }

}
