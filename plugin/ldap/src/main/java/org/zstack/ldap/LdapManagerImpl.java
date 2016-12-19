package org.zstack.ldap;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.orm.jpa.JpaSystemException;
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
import java.util.ArrayList;
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


    @Transactional(readOnly = true)
    private LdapServerVO getLdapServer() {
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

    private LdapTemplateContextSource readLdapServerConfiguration() {
        LdapServerVO ldapServerVO = getLdapServer();
        LdapServerInventory ldapServerInventory = LdapServerInventory.valueOf(ldapServerVO);
        return new LdapUtil().loadLdap(ldapServerInventory);
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

        if (msg instanceof APILogInByLdapMsg) {
            handle((APILogInByLdapMsg) msg);
        } else if (msg instanceof APIAddLdapServerMsg) {
            handle((APIAddLdapServerMsg) msg);
        } else if (msg instanceof APIDeleteLdapServerMsg) {
            handle((APIDeleteLdapServerMsg) msg);
        } else if (msg instanceof APICreateLdapBindingMsg) {
            handle((APICreateLdapBindingMsg) msg);
        } else if (msg instanceof APIDeleteLdapBindingMsg) {
            handle((APIDeleteLdapBindingMsg) msg);
        } else if (msg instanceof APIUpdateLdapServerMsg) {
            handle((APIUpdateLdapServerMsg) msg);
        } else if (msg instanceof APICleanInvalidLdapBindingMsg) {
            handle((APICleanInvalidLdapBindingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }


    public boolean isValid(String uid, String password) {
        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration();
        try {
            boolean valid;
            String fullUserDn = getFullUserDn(ldapTemplateContextSource.getLdapTemplate(), "uid", uid);
            if (fullUserDn.equals("") || password.equals("")) {
                return false;
            }
            LdapServerVO ldapServerVO = getLdapServer();
            LdapServerInventory ldapServerInventory = LdapServerInventory.valueOf(ldapServerVO);
            ldapServerInventory.setUsername(fullUserDn);
            ldapServerInventory.setPassword(password);
            LdapTemplateContextSource ldapTemplateContextSource2 = new LdapUtil().loadLdap(ldapServerInventory);

            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter(fullUserDn.split(",")[0].split("=")[0], fullUserDn.split(",")[0].split("=")[1]));
            valid = ldapTemplateContextSource2.getLdapTemplate().
                    authenticate("", filter.toString(), password);
            logger.info(String.format("isValid[userName:%s, dn:%s, valid:%s]", uid, fullUserDn, valid));
            return valid;
        } catch (NamingException e) {
            logger.info("isValid fail userName:" + uid, e);
            return false;
        } catch (Exception e) {
            logger.info("isValid error userName:" + uid, e);
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

    private String getPartialUserDnByUid(LdapTemplateContextSource ldapTemplateContextSource, String uid) {
        return getFullUserDn(ldapTemplateContextSource.getLdapTemplate(), "uid", uid).
                replace("," + ldapTemplateContextSource.getLdapContextSource().getBaseLdapPathAsString(), "");
    }

    private String getFullUserDn(LdapTemplate ldapTemplate, String key, String val) {
        String dn;
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
            } else if (result.size() > 1) {
                throw new OperationFailureException(errf.instantiateErrorCode(
                        LdapErrors.UNABLE_TO_GET_SPECIFIED_LDAP_UID, "More than one ldap search result"));
            } else {
                return "";
            }
            logger.info(String.format("getDn success key:%s, val:%s, dn:%s", key, val, dn));
        } catch (NamingException e) {
            LdapServerVO ldapServerVO = getLdapServer();
            String errString = String.format(
                    "You'd better check the ldap server[url:%s, baseDN:%s, encryption:%s, username:%s, password:******]" +
                            " configuration and test connection first.getDn error key:%s, val:%s",
                    ldapServerVO.getUrl(), ldapServerVO.getBase(),
                    ldapServerVO.getEncryption(), ldapServerVO.getUsername(), key, val);
            throw new OperationFailureException(errf.instantiateErrorCode(
                    LdapErrors.UNABLE_TO_GET_SPECIFIED_LDAP_UID, errString));
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
        return true;
    }

    public boolean stop() {
        return true;
    }

    private void handle(APILogInByLdapMsg msg) {
        APILogInByLdapReply reply = new APILogInByLdapReply();

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
            reply.setInventory(getSession(vo.getAccountUuid(), vo.getAccountUuid()));

            SimpleQuery<AccountVO> sq = dbf.createQuery(AccountVO.class);
            sq.add(AccountVO_.uuid, SimpleQuery.Op.EQ, vo.getAccountUuid());
            AccountVO avo = sq.find();
            if (avo == null) {
                throw new CloudRuntimeException(String.format("Account[uuid:%s] Not Found!!!", vo.getAccountUuid()));
            }
            reply.setAccountInventory(AccountInventory.valueOf(avo));
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
            ldapServerVO.setEncryption(msg.getEncryption());

            ldapServerVO = dbf.persistAndRefresh(ldapServerVO);
            LdapServerInventory inv = LdapServerInventory.valueOf(ldapServerVO);
            evt.setInventory(inv);
        } else {
            evt.setErrorCode(errf.instantiateErrorCode(LdapErrors.MORE_THAN_ONE_LDAP_SERVER,
                    "There has been a ldap server record. " +
                            "You'd better remove it before adding a new one!"));
        }


        bus.publish(evt);
    }

    private void handle(APIDeleteLdapServerMsg msg) {
        APIDeleteLdapServerEvent evt = new APIDeleteLdapServerEvent(msg.getId());

        dbf.removeByPrimaryKey(msg.getUuid(), LdapServerVO.class);

        bus.publish(evt);
    }

    private void handle(APICreateLdapBindingMsg msg) {
        APICreateLdapBindingEvent evt = new APICreateLdapBindingEvent(msg.getId());

        // account check
        SimpleQuery<AccountVO> sq = dbf.createQuery(AccountVO.class);
        sq.add(AccountVO_.uuid, SimpleQuery.Op.EQ, msg.getAccountUuid());
        AccountVO avo = sq.find();
        if (avo == null) {
            evt.setErrorCode(errf.instantiateErrorCode(LdapErrors.CANNOT_FIND_ACCOUNT,
                    String.format("cannot find the specified account[uuid:%s]", msg.getAccountUuid())));
            bus.publish(evt);
            return;
        }
        if (avo.getType().equals(AccountType.SystemAdmin)) {
            evt.setErrorCode(errf.instantiateErrorCode(LdapErrors.CANNOT_BIND_ADMIN_ACCOUNT,
                    "cannot bind ldap uid to admin account."));
            bus.publish(evt);
            return;
        }

        // bind op
        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration();
        if (getPartialUserDnByUid(ldapTemplateContextSource, msg.getLdapUid()).equals("")) {
            throw new OperationFailureException(errf.instantiateErrorCode(LdapErrors.UNABLE_TO_GET_SPECIFIED_LDAP_UID,
                    String.format("cannot find uid[%s] on ldap server[Address:%s, BaseDN:%s].", msg.getLdapUid(),
                            String.join(", ", ldapTemplateContextSource.getLdapContextSource().getUrls()),
                            ldapTemplateContextSource.getLdapContextSource().getBaseLdapPathAsString())));
        }
        try {
            evt.setInventory(bindLdapAccount(msg.getAccountUuid(), msg.getLdapUid()));
        } catch (JpaSystemException e) {
            if (e.getRootCause() instanceof MySQLIntegrityConstraintViolationException) {
                evt.setErrorCode(errf.instantiateErrorCode(LdapErrors.BIND_SAME_LDAP_UID_TO_MULTI_ACCOUNT,
                        "The ldap uid has been bound to an account. "));
            } else {
                throw e;
            }
        }
        bus.publish(evt);
    }

    private void handle(APIDeleteLdapBindingMsg msg) {
        APIDeleteLdapBindingEvent evt = new APIDeleteLdapBindingEvent(msg.getId());

        dbf.removeByPrimaryKey(msg.getUuid(), LdapAccountRefVO.class);

        bus.publish(evt);
    }


    @Transactional
    private void handle(APICleanInvalidLdapBindingMsg msg) {
        APICleanInvalidLdapBindingEvent evt = new APICleanInvalidLdapBindingEvent(msg.getId());

        ArrayList<String> accountUuidList = new ArrayList<>();
        ArrayList<String> ldapAccountRefUuidList = new ArrayList<>();
        SimpleQuery<LdapAccountRefVO> sq = dbf.createQuery(LdapAccountRefVO.class);
        List<LdapAccountRefVO> refList = sq.list();
        if (refList != null && !refList.isEmpty()) {
            LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration();
            for (LdapAccountRefVO ldapAccRefVO : refList) {
                if (getPartialUserDnByUid(ldapTemplateContextSource, ldapAccRefVO.getLdapUid()).equals("")) {
                    accountUuidList.add(ldapAccRefVO.getAccountUuid());
                    ldapAccountRefUuidList.add(ldapAccRefVO.getUuid());
                }
            }
        }
        if (!accountUuidList.isEmpty()) {
            // remove ldap bindings
            dbf.removeByPrimaryKeys(ldapAccountRefUuidList, LdapAccountRefVO.class);
            // return accounts of which ldap bindings had been removed
            SimpleQuery<AccountVO> sq1 = dbf.createQuery(AccountVO.class);
            sq1.add(AccountVO_.uuid, SimpleQuery.Op.IN, accountUuidList);
            evt.setAccountInventories(sq1.list());
        }

        bus.publish(evt);
    }


    private void handle(APIUpdateLdapServerMsg msg) {
        APIUpdateLdapServerEvent evt = new APIUpdateLdapServerEvent(msg.getId());

        LdapServerVO ldapServerVO = dbf.findByUuid(msg.getLdapServerUuid(), LdapServerVO.class);
        if (ldapServerVO == null) {
            evt.setErrorCode(errf.instantiateErrorCode(LdapErrors.UNABLE_TO_GET_SPECIFIED_LDAP_SERVER_RECORD,
                    String.format("Cannot find the specified ldap server[uuid:%s] in database.",
                            msg.getLdapServerUuid())));
            bus.publish(evt);
            return;
        }

        //
        if (msg.getName() != null) {
            ldapServerVO.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            ldapServerVO.setDescription(msg.getDescription());
        }
        if (msg.getUrl() != null) {
            ldapServerVO.setUrl(msg.getUrl());
        }
        if (msg.getBase() != null) {
            ldapServerVO.setBase(msg.getBase());
        }
        if (msg.getUsername() != null) {
            ldapServerVO.setUsername(msg.getUsername());
        }
        if (msg.getPassword() != null) {
            ldapServerVO.setPassword(msg.getPassword());
        }
        if (msg.getEncryption() != null) {
            ldapServerVO.setEncryption(msg.getEncryption());
        }

        ldapServerVO = dbf.updateAndRefresh(ldapServerVO);
        evt.setInventory(LdapServerInventory.valueOf(ldapServerVO));

        bus.publish(evt);
    }

}
