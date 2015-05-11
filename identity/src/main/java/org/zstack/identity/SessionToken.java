package org.zstack.identity;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.zstack.header.message.APIMessage;

public class SessionToken extends AbstractAuthenticationToken {
    private final String sessionUuid;
    private final APIMessage msg;
    
    public SessionToken(String sessionUuid, APIMessage msg) {
        super(null);
        this.sessionUuid = sessionUuid;
        this.msg = msg;
    }

    @Override
    public Object getCredentials() {
        return sessionUuid;
    }

    @Override
    public Object getPrincipal() {
        return msg;
    }

}
