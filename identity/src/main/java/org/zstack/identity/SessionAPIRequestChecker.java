package org.zstack.identity;

import org.zstack.core.db.SQLBatch;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.SessionVO;
import org.zstack.header.message.APIMessage;
import static org.zstack.core.Platform.err;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionAPIRequestChecker implements APIRequestChecker {
    private static Map<String, SessionInventory> sessions = new ConcurrentHashMap<>();

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

        new SQLBatch() {
            @Override
            protected void scripts() {
                SessionInventory s = sessions.get(msg.getSession().getUserUuid());
                if (s == null) {
                    SessionVO vo = findByUuid(msg.getSession().getUuid(), SessionVO.class);
                    if (vo == null) {
                        throw new OperationFailureException(err(IdentityErrors.INVALID_SESSION,
                                "Session expired"));
                    }

                    s = SessionInventory.valueOf(vo);
                    sessions.put(s.getUserUuid(), s);
                }
            }
        }.execute();
    }
}
