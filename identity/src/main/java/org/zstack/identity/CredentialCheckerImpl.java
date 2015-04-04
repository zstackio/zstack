package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.*;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CredentialCheckerImpl implements CredentialChecker, AuthenticationProvider, Component {
    private static final CLogger logger = Utils.getLogger(CredentialChecker.class);

    @Autowired
    private AuthenticationManager authMgr;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccessDecisionManager accessMgr;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ErrorFacade errf;

    private Future<Void> expiredSessionCollector;
    private Map<String, SessionInventory> sessions = Collections.synchronizedMap(new HashMap<String, SessionInventory>());

    @Override
    public void authenticateAndAuthorize(APIMessage msg, AuthorizationInfo ainfo) throws CredentialDeniedException {
        if (msg.getSession() == null) {
            ErrorCode err = errf.instantiateErrorCode(IdentityErrors.INVALID_SESSION,
                    String.format("session of message[%s] is null", msg.getMessageName()));
            throw new CredentialDeniedException(err);
        }

        SessionToken token = new SessionToken(msg.getSession().getUuid(), msg);
        try {
            token = (SessionToken) authMgr.authenticate(token);
            List<ConfigAttribute> configs = new ArrayList<ConfigAttribute>(1);
            configs.add(ainfo);
            accessMgr.decide(token, msg, configs);
        } catch (Exception e) {
            logger.trace("", e);
            throw new CredentialDeniedException(errf.instantiateErrorCode(IdentityErrors.AUTHENTICATION_ERROR, e.getMessage()));
        }

    }

    @Override
    public Authentication authenticate(Authentication token) throws AuthenticationException {
        try {
            if (token instanceof UsernamePasswordAuthenticationToken) {
                authenticateByUsernamePassword((UsernamePasswordAuthenticationToken) token);
            } else {
                authenticateBySession((SessionToken) token);
            }
            return token;
        } catch (BadCredentialsException be) {
            throw be;
        } catch (Exception e) {
            logger.debug("", e);
            throw new BadCredentialsException(e.getMessage());
        }

    }

    private void authenticateBySession(SessionToken token) {
        APIMessage msg = (APIMessage) token.getPrincipal();
        SessionInventory session = sessions.get(msg.getSession().getUuid());
        
        if (msg.getSession().getUuid() == null) {
            throw new BadCredentialsException("session uuid cannot be null");
        }
        
        if (session == null) {
            SessionVO svo = dbf.findByUuid(msg.getSession().getUuid(), SessionVO.class);
            if (svo == null) {
                throw new BadCredentialsException("Session expired");
            }
            session = SessionInventory.valueOf(svo);
            sessions.put(session.getUuid(), session);
        }
        
        Timestamp curr = getCurrentSqlDate();
        if (curr.after(session.getExpiredDate())) {
            logger.debug(String.format("session expired[%s < %s] for account[uuid:%s]", curr, session.getExpiredDate(), session.getAccountUuid()));
            logOutSession(session.getUuid());
            throw new BadCredentialsException("Session expired");
        }

        msg.setSession(session);
        UserVO user = dbf.findByUuid(session.getUserUuid(), UserVO.class);
        token.setDetails(user);
    }

    private void authenticateByUsernamePassword(UsernamePasswordAuthenticationToken token) {
        CredentialObject co = (CredentialObject) token.getCredentials();
        SimpleQuery<UserVO> q = dbf.createQuery(UserVO.class);
        q.add(UserVO_.name, Op.EQ, co.getUserName());
        if (!co.isAuthenticatedByAccountName()) {
            q.add(UserVO_.accountUuid, Op.EQ, co.getAccountUuid());
        }
        UserVO user = q.find();
        if (user == null) {
            String err = co.isAuthenticatedByAccountName() ? String.format("Unable to find account[name: %s]", co.getUserName()) : String.format(
                    "Unable to find user[name: %s] in account[uuid:%s]", co.getUserName(), co.getAccountUuid());
            logger.warn(err);
            throw new BadCredentialsException(String.format("Username/Password is incorrect"));
        }

        if (!user.getPassword().equals(co.getPassword())) {
            logger.debug(String.format("Password for user[name:%s] is not correct", user.getName()));
            throw new BadCredentialsException(String.format("Username/Password is incorrect"));
        }
        
        token.setDetails(user);
    }

    @Override
    public boolean supports(Class<?> arg0) {
        if (arg0 == UsernamePasswordAuthenticationToken.class || arg0 == SessionToken.class) {
            return true;
        }
        return false;
    }

    private SessionInventory getSession(UserVO user) {
        int maxLoginTimes = org.zstack.identity.IdentityGlobalConfig.MAX_CONCURRENT_SESSION.value(Integer.class);
        SimpleQuery<SessionVO> query = dbf.createQuery(SessionVO.class);
        query.add(SessionVO_.accountUuid, Op.EQ, user.getAccountUuid());
        query.add(SessionVO_.userUuid, Op.EQ, user.getUuid());
        long count = query.count();
        if (count >= maxLoginTimes) {
            String err = String.format("Login sessions hit limit of max allowed concurrent login sessions, max allowed: %s", maxLoginTimes);
            throw new BadCredentialsException(err);
        }

        return createNewSession(user);
    }

    @Transactional(readOnly = true)
    private Timestamp getCurrentSqlDate() {
        Query query = dbf.getEntityManager().createNativeQuery("select current_timestamp()");
        return (Timestamp) query.getSingleResult();
    }

    private SessionInventory createNewSession(UserVO user) {
        int sessionTimeout = IdentityGlobalConfig.SESSION_TIMEOUT.value(Integer.class);
        SessionVO svo = new SessionVO();
        svo.setUuid(Platform.getUuid());
        svo.setAccountUuid(user.getAccountUuid());
        svo.setUserUuid(user.getUuid());
        long expiredTime = getCurrentSqlDate().getTime() + TimeUnit.SECONDS.toMillis(sessionTimeout);
        svo.setExpiredDate(new Timestamp(expiredTime));
        svo = dbf.persistAndRefresh(svo);
        return SessionInventory.valueOf(svo);
    }

    @Override
    public SessionInventory authenticateByAccount(String accountName, String password) throws CredentialDeniedException {
        CredentialObject co = new CredentialObject();
        co.setUserName(accountName);
        co.setPassword(password);
        co.setAuthenticatedByAccountName(true);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(null, co);
        authMgr.authenticate(token);
        return getSession((UserVO) token.getDetails());
    }

    @Override
    public SessionInventory authenticateByUser(String accountUuid, String userName, String password) throws CredentialDeniedException {
        CredentialObject co = new CredentialObject();
        co.setAccountUuid(accountUuid);
        co.setPassword(password);
        co.setUserName(userName);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(null, co);
        authMgr.authenticate(token);
        return getSession((UserVO) token.getDetails());
    }

    @Override
    public void validateSession(APIValidateSessionMsg msg) throws BadCredentialsException{
        SessionToken token = new SessionToken(msg.getSessionUuid(), msg);
        SessionInventory session = new SessionInventory();
        session.setUuid(msg.getSessionUuid());
        msg.setSession(session);
        this.authenticateBySession(token);
    }

    @Override
    public void logOutSession(String sessionUuid) {
        sessions.remove(sessionUuid);
        dbf.removeByPrimaryKey(sessionUuid, SessionVO.class);
    }

    @Override
    public boolean start() {
        final int interval = IdentityGlobalConfig.SESSION_CELANUP_INTERVAL.value(Integer.class);
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
            
            @Override
            public void run() {
                List<String> uuids = deleteExpiredSessions();
                for (String uuid : uuids) {
                    sessions.remove(uuid);
                }
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
        return true;
    }

    @Override
    public boolean stop() {
        if (expiredSessionCollector != null) {
            expiredSessionCollector.cancel(false);
        }
        return true;
    }
}
