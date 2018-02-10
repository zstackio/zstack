package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import static org.zstack.core.Platform.*;

import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class Session {
    private static final CLogger logger = Utils.getLogger(Session.class);

    private static Map<String, SessionInventory> sessions = new ConcurrentHashMap<>();

    @Autowired
    private PluginRegistry pluginRgty;

    public void logout(String uuid) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                SessionInventory s = sessions.remove(uuid);
                if (s == null) {
                    SessionVO vo = findByUuid(uuid, SessionVO.class);
                    s = vo == null ? null : SessionInventory.valueOf(vo);
                }

                if (s == null) {
                    return;
                }

                SessionInventory finalS = s;
                pluginRgty.getExtensionList(SessionLogoutExtensionPoint.class).forEach(ext -> ext.sessionLogout(finalS));

                sql(SessionVO.class).eq(SessionVO_.uuid, uuid).hardDelete();
            }
        }.execute();
    }

    public void errorOnTimeout(String uuid) {
        new SQLBatch() {
            @Transactional(readOnly = true)
            private Timestamp getCurrentSqlDate() {
                Query query = databaseFacade.getEntityManager().createNativeQuery("select current_timestamp()");
                return (Timestamp) query.getSingleResult();
            }

            @Override
            protected void scripts() {
                SessionInventory s = getSession(uuid);
                if (s == null) {
                    throw new OperationFailureException(err(IdentityErrors.INVALID_SESSION,
                            "Session expired"));
                }

                Timestamp curr = getCurrentSqlDate();
                if (curr.after(s.getExpiredDate())) {
                    if (logger.isTraceEnabled()) {
                        logger.debug(String.format("session expired[%s < %s] for account[uuid:%s]", curr,
                                s.getExpiredDate(), s.getAccountUuid()));
                    }

                    logout(s.getUuid());
                    throw new OperationFailureException(err(IdentityErrors.INVALID_SESSION, "Session expired"));
                }
            }
        }.execute();
    }

    public SessionInventory getSession(String uuid) {
        return new SQLBatchWithReturn<SessionInventory>() {
            @Override
            protected SessionInventory scripts() {
                SessionInventory s = sessions.get(uuid);
                if (s == null) {
                    SessionVO vo = findByUuid(uuid, SessionVO.class);
                    if (vo == null) {
                        return null;
                    }

                    s = SessionInventory.valueOf(vo);
                    sessions.put(s.getUserUuid(), s);
                }

                return s;
            }
        }.execute();
    }
}
