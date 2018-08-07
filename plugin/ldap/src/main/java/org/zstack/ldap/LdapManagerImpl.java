package org.zstack.ldap;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.filter.*;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.header.core.captcha.Captcha;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.identity.AccountManager;
import org.zstack.identity.IdentityGlobalConfig;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.SystemTagUtils;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.map;

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
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private Captcha captcha;


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

    protected LdapTemplateContextSource readLdapServerConfiguration() {
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
        } else if (msg instanceof APIGetLdapEntryMsg) {
            handle((APIGetLdapEntryMsg) msg);
        } else if(msg instanceof APIGetCandidateLdapEntryForBindingMsg){
            handle((APIGetCandidateLdapEntryForBindingMsg) msg);
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
        String ldapUseAsLoginName = LdapUtil.getLdapUseAsLoginName();
        try {
            boolean valid;
            String fullUserDn = getFullUserDn(ldapTemplateContextSource.getLdapTemplate(), ldapUseAsLoginName, uid);
            if (fullUserDn.equals("") || password.equals("")) {
                return false;
            }

            LdapServerVO ldapServerVO = getLdapServer();
            LdapServerInventory ldapServerInventory = LdapServerInventory.valueOf(ldapServerVO);
            ldapServerInventory.setUsername(fullUserDn);
            ldapServerInventory.setPassword(password);
            LdapTemplateContextSource ldapTemplateContextSource2 = new LdapUtil().loadLdap(ldapServerInventory);

            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter(ldapUseAsLoginName, uid));
            valid = ldapTemplateContextSource2.getLdapTemplate().
                    authenticate("", filter.toString(), password);
            logger.info(String.format("isValid[%s:%s, dn:%s, valid:%s]", ldapUseAsLoginName, uid, fullUserDn, valid));
            return valid;
        } catch (NamingException e) {
            logger.info("isValid fail userName:" + uid, e);
            return false;
        } catch (Exception e) {
            logger.info("isValid error userName:" + uid, e);
            return false;
        }
    }

    @Override
    public String getFullUserDn(String uid) {
        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration();
        String ldapUseAsLoginName = LdapUtil.getLdapUseAsLoginName();
        String fullUserDn = null;
        try {
            fullUserDn = getFullUserDn(ldapTemplateContextSource.getLdapTemplate(), ldapUseAsLoginName, uid);

            if (fullUserDn.equals("")) {
                return null;
            }
        } catch (Exception e) {
            logger.debug(String.format("Get user dn of uid[%s] failed ", uid));
        }

        return fullUserDn;
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

    private String getPartialUserDn(LdapTemplateContextSource ldapTemplateContextSource,String key, String value) {
        return getFullUserDn(ldapTemplateContextSource.getLdapTemplate(), key, value).
                replace("," + ldapTemplateContextSource.getLdapContextSource().getBaseLdapPathAsString(), "");
    }

    public String getFullUserDn(LdapTemplate ldapTemplate, String key, String val) {
        EqualsFilter f = new EqualsFilter(key, val);
        return getFullUserDn(ldapTemplate, f.toString());
    }

    private String getFullUserDn(LdapTemplate ldapTemplate, String filter) {
        String dn;
        try {
            List<Object> result = ldapTemplate.search("", filter, new AbstractContextMapper<Object>() {
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
            logger.info(String.format("getDn success filter:%s, dn:%s", filter, dn));
        } catch (NamingException e) {
            LdapServerVO ldapServerVO = getLdapServer();
            String errString = String.format(
                    "You'd better check the ldap server[url:%s, baseDN:%s, encryption:%s, username:%s, password:******]" +
                            " configuration and test connection first.getDn error filter:%s",
                    ldapServerVO.getUrl(), ldapServerVO.getBase(),
                    ldapServerVO.getEncryption(), ldapServerVO.getUsername(), filter);
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

    public void findLdapDnMemberOfList(LdapTemplate ldapTemplate, String ldapDn, List<String> resultDnList, List<String> dnIgnoreList){
        if(dnIgnoreList.contains(ldapDn)){
            return;
        }

        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter(LdapUtil.getMemberKey(), ldapDn));

        List<Object> groupList = ldapTemplate.search("", filter.toString(), new AbstractContextMapper<Object>() {
            @Override
            protected Object doMapFromContext(DirContextOperations ctx) {
                return ctx.getNameInNamespace();
            }
        });

        if(groupList.isEmpty()){
            dnIgnoreList.add(ldapDn);
            return;
        }

        for(Object groupObj : groupList){
            if(groupObj == null || !(groupObj instanceof String)){
                continue;
            }

            String groupDn = (String)groupObj;

            if(resultDnList.contains(groupDn)){
                continue;
            }

            resultDnList.add(groupDn);
            findLdapDnMemberOfList(ldapTemplate, groupDn, resultDnList, dnIgnoreList);
        }
    }

    @Override
    public LdapAccountRefVO findLdapAccountRefVO(String ldapDn){

        LdapAccountRefVO ldapAccountRefVO = Q.New(LdapAccountRefVO.class)
                .eq(LdapAccountRefVO_.ldapUid, ldapDn).find();

        if(ldapAccountRefVO != null){
            return ldapAccountRefVO;
        }

        ldapAccountRefVO = findLdapAccountRefByAffiliatedGroup(ldapDn);
        return ldapAccountRefVO;
    }

    /**
     * step 1: Query the ldap group where the ldap user is located（all group）
     * step 2: Check if there is a ldap group bound to the ZStack account
     *              No ZStackAccount-LdapGroup binding，return null
     *              Only one ZStackAccount-LdapGroup binding，return it
     *              Multiple ZStackAccount-LdapGroup bindings，throw exception
     */
    private LdapAccountRefVO findLdapAccountRefByAffiliatedGroup(String ldapDn){

        List<String> ldapDnList = Q.New(LdapAccountRefVO.class)
                .select(LdapAccountRefVO_.ldapUid)
                .listValues();

        if(ldapDnList.isEmpty()){
            return null;
        }

        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration();
        LdapTemplate ldapTemplate = ldapTemplateContextSource.getLdapTemplate();

        List<String> groupDnList = new ArrayList();
        findLdapDnMemberOfList(ldapTemplate, ldapDn, groupDnList, new ArrayList<>());

        if(groupDnList.isEmpty()){
            return null;
        }

        ldapDnList.retainAll(groupDnList);

        if(ldapDnList.isEmpty()){
            return null;
        }

        List<LdapAccountRefVO> vos = Q.New(LdapAccountRefVO.class)
                .in(LdapAccountRefVO_.ldapUid, ldapDnList)
                .list();

        if(vos.size() == 1){
            return vos.get(0);
        }

        List<String> accountList = vos.stream().map(LdapAccountRefVO::getAccountUuid).distinct().collect(Collectors.toList());
        throw new CloudRuntimeException(String.format("The ldapUid[%s] is bound to multiple accounts: %s", ldapDn, accountList.toString()));
    }

    private void handle(APILogInByLdapMsg msg) {
        APILogInByLdapReply reply = new APILogInByLdapReply();

        String ldapLoginName = msg.getUid();
        if (!isValid(ldapLoginName, msg.getPassword())) {
            reply.setError(errf.instantiateErrorCode(IdentityErrors.AUTHENTICATION_ERROR,
                    "Login validation failed in LDAP"));
            bus.reply(msg, reply);
            return;
        }

        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration();
        String dn = getFullUserDn(ldapTemplateContextSource.getLdapTemplate(), LdapUtil.getLdapUseAsLoginName(), ldapLoginName);
        LdapAccountRefVO vo = findLdapAccountRefVO(dn);

        if (vo == null) {
            reply.setError(errf.instantiateErrorCode(IdentityErrors.AUTHENTICATION_ERROR,
                    "The ldapUid does not have a binding account."));
            bus.reply(msg, reply);
            return;
        }

        SimpleQuery<AccountVO> sq = dbf.createQuery(AccountVO.class);
        sq.add(AccountVO_.uuid, SimpleQuery.Op.EQ, vo.getAccountUuid());
        AccountVO avo = sq.find();
        if (avo == null) {
            throw new CloudRuntimeException(String.format("Account[uuid:%s] Not Found!!!", vo.getAccountUuid()));
        }

        reply.setInventory(getSession(vo.getAccountUuid(), vo.getAccountUuid()));
        reply.setAccountInventory(AccountInventory.valueOf(avo));

        bus.reply(msg, reply);
    }

    private void handle(APIAddLdapServerMsg msg) {
        APIAddLdapServerEvent evt = new APIAddLdapServerEvent(msg.getId());

        SimpleQuery<LdapServerVO> sq = dbf.createQuery(LdapServerVO.class);
        List<LdapServerVO> ldapServers = sq.list();
        if (!ldapServers.isEmpty()) {
            evt.setError(errf.instantiateErrorCode(LdapErrors.MORE_THAN_ONE_LDAP_SERVER,
                    "There has been a ldap server record. " +
                            "You'd better remove it before adding a new one!"));
            bus.publish(evt);
            return;
        }

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

        this.saveLdapCleanBindingFilterTag(msg.getSystemTags(), ldapServerVO.getUuid());
        this.saveLdapServerTypeTag(msg.getSystemTags(), ldapServerVO.getUuid());
        this.saveLdapUseAsLoginNameTag(msg.getSystemTags(), ldapServerVO.getUuid());

        bus.publish(evt);
    }

    private void saveLdapCleanBindingFilterTag(List<String> systemTags, String uuid) {
        if(systemTags == null || systemTags.isEmpty()) {
            return;
        }

        PatternedSystemTag tag =  LdapSystemTags.LDAP_CLEAN_BINDING_FILTER;
        String token = LdapSystemTags.LDAP_CLEAN_BINDING_FILTER_TOKEN;

        String tagValue = SystemTagUtils.findTagValue(systemTags, tag, token);
        if(StringUtils.isEmpty(tagValue)){
            return;
        }

        SystemTagCreator creator = tag.newSystemTagCreator(uuid);
        creator.recreate = true;
        creator.setTagByTokens(map(CollectionDSL.e(token, tagValue)));
        creator.create();
    }

    private void saveLdapServerTypeTag(List<String> systemTags, String uuid) {
        if(systemTags == null || systemTags.isEmpty()) {
            return;
        }

        PatternedSystemTag tag =  LdapSystemTags.LDAP_SERVER_TYPE;
        String token = LdapSystemTags.LDAP_SERVER_TYPE_TOKEN;

        String tagValue = SystemTagUtils.findTagValue(systemTags, tag, token);
        if(StringUtils.isEmpty(tagValue)){
            return;
        }

        SystemTagCreator creator = tag.newSystemTagCreator(uuid);
        creator.recreate = true;
        creator.setTagByTokens(map(CollectionDSL.e(token, tagValue)));
        creator.create();
    }

    private void saveLdapUseAsLoginNameTag(List<String> systemTags, String uuid) {
        if(systemTags == null || systemTags.isEmpty()) {
            return;
        }

        PatternedSystemTag tag =  LdapSystemTags.LDAP_USE_AS_LOGIN_NAME;
        String token = LdapSystemTags.LDAP_USE_AS_LOGIN_NAME_TOKEN;

        String tagValue = SystemTagUtils.findTagValue(systemTags, tag, token);
        if(StringUtils.isEmpty(tagValue)){
            return;
        }

        SystemTagCreator creator = tag.newSystemTagCreator(uuid);
        creator.recreate = true;
        creator.setTagByTokens(map(CollectionDSL.e(token, tagValue)));
        creator.create();
    }

    private void handle(APIDeleteLdapServerMsg msg) {
        APIDeleteLdapServerEvent evt = new APIDeleteLdapServerEvent(msg.getId());

        new SQLBatch() {
            @Override
            protected void scripts() {
                if (!q(LdapServerVO.class).eq(LdapServerVO_.uuid, msg.getUuid()).isExists()) {
                    return;
                }

                LdapServerVO vo = q(LdapServerVO.class).eq(LdapServerVO_.uuid, msg.getUuid()).find();
                remove(vo);
                flush();
            }
        }.execute();

        bus.publish(evt);
    }

    private String[] getReturningAttributes() {
        Set<String> returnedAttSet = new HashSet<>();

        String queryLdapEntryReturnAttributes = LdapGlobalConfig.QUERY_LDAP_ENTRY_RETURN_ATTRIBUTES.value();
        if(StringUtils.isNotEmpty(queryLdapEntryReturnAttributes)){
            String separator = LdapGlobalConfig.QUERY_LDAP_ENTRY_RETURN_ATTRIBUTE_SEPARATOR.value();
            separator = separator != null ? separator : LdapConstant.QUERY_LDAP_ENTRY_RETURN_ATTRIBUTE_SEPARATOR;
            String[] attributes = queryLdapEntryReturnAttributes.split(separator);

            returnedAttSet.addAll(Arrays.asList(attributes));
        }

        returnedAttSet.addAll(Arrays.asList(LdapConstant.QUERY_LDAP_ENTRY_MUST_RETURN_ATTRIBUTES));

        returnedAttSet.add(LdapUtil.getLdapUseAsLoginName());

        return returnedAttSet.toArray(new String[]{});
    }

    private void handle(APIGetLdapEntryMsg msg) {
        APIGetLdapEntryReply reply = new APIGetLdapEntryReply();

        List<Object> result = this.searchLdapEntry(msg.getLdapFilter(), msg.getLimit(), null);
        reply.setInventories(result);

        bus.reply(msg, reply);
    }

    private void handle(APIGetCandidateLdapEntryForBindingMsg msg) {
        APIGetLdapEntryReply reply = new APIGetLdapEntryReply();

        AndFilter andFilter = new AndFilter();
        andFilter.and(new HardcodedFilter(msg.getLdapFilter()));

        List<String> boundLdapEntryList = Q.New(LdapAccountRefVO.class)
                .select(LdapAccountRefVO_.ldapUid)
                .listValues();

        List<Object> result = this.searchLdapEntry(andFilter.toString(), msg.getLimit(), new ResultFilter() {
            @Override
            boolean needSelect(String dn) {
                return !boundLdapEntryList.contains(dn);
            }
        });

        reply.setInventories(result);

        bus.reply(msg, reply);
    }

    /**
     * @param filter
     * @param count
     *           count is null, return all
     * @param resultFilter
     *           resultFilter is null, do not check result
     *
     * @return
     */
    private List<Object> searchLdapEntry(String filter, Integer count, ResultFilter resultFilter){
        List<Object> result = new ArrayList<>();

        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration();
        LdapTemplate ldapTemplate = ldapTemplateContextSource.getLdapTemplate();

        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchCtls.setReturningAttributes(this.getReturningAttributes());

        PagedResultsDirContextProcessor processor = new PagedResultsDirContextProcessor(1000);

        try {
            do {
                List<Object> subResult = ldapTemplate.search("", filter, searchCtls, new AbstractContextMapper<Object>() {
                    @Override
                    protected Object doMapFromContext(DirContextOperations ctx) {
                        if (resultFilter != null && !resultFilter.needSelect(ctx.getNameInNamespace())){
                            return null;
                        }

                        Map<String, Object> result = new HashMap<>();
                        result.put(LdapConstant.LDAP_DN_KEY, ctx.getNameInNamespace());

                        List<Object> list = new ArrayList<>();
                        result.put("attributes", list);

                        Attributes attributes = ctx.getAttributes();
                        NamingEnumeration it = attributes.getAll();
                        try {
                            while (it.hasMore()){
                                list.add(it.next());
                            }
                        } catch (javax.naming.NamingException e){
                            logger.error("query ldap entry attributes fail", e.getCause());
                            throw new OperationFailureException(operr("query ldap entry fail, %s", e.toString()));
                        }

                        return result;
                    }
                }, processor);

                subResult.removeIf(Objects::isNull);
                result.addAll(subResult);

            } while (processor.hasMore() && (count == null || count > result.size()));

            if (count == null){
                return result;
            }

            if (result.size() <= count){
                return result;
            }

            return result.subList(0, count);

        } catch (Exception e){
            logger.error("query ldap entry fail", e);
            throw new OperationFailureException(operr("query ldap entry[filter: %s] fail, %s", filter, e.toString()));
        }
    }

    abstract class ResultFilter{
        abstract boolean needSelect(String dn);
    }

    private void handle(APICreateLdapBindingMsg msg) {
        APICreateLdapBindingEvent evt = new APICreateLdapBindingEvent(msg.getId());

        // account check
        SimpleQuery<AccountVO> sq = dbf.createQuery(AccountVO.class);
        sq.add(AccountVO_.uuid, SimpleQuery.Op.EQ, msg.getAccountUuid());
        AccountVO avo = sq.find();
        if (avo == null) {
            evt.setError(errf.instantiateErrorCode(LdapErrors.CANNOT_FIND_ACCOUNT,
                    String.format("cannot find the specified account[uuid:%s]", msg.getAccountUuid())));
            bus.publish(evt);
            return;
        }

        String ldapUseAsLoginName = LdapUtil.getLdapUseAsLoginName();

        // bind op
        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration();
        String fullDn = msg.getLdapUid();
        if (!validateDnExist(ldapTemplateContextSource, fullDn)) {
            throw new OperationFailureException(errf.instantiateErrorCode(LdapErrors.UNABLE_TO_GET_SPECIFIED_LDAP_UID,
                    String.format("cannot find dn[%s] on ldap server[Address:%s, BaseDN:%s].", fullDn,
                            String.join(", ", ldapTemplateContextSource.getLdapContextSource().getUrls()),
                            ldapTemplateContextSource.getLdapContextSource().getBaseLdapPathAsString())));
        }
        try {
            evt.setInventory(bindLdapAccount(msg.getAccountUuid(), fullDn));
            logger.info(String.format("create ldap binding[ldapUid=%s, ldapUseAsLoginName=%s] success", fullDn, ldapUseAsLoginName));
        } catch (JpaSystemException e) {
            if (e.getRootCause() instanceof MySQLIntegrityConstraintViolationException) {
                evt.setError(errf.instantiateErrorCode(LdapErrors.BIND_SAME_LDAP_UID_TO_MULTI_ACCOUNT,
                        "The ldap uid has been bound to an account. "));
            } else {
                throw e;
            }
        }
        bus.publish(evt);
    }

    private boolean validateDnExist(LdapTemplateContextSource ldapTemplateContextSource, String fullDn){
        try {
            String dn = fullDn.replace("," + ldapTemplateContextSource.getLdapContextSource().getBaseLdapPathAsString(), "");
            Object result = ldapTemplateContextSource.getLdapTemplate().lookup(dn, new AbstractContextMapper<Object>() {
                @Override
                protected Object doMapFromContext(DirContextOperations ctx) {
                    Attributes group = ctx.getAttributes();
                    return group;
                }
            });
            return result != null;
        }catch (Exception e){
            logger.warn(String.format("validateDnExist[%s] fail", fullDn), e);
            return false;
        }
    }

    private boolean validateDnExist(LdapTemplateContextSource ldapTemplateContextSource, String fullDn, Filter filter){
        try {
            String dn = fullDn.replace("," + ldapTemplateContextSource.getLdapContextSource().getBaseLdapPathAsString(), "");
            List<Object> result = ldapTemplateContextSource.getLdapTemplate().search(dn, filter.toString(), new AbstractContextMapper<Object>() {
                @Override
                protected Object doMapFromContext(DirContextOperations ctx) {
                    return ctx.getNameInNamespace();
                }
            });
            return result.contains(fullDn);
        }catch (Exception e){
            logger.warn(String.format("validateDnExist[dn=%s, filter=%s] fail", fullDn, filter.toString()), e);
            return false;
        }
    }

    private void handle(APIDeleteLdapBindingMsg msg) {
        APIDeleteLdapBindingEvent evt = new APIDeleteLdapBindingEvent(msg.getId());

        dbf.removeByPrimaryKey(msg.getUuid(), LdapAccountRefVO.class);

        bus.publish(evt);
    }

    @Transactional
    private void handle(APICleanInvalidLdapBindingMsg msg) {
        APICleanInvalidLdapBindingEvent evt = new APICleanInvalidLdapBindingEvent(msg.getId());

        SimpleQuery<LdapAccountRefVO> sq = dbf.createQuery(LdapAccountRefVO.class);
        List<LdapAccountRefVO> refList = sq.list();
        if(refList == null || refList.isEmpty()){
            bus.publish(evt);
            return;
        }

        ArrayList<String> accountUuidList = new ArrayList<>();
        ArrayList<String> ldapAccountRefUuidList = new ArrayList<>();
        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration();

        for (LdapAccountRefVO ldapAccRefVO : refList) {
            // no data in ldap
            String ldapDn = ldapAccRefVO.getLdapUid();
            if(!validateDnExist(ldapTemplateContextSource, ldapDn)){
                accountUuidList.add(ldapAccRefVO.getAccountUuid());
                ldapAccountRefUuidList.add(ldapAccRefVO.getUuid());
                continue;
            }

            // filter
            String filter = LdapSystemTags.LDAP_CLEAN_BINDING_FILTER.getTokenByResourceUuid(ldapAccRefVO.getLdapServerUuid(), LdapSystemTags.LDAP_CLEAN_BINDING_FILTER_TOKEN);
            if(StringUtils.isEmpty(filter)){
                continue;
            }

            HardcodedFilter hardcodedFilter = new HardcodedFilter(filter);
            if(validateDnExist(ldapTemplateContextSource, ldapDn, hardcodedFilter)){
                accountUuidList.add(ldapAccRefVO.getAccountUuid());
                ldapAccountRefUuidList.add(ldapAccRefVO.getUuid());
            }
        }

        if (!accountUuidList.isEmpty()) {
            // remove ldap bindings
            dbf.removeByPrimaryKeys(ldapAccountRefUuidList, LdapAccountRefVO.class);
            // return accounts of which ldap bindings had been removed
            SimpleQuery<AccountVO> sq1 = dbf.createQuery(AccountVO.class);
            sq1.add(AccountVO_.uuid, SimpleQuery.Op.IN, accountUuidList);
            evt.setInventories(sq1.list());
        }

        bus.publish(evt);
    }

    private void handle(APIUpdateLdapServerMsg msg) {
        APIUpdateLdapServerEvent evt = new APIUpdateLdapServerEvent(msg.getId());

        LdapServerVO ldapServerVO = dbf.findByUuid(msg.getLdapServerUuid(), LdapServerVO.class);
        if (ldapServerVO == null) {
            evt.setError(errf.instantiateErrorCode(LdapErrors.UNABLE_TO_GET_SPECIFIED_LDAP_SERVER_RECORD,
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

        this.saveLdapCleanBindingFilterTag(msg.getSystemTags(), ldapServerVO.getUuid());
        this.saveLdapServerTypeTag(msg.getSystemTags(), ldapServerVO.getUuid());
        this.saveLdapUseAsLoginNameTag(msg.getSystemTags(), ldapServerVO.getUuid());

        bus.publish(evt);
    }

}
