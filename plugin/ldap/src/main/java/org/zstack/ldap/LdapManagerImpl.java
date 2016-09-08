package org.zstack.ldap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.identity.AccountManager;
import org.zstack.identity.IdentityGlobalConfig;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by miao on 16-9-6.
 */
public class LdapManagerImpl extends AbstractService implements LdapManager {
    private static final CLogger logger = Utils.getLogger(LdapManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private LdapTemplate ldapTemplate;
    private LdapContextSource ldapContextSource;

    @Transactional(readOnly = true)
    public LdapServerVO getLdapServer() {
        SimpleQuery<LdapServerVO> sq = dbf.createQuery(LdapServerVO.class);
        List<LdapServerVO> ldapServers = sq.list();
        if (ldapServers.isEmpty()) {
            throw new CloudRuntimeException("No ldap server record in database.");
        }
        if (ldapServers.size() > 1) {
            throw new CloudRuntimeException("More than one ldap server record in database.");
        }
        return ldapServers.get(0);
    }

    public void readLdapServerConfiguration() {
        LdapServerVO ldapServer = getLdapServer();

        ldapContextSource = new LdapContextSource();
        ldapContextSource.setUrl(ldapServer.getUrl());
        ldapContextSource.setBase(ldapServer.getBase());
        ldapContextSource.setUserDn(ldapServer.getUsername());
        ldapContextSource.setPassword(ldapServer.getPassword());

        ldapTemplate = new LdapTemplate();
        ldapTemplate.setContextSource(ldapContextSource);

        try {
            ldapContextSource.afterPropertiesSet();
            logger.info("LDAP Context Source loaded ");
        } catch (Exception e) {
            logger.error("LDAP Context Source not loaded ", e);
            throw new CloudRuntimeException("LDAP Context Source not loaded", e);
        }
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {


        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {

        if (msg instanceof APILoginByLdapMsg) {
            handle((APILoginByLdapMsg) msg);
        } else if (msg instanceof APIAddLdapServerMsg) {
            handle((APIAddLdapServerMsg) msg);
        } else if (msg instanceof APIDeleteLdapServerMsg) {
            handle((APIDeleteLdapServerMsg) msg);
        } else if (msg instanceof APIBindLdapAccountMsg) {
            handle((APIBindLdapAccountMsg) msg);
        } else if (msg instanceof APIUnbindLdapAccountMsg) {
            handle((APIUnbindLdapAccountMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }


    public boolean isValid(String uid, String password) {
        try {
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("uid", uid));
            boolean valid = ldapTemplate.authenticate(getDnByUid(uid), filter.toString(), password);
            logger.info(String.format("isValid success userName:%s, isValid:%s", uid, valid));
            return valid;
        } catch (NamingException e) {
            logger.info("isValid fail userName:" + uid, e);
            return false;
        } catch (Exception e) {
            logger.info("isValid error userName" + uid, e);
            return false;
        }
    }

    @Transactional
    private LdapAccountRefInventory bindLdapAccount(String accountUuid, String ldapUid) {
        LdapAccountRefVO ref = new LdapAccountRefVO();
        ref.setUuid(Platform.getUuid());
        ref.setAccountUuid(accountUuid);
        ref.setLdapServerUuid(getLdapServer().getUuid());
        ref.setLdapUid(ldapUid);
        ref = dbf.persistAndRefresh(ref);
        return LdapAccountRefInventory.valueOf(ref);
    }

    public String getDnByUid(String uid) {
        return getUserDn("uid", uid).replace("," + ldapContextSource.getBaseLdapPathAsString(), "");
    }

    public String getUserDn(String key, String val) {
        String dn = "";
        try {
            EqualsFilter f = new EqualsFilter(key, val);
            List<Object> result = ldapTemplate.search("", f.toString(), new AbstractContextMapper<Object>() {
                @Override
                protected Object doMapFromContext(DirContextOperations ctx) {
                    return ctx.getNameInNamespace();
                }
            });
            if (result.size() == 1) {
                dn = result.get(0).toString();
            }
            logger.info(String.format("getDn success key:%s, val : %s, dn: %s", key, val, dn));
        } catch (NamingException e) {
            logger.error(String.format("getDn error key:%s, val : %s", key, val), e);
        } catch (Exception e) {
            logger.error(String.format("getDn error key:%s, val : %s", key, val), e);
        }
        return dn;
    }

    public String getId() {
        return bus.makeLocalServiceId(LdapConstant.SERVICE_ID);
    }

    private SessionInventory getSession(String accountUuid, String userUuid) {
        int maxLoginTimes = org.zstack.identity.IdentityGlobalConfig.MAX_CONCURRENT_SESSION.value(Integer.class);
        SimpleQuery<SessionVO> query = dbf.createQuery(SessionVO.class);
        query.add(SessionVO_.accountUuid, SimpleQuery.Op.EQ, accountUuid);
        query.add(SessionVO_.userUuid, SimpleQuery.Op.EQ, userUuid);
        long count = query.count();
        if (count >= maxLoginTimes) {
            String err = String.format("Login sessions hit limit of max allowed concurrent login sessions, max allowed: %s", maxLoginTimes);
            throw new BadCredentialsException(err);
        }

        int sessionTimeout = IdentityGlobalConfig.SESSION_TIMEOUT.value(Integer.class);
        SessionVO svo = new SessionVO();
        svo.setUuid(Platform.getUuid());
        svo.setAccountUuid(accountUuid);
        svo.setUserUuid(userUuid);
        long expiredTime = getCurrentSqlDate().getTime() + TimeUnit.SECONDS.toMillis(sessionTimeout);
        svo.setExpiredDate(new Timestamp(expiredTime));
        svo = dbf.persistAndRefresh(svo);
        SessionInventory session = SessionInventory.valueOf(svo);
        return session;
    }

    @Transactional(readOnly = true)
    private Timestamp getCurrentSqlDate() {
        Query query = dbf.getEntityManager().createNativeQuery("select current_timestamp()");
        return (Timestamp) query.getSingleResult();
    }

    public boolean start() {
        readLdapServerConfiguration();
        return true;
    }

    public boolean stop() {
        return true;
    }

    private void handle(APILoginByLdapMsg msg) {
        APILogInReply reply = new APILogInReply();

        SimpleQuery<LdapAccountRefVO> q = dbf.createQuery(LdapAccountRefVO.class);
        q.add(LdapAccountRefVO_.ldapUid, SimpleQuery.Op.EQ, msg.getUid());
        LdapAccountRefVO vo = q.find();
        if (vo == null) {
            reply.setError(errf.instantiateErrorCode(IdentityErrors.AUTHENTICATION_ERROR,
                    "No LdapAccountRef Exist."));
            bus.reply(msg, reply);
            return;
        }
        if (isValid(vo.getLdapUid(), msg.getPassword())) {
            reply.setInventory(getSession(vo.getAccountUuid(), vo.getUuid()));
        } else {
            reply.setError(errf.instantiateErrorCode(IdentityErrors.AUTHENTICATION_ERROR,
                    "Login Failed."));
        }
        bus.reply(msg, reply);
    }

    private void handle(APIAddLdapServerMsg msg) {
        APIAddLdapServerEvent evt = new APIAddLdapServerEvent(msg.getId());

        SimpleQuery<LdapServerVO> sq = dbf.createQuery(LdapServerVO.class);
        List<LdapServerVO> ldapServers = sq.list();
        if (ldapServers.isEmpty()) {
            LdapServerVO ldapServerVO = new LdapServerVO();
            ldapServerVO.setUuid(Platform.getUuid());
            ldapServerVO.setName(msg.getName());
            ldapServerVO.setDescription(msg.getDescription());
            ldapServerVO.setUrl(msg.getUrl());
            ldapServerVO.setBase(msg.getBase());
            ldapServerVO.setUsername(msg.getUsername());
            ldapServerVO.setPassword(msg.getPassword());

            ldapServerVO = dbf.persistAndRefresh(ldapServerVO);
            LdapServerInventory inv = LdapServerInventory.valueOf(ldapServerVO);
            evt.setInventory(inv);

            readLdapServerConfiguration();
        } else {
            evt.setErrorCode(errf.stringToOperationError("There has been a ldap server record. " +
                    "You'd better remove it before add a new one!"));
        }


        bus.publish(evt);
    }

    private void handle(APIDeleteLdapServerMsg msg) {
        APIDeleteLdapServerEvent evt = new APIDeleteLdapServerEvent(msg.getId());

        dbf.removeByPrimaryKey(msg.getUuid(), LdapServerVO.class);
        readLdapServerConfiguration();
        
        bus.publish(evt);
    }

    private void handle(APIBindLdapAccountMsg msg) {
        APIBindLdapAccountEvent evt = new APIBindLdapAccountEvent(msg.getId());

        if (getDnByUid(msg.getLdapUid()).equals("")) {
            throw new OperationFailureException(errf.stringToOperationError("cannot find uid on ldap server."));
        }
        evt.setInventory(bindLdapAccount(msg.getAccountUuid(), msg.getLdapUid()));

        bus.publish(evt);
    }

    private void handle(APIUnbindLdapAccountMsg msg) {
        APIUnbindLdapAccountEvent evt = new APIUnbindLdapAccountEvent(msg.getId());

        dbf.removeByPrimaryKey(msg.getUuid(), LdapAccountRefVO.class);

        bus.publish(evt);
    }


}
