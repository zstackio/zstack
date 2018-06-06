package org.zstack.identity;

import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
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
import java.util.concurrent.TimeUnit;

public class Session {
    private static final CLogger logger = Utils.getLogger(Session.class);

    private static Map<String, SessionInventory> sessions = new ConcurrentHashMap<>();

    public static SessionInventory login(String accountUuid, String userUuid) {
        return new SQLBatchWithReturn<SessionInventory>() {
            @Transactional(readOnly = true)
            private Timestamp getCurrentSqlDate() {
                Query query = databaseFacade.getEntityManager().createNativeQuery("select current_timestamp()");
                return (Timestamp) query.getSingleResult();
            }

            @Override
            protected SessionInventory scripts() {
                if (q(SessionVO.class).eq(SessionVO_.accountUuid, accountUuid)
                        .eq(SessionVO_.userUuid, userUuid).count() > IdentityGlobalConfig.MAX_CONCURRENT_SESSION.value(Integer.class)) {
                    throw new OperationFailureException(operr("Login sessions hit limit of max allowed concurrent login sessions"));
                }

                SessionVO vo = new SessionVO();
                vo.setUuid(Platform.getUuid());
                vo.setAccountUuid(accountUuid);
                vo.setUserUuid(userUuid);
                long expiredTime = getCurrentSqlDate().getTime() + TimeUnit.SECONDS.toMillis(IdentityGlobalConfig.SESSION_TIMEOUT.value(Long.class));
                vo.setExpiredDate(new Timestamp(expiredTime));
                persist(vo);
                reload(vo);
                SessionInventory inv = SessionInventory.valueOf(vo);
                sessions.put(inv.getUuid(), inv);

                return inv;
            }
        }.execute();
    }

    public static SessionInventory renewSession(String uuid, Long extendPeriod) {
        errorOnTimeout(uuid);

        if (extendPeriod == null) {
            extendPeriod = IdentityGlobalConfig.SESSION_TIMEOUT.value(Long.class);
        }

        Long finalExtendPeriod = extendPeriod;
        return new SQLBatchWithReturn<SessionInventory>() {
            @Transactional(readOnly = true)
            private Timestamp getCurrentSqlDate() {
                Query query = databaseFacade.getEntityManager().createNativeQuery("select current_timestamp()");
                return (Timestamp) query.getSingleResult();
            }

            @Override
            protected SessionInventory scripts() {
               Timestamp expiredDate = new Timestamp(TimeUnit.SECONDS.toMillis(finalExtendPeriod) + getCurrentSqlDate().getTime());
               SessionInventory s = getSession(uuid);
               s.setExpiredDate(expiredDate);

               sql(SessionVO.class).eq(SessionVO_.uuid, uuid).set(SessionVO_.expiredDate, expiredDate).update();

               return s;
            }
        }.execute();
    }

    public static void logout(String uuid) {
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
                Platform.getComponentLoader().getComponent(PluginRegistry.class)
                        .getExtensionList(SessionLogoutExtensionPoint.class)
                        .forEach(ext -> ext.sessionLogout(finalS));

                sql(SessionVO.class).eq(SessionVO_.uuid, uuid).hardDelete();
            }
        }.execute();
    }

    public static void errorOnTimeout(String uuid) {
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

    public static SessionInventory getSession(String uuid) {
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
                    sessions.put(s.getUuid(), s);
                }

                return s;
            }
        }.execute();
    }
}
