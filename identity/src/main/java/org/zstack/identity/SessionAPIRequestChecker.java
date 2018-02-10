package org.zstack.identity;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.message.APIMessage;
import static org.zstack.core.Platform.*;

public class SessionAPIRequestChecker implements APIRequestChecker {
    @Override
    public void check(APIMessage msg) {
        if (msg.getSession() == null) {
            throw new ApiMessageInterceptionException(err(IdentityErrors.INVALID_SESSION,
                    "session of message[%s] is null", msg.getMessageName()));
        }

        if (msg.getSession().getUuid() == null) {
            throw new ApiMessageInterceptionException(err(IdentityErrors.INVALID_SESSION,
                    "session uuid is null"));
        }

        Session session = new Session();
        session.errorOnTimeout(msg.getSession().getUuid());
        msg.setSession(session.getSession(msg.getSession().getUuid()));
    }
}
