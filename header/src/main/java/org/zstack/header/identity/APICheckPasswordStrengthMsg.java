package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(category = AccountConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/accounts/actions",
        responseClass = APICheckPasswordStrengthEvent.class,
        method = HttpMethod.PUT,
        isAction = true
)
public class APICheckPasswordStrengthMsg extends APIMessage {
    @APIParam(maxLength = 255)
    private String accountName;

    @APIParam(maxLength = 255)
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
}
