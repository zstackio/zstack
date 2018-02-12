package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
@RestRequest(
        path = "/accounts/policies",
        method = HttpMethod.POST,
        responseClass = APICreatePolicyEvent.class,
        parameterName = "params"
)
public class APICreatePolicyMsg extends APICreateMessage implements AccountMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(nonempty = true)
    private List<Statement> statements;

    public List<Statement> getStatements() {
        return statements;
    }

    public void setStatements(List<Statement> statements) {
        this.statements = statements;
    }

    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
 
    public static APICreatePolicyMsg __example__() {
        APICreatePolicyMsg msg = new APICreatePolicyMsg();

        msg.setName("USER-RESET-PASSWORD");

        Statement s = new Statement();
        s.setName(String.format("user-reset-password-%s", uuid()));
        s.setEffect(AccountConstant.StatementEffect.Allow);
        s.addAction(String.format("%s:%s", AccountConstant.ACTION_CATEGORY, APIUpdateUserMsg.class.getSimpleName()));
        msg.setStatements(list(s));

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Creating").resource(((APICreatePolicyEvent)evt).getInventory().getUuid(), PolicyVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
                }
            }
        };
    }
}
