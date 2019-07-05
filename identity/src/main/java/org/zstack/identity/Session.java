package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import static org.zstack.core.Platform.*;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Session implements Component {
    private static final CLogger logger = Utils.getLogger(Session.class);

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;

    private Future<Void> expiredSessionCollector;

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
        SessionInventory session = new SQLBatchWithReturn<SessionInventory>() {
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

        return session;
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

    public static Map<String, SessionInventory> getSessionsCopy() {
        return new HashMap<>(sessions);
    }

    public static SessionInventory getSession(String uuid) {
        SessionInventory s = sessions.get(uuid);
        if (s == null) {
            SessionVO vo = Q.New(SessionVO.class).eq(SessionVO_.uuid, uuid).find();
            if (vo == null) {
                return null;
            }

            s = SessionInventory.valueOf(vo);
            sessions.put(s.getUuid(), s);
        }

        return s;
    }

    @Override
    public boolean start() {
        startCleanUpStaleSessionTask();
        setupCanonicalEvents();
        return true;
    }

    private void startCleanUpStaleSessionTask() {
        final int interval = IdentityGlobalConfig.SESSION_CLEANUP_INTERVAL.value(Integer.class);
        expiredSessionCollector = thdf.submitPeriodicTask(new PeriodicTask() {
            @Transactional
            private List<String> deleteExpiredSessions() {
                String sql = "select s.uuid from SessionVO s where CURRENT_TIMESTAMP  >= s.expiredDate";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                List<String> uuids = q.getResultList();
                if (!uuids.isEmpty()) {
                    String dsql = "delete from SessionVO s where s.uuid in :uuids";
                    Query dq = dbf.getEntityManager().createQuery(dsql);
                    dq.setParameter("uuids", uuids);
                    dq.executeUpdate();
                }
                return uuids;
            }

            @Transactional(readOnly = true)
            private Timestamp getCurrentSqlDate() {
                Query query = dbf.getEntityManager().createNativeQuery("select current_timestamp()");
                return (Timestamp) query.getSingleResult();
            }

            private void deleteExpiredCachedSessions() {
                Timestamp curr = getCurrentSqlDate();
                sessions.entrySet().removeIf(entry -> curr.after(entry.getValue().getExpiredDate()));
            }

            @Override
            public void run() {
                List<String> uuids = deleteExpiredSessions();
                for (String uuid : uuids) {
                    sessions.remove(uuid);
                }

                deleteExpiredCachedSessions();
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return interval;
            }

            @Override
            public String getName() {
                return "ExpiredSessionCleanupThread";
            }

        });
    }

    @Override
    public boolean stop() {
        if (expiredSessionCollector != null) {
            expiredSessionCollector.cancel(true);
        }

        return true;
    }

    private void setupCanonicalEvents() {
        evtf.on(IdentityCanonicalEvents.ACCOUNT_DELETED_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                // as a foreign key would clean SessionVO after account deleted, just clean memory sessions here
                removeMemorySessionsAccordingToDB(tokens, data);
                removeMemorySessionsAccordingToAccountUuid(tokens, data);
            }

            private void removeMemorySessionsAccordingToDB(Map tokens, Object data) {
                IdentityCanonicalEvents.AccountDeletedData d = (IdentityCanonicalEvents.AccountDeletedData) data;

                SimpleQuery<SessionVO> q = dbf.createQuery(SessionVO.class);
                q.select(SessionVO_.uuid);
                q.add(SessionVO_.accountUuid, SimpleQuery.Op.EQ, d.getAccountUuid());
                List<String> suuids = q.listValue();

                for (String uuid : suuids) {
                    logout(uuid);
                }

                if (!suuids.isEmpty()) {
                    logger.debug(String.format("successfully removed %s sessions for the deleted account[%s]",
                            suuids.size(),
                            d.getAccountUuid()));
                }
            }

            private void removeMemorySessionsAccordingToAccountUuid(Map tokens, Object data) {
                IdentityCanonicalEvents.AccountDeletedData d = (IdentityCanonicalEvents.AccountDeletedData) data;

                List<String> suuids = sessions.entrySet().stream()
                        .filter(it -> it.getValue().getAccountUuid().equals(d.getAccountUuid()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

                for (String uuid : suuids) {
                    logout(uuid);
                }

                if (!suuids.isEmpty()) {
                    logger.debug(String.format("successfully removed %s sessions for the deleted account[%s]",
                            suuids.size(),
                            d.getAccountUuid()));
                }
            }
        });

        evtf.on(IdentityCanonicalEvents.USER_DELETED_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                IdentityCanonicalEvents.UserDeletedData d = (IdentityCanonicalEvents.UserDeletedData) data;

                SimpleQuery<SessionVO> q = dbf.createQuery(SessionVO.class);
                q.select(SessionVO_.uuid);
                q.add(SessionVO_.userUuid, SimpleQuery.Op.EQ, d.getUserUuid());
                List<String> suuids = q.listValue();

                for (String uuid : suuids) {
                    logout(uuid);
                }

                if (!suuids.isEmpty()) {
                    logger.debug(String.format("successfully removed %s sessions for the deleted user[%s]", suuids.size(),
                            d.getUserUuid()));
                }
            }
        });
    }
}
