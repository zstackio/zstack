package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.*;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.APIIsOpensourceVersionMsg;
import org.zstack.header.APIIsOpensourceVersionReply;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.Message;
import org.zstack.header.rest.RestAuthenticationBackend;
import org.zstack.header.rest.RestAuthenticationParams;
import org.zstack.header.rest.RestAuthenticationType;
import org.zstack.header.vo.*;
import org.zstack.identity.rbac.PolicyUtils;
import org.zstack.utils.*;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.header.identity.AccountConstant.ACCOUNT_REST_AUTHENTICATION_TYPE;

public class AccountManagerImpl extends AbstractService implements AccountManager, SoftDeleteEntityExtensionPoint,
        HardDeleteEntityExtensionPoint, ApiMessageInterceptor, RestAuthenticationBackend, PrepareDbInitialValueExtensionPoint {
    private static final CLogger logger = Utils.getLogger(AccountManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private List<QuotaUpdateChecker> quotaChangeCheckers = Collections.emptyList();

    private final List<String> resourceTypeForAccountRef = new ArrayList<>();
    private final List<Class> resourceTypes = new ArrayList<>();
    private final Map<Class, List<Quota>> messageQuotaMap = new HashMap<>();
    private final Map<String, Quota> nameQuotaMap = new HashMap<>();
    private final HashSet<Class> accountApiControl = new HashSet<>();
    private final HashSet<Class> accountApiControlInternal = new HashSet<>();
    private final List<Quota> definedQuotas = new ArrayList<>();

    @Override
    public void prepareDbInitialValue() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                if (!q(AccountVO.class)
                        .eq(AccountVO_.name, AccountConstant.INITIAL_SYSTEM_ADMIN_NAME)
                        .eq(AccountVO_.type, AccountType.SystemAdmin).isExists()) {
                    AccountVO vo = new AccountVO();
                    vo.setUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    vo.setName(AccountConstant.INITIAL_SYSTEM_ADMIN_NAME);
                    vo.setPassword(AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD);
                    vo.setType(AccountType.SystemAdmin);
                    persist(vo);
                    flush();

                    logger.debug(String.format("Created initial system admin account[name:%s]", AccountConstant.INITIAL_SYSTEM_ADMIN_NAME));
                }
            }
        }.execute();
    }
    class AccountCheckField {
        Field field;
        APIParam param;
    }

    class MessageAction {
        boolean adminOnly;
        List<String> actions;
        String category;
        boolean accountOnly;
        List<AccountCheckField> accountCheckFields;
        boolean accountControl;
    }

    private Map<Class, MessageAction> actions = new HashMap<>();
    private Future<Void> expiredSessionCollector;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof AccountMessage) {
            passThrough((AccountMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    public Map<Class, List<Quota>> getMessageQuotaMap() {
        return messageQuotaMap;
    }

    @Override
    public List<Quota> getQuotas() {
        return definedQuotas;
    }

    @Override
    @Transactional
    public AccountResourceRefInventory changeResourceOwner(String resourceUuid, String newOwnerUuid) {
        String sql = "select ref from AccountResourceRefVO ref where ref.resourceUuid = :resUuid";
        TypedQuery<AccountResourceRefVO> q = dbf.getEntityManager().createQuery(sql, AccountResourceRefVO.class);
        q.setParameter("resUuid", resourceUuid);
        List<AccountResourceRefVO> refs = q.getResultList();
        if (refs.isEmpty()) {
            throw new OperationFailureException(argerr("cannot find the resource[uuid:%s]; wrong resourceUuid or the resource is admin resource",
                            resourceUuid));
        }

        AccountResourceRefVO ref = refs.get(0);
        final AccountResourceRefInventory origin = AccountResourceRefInventory.valueOf(ref);

        for (ResourceOwnerPreChangeExtensionPoint ext : pluginRgty.getExtensionList(ResourceOwnerPreChangeExtensionPoint.class)) {
            ext.resourceOwnerPreChange(origin, newOwnerUuid);
        }

        ref.setAccountUuid(newOwnerUuid);
        ref.setOwnerAccountUuid(newOwnerUuid);
        ref = dbf.getEntityManager().merge(ref);

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(ResourceOwnerAfterChangeExtensionPoint.class),
                new ForEachFunction<ResourceOwnerAfterChangeExtensionPoint>() {
                    @Override
                    public void run(ResourceOwnerAfterChangeExtensionPoint ext) {
                        ext.resourceOwnerAfterChange(origin, newOwnerUuid);
                    }
                });

        return AccountResourceRefInventory.valueOf(ref);
    }

    @Override
    public void checkApiMessagePermission(APIMessage msg) {
        new Auth().check(msg);
    }

    @Override
    public boolean isAdmin(SessionInventory session) {
        return AccountConstant.INITIAL_SYSTEM_ADMIN_UUID.equals(session.getAccountUuid());
    }

    private void passThrough(AccountMessage msg) {
        AccountVO vo = dbf.findByUuid(msg.getAccountUuid(), AccountVO.class);
        if (vo == null) {
            bus.replyErrorByMessageType((Message) msg, err(SysErrors.RESOURCE_NOT_FOUND, "unable to find account[uuid=%s]", msg.getAccountUuid()));
            return;
        }

        AccountBase base = new AccountBase(vo);
        base.handleMessage((Message) msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateAccountMsg) {
            handle((APICreateAccountMsg) msg);
        } else if (msg instanceof APILogInByAccountMsg) {
            handle((APILogInByAccountMsg) msg);
        } else if (msg instanceof APILogInByUserMsg) {
            handle((APILogInByUserMsg) msg);
        } else if (msg instanceof APILogOutMsg) {
            handle((APILogOutMsg) msg);
        } else if (msg instanceof APIValidateSessionMsg) {
            handle((APIValidateSessionMsg) msg);
        } else if (msg instanceof APICheckApiPermissionMsg) {
            handle((APICheckApiPermissionMsg) msg);
        } else if (msg instanceof APIGetResourceAccountMsg) {
            handle((APIGetResourceAccountMsg) msg);
        } else if (msg instanceof APIChangeResourceOwnerMsg) {
            handle((APIChangeResourceOwnerMsg) msg);
        } else if (msg instanceof APIGetResourceNamesMsg) {
            handle((APIGetResourceNamesMsg) msg);
        } else if (msg instanceof APIIsOpensourceVersionMsg) {
            handle((APIIsOpensourceVersionMsg) msg);
        } else if (msg instanceof APIRenewSessionMsg) {
            handle((APIRenewSessionMsg) msg);
        } else if (msg instanceof APIGetSupportedIdentityModelsMsg) {
            handle((APIGetSupportedIdentityModelsMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetSupportedIdentityModelsMsg msg) {
        APIGetSupportedIdentityModelsReply reply = new APIGetSupportedIdentityModelsReply();
        reply.setConfigs(Arrays.asList(IdentityGlobalProperty.IDENTITY_INIT_TYPE.split(",")));
        bus.reply(msg, reply);
    }

    private void handle(APIIsOpensourceVersionMsg msg) {
        APIIsOpensourceVersionReply reply = new APIIsOpensourceVersionReply();
        reply.setOpensource(true);
        bus.reply(msg, reply);
    }

    private void handle(APIGetResourceNamesMsg msg) {
        List<ResourceInventory> invs = new SQLBatchWithReturn<List<ResourceInventory>>() {
            @Override
            protected List<ResourceInventory> scripts() {
                Query q = dbf.getEntityManager().createNativeQuery("select uuid, resourceName, resourceType from ResourceVO where uuid in (:uuids)");
                q.setParameter("uuids", msg.getUuids());
                List<Object[]> objs = q.getResultList();

                List<ResourceVO> vos = objs.stream().map(ResourceVO::new).collect(Collectors.toList());

                return ResourceInventory.valueOf(vos);
            }
        }.execute();

        APIGetResourceNamesReply reply = new APIGetResourceNamesReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void handle(final APIChangeResourceOwnerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("change-resource-owner-to-account-%s", msg.getAccountUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doChangeResourceOwnerInQueue(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void doChangeResourceOwnerInQueue(APIChangeResourceOwnerMsg msg, NoErrorCompletion completion) {
        APIChangeResourceOwnerEvent evt = new APIChangeResourceOwnerEvent(msg.getId());

        FlowChain chain = new SimpleFlowChain();
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getAccountUuid();
                if (new QuotaUtil().isAdminAccount(resourceTargetOwnerAccountUuid)) {
                    trigger.next();
                    return;
                }
                // check if change resource owner to self
                SimpleQuery<AccountResourceRefVO> queryAccResRefVO = dbf.createQuery(AccountResourceRefVO.class);
                queryAccResRefVO.add(AccountResourceRefVO_.resourceUuid, Op.EQ, msg.getResourceUuid());
                AccountResourceRefVO accResRefVO = queryAccResRefVO.find();
                String resourceOriginalOwnerAccountUuid = accResRefVO.getOwnerAccountUuid();
                if (resourceTargetOwnerAccountUuid.equals(resourceOriginalOwnerAccountUuid)) {
                    trigger.fail(err(IdentityErrors.QUOTA_INVALID_OP,
                            "Invalid ChangeResourceOwner operation." +
                                    "Original owner is the same as target owner." +
                                    "Current account is [uuid: %s]." +
                                    "The resource target owner account[uuid: %s]." +
                                    "The resource original owner account[uuid:%s].",
                            currentAccountUuid, resourceTargetOwnerAccountUuid, resourceOriginalOwnerAccountUuid
                    ));
                    return;
                }
                // check quota
                Map<String, QuotaPair> pairs = new QuotaUtil().makeQuotaPairs(resourceTargetOwnerAccountUuid);
                for (Quota quota : messageQuotaMap.get(APIChangeResourceOwnerMsg.class)) {
                    quota.getOperator().checkQuota(msg, pairs);
                }

                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                evt.setInventory(changeResourceOwner(msg.getResourceUuid(), msg.getAccountUuid()));
                trigger.next();
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.publish(evt);
                completion.done();
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
                completion.done();
            }
        }).start();
    }


    @Transactional(readOnly = true)
    private void handle(APIGetResourceAccountMsg msg) {
        String sql = "select a, ref.resourceUuid" +
                " from AccountResourceRefVO ref, AccountVO a" +
                " where a.uuid = ref.accountUuid" +
                " and ref.resourceUuid in (:uuids)";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("uuids", msg.getResourceUuids());
        List<Tuple> tuples = q.getResultList();
        Map<String, AccountInventory> ret = new HashMap<>();
        for (Tuple t : tuples) {
            String resUuid = t.get(1, String.class);
            AccountVO vo = t.get(0, AccountVO.class);
            ret.put(resUuid, AccountInventory.valueOf(vo));
        }

        AccountVO admin = dbf.findByUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, AccountVO.class);
        AccountInventory adminInv = AccountInventory.valueOf(admin);
        for (String resUuid : msg.getResourceUuids()) {
            if (!ret.containsKey(resUuid)) {
                ret.put(resUuid, adminInv);
            }
        }

        APIGetResourceAccountReply reply = new APIGetResourceAccountReply();
        reply.setInventories(ret);
        bus.reply(msg, reply);
    }

    private void handle(APICheckApiPermissionMsg msg) {
        if (msg.getUserUuid() != null) {
            SimpleQuery<AccountVO> q = dbf.createQuery(AccountVO.class);
            q.add(AccountVO_.uuid, Op.EQ, msg.getSession().getAccountUuid());
            q.add(AccountVO_.type, Op.EQ, AccountType.SystemAdmin);
            boolean isAdmin = q.isExists();

            SimpleQuery<UserVO> uq = dbf.createQuery(UserVO.class);
            uq.add(UserVO_.accountUuid, Op.EQ, msg.getSession().getAccountUuid());
            uq.add(UserVO_.uuid, Op.EQ, msg.getUserUuid());
            boolean isMine = uq.isExists();

            if (!isAdmin && !isMine) {
                throw new OperationFailureException(operr(
                        "the user specified by the userUuid[%s] does not belong to the current account, and the" +
                                " current account is not an admin account, so it has no permission to check the user's" +
                                "permissions", msg.getUserUuid()
                ));
            }
        }

        Map<String, String> ret = new HashMap<>();

        SessionInventory session = new SessionInventory();
        if (msg.getUserUuid() != null) {
            UserVO user = dbf.findByUuid(msg.getUserUuid(), UserVO.class);
            session.setAccountUuid(user.getAccountUuid());
            session.setUserUuid(user.getUuid());
        } else {
            session = msg.getSession();
        }

        for (String apiName : msg.getApiNames()) {
            try {
                Class apiClass = Class.forName(apiName);
                APIMessage api = (APIMessage) apiClass.newInstance();
                api.setSession(session);

                try {
                    new Auth().check(api);
                    ret.put(apiName, StatementEffect.Allow.toString());
                } catch (ApiMessageInterceptionException e) {
                    logger.debug(e.getMessage());
                    ret.put(apiName, StatementEffect.Deny.toString());
                }
            } catch (ClassNotFoundException e) {
                throw new OperationFailureException(argerr("%s is not an API", apiName));
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        }

        APICheckApiPermissionReply reply = new APICheckApiPermissionReply();
        reply.setInventory(ret);
        bus.reply(msg, reply);
    }


    private void handle(APIValidateSessionMsg msg) {
        APIValidateSessionReply reply = new APIValidateSessionReply();

        ErrorCode errorCode = Session.checkSessionExpired(msg.getSessionUuid());

        boolean valid = errorCode == null;

        reply.setValidSession(valid);

        bus.reply(msg, reply);
    }

    private void handle(APIRenewSessionMsg msg) {
        APIRenewSessionEvent evt = new APIRenewSessionEvent(msg.getId());
        evt.setInventory(Session.renewSession(msg.getSessionUuid(), msg.getDuration()));
        bus.publish(evt);
    }

    private void handle(APILogOutMsg msg) {
        APILogOutReply reply = new APILogOutReply();
        SessionInventory session = Session.getSession(msg.getSessionUuid());
        if (session == null) {
            SessionVO svo = dbf.findByUuid(msg.getSessionUuid(), SessionVO.class);
            session = svo == null ? null : SessionInventory.valueOf(svo);
        }
        msg.setSession(session);
        logOutSession(msg.getSessionUuid());
        bus.reply(msg, reply);
    }

    private SessionInventory getSession(String accountUuid, String userUuid) {
        return Session.login(accountUuid, userUuid);
    }

    private void handle(APILogInByUserMsg msg) {
        APILogInReply reply = new APILogInReply();

        String accountUuid;
        if (msg.getAccountUuid() != null) {
            accountUuid = msg.getAccountUuid();
        } else {
            SimpleQuery<AccountVO> accountq = dbf.createQuery(AccountVO.class);
            accountq.select(AccountVO_.uuid);
            accountq.add(AccountVO_.name, Op.EQ, msg.getAccountName());
            accountUuid = accountq.findValue();
            if (accountUuid == null) {
                reply.setError(err(IdentityErrors.AUTHENTICATION_ERROR, "wrong account or username or password"));
                bus.reply(msg, reply);
                return;
            }
        }

        SimpleQuery<UserVO> q = dbf.createQuery(UserVO.class);
        q.add(UserVO_.accountUuid, Op.EQ, accountUuid);
        q.add(UserVO_.password, Op.EQ, msg.getPassword());
        q.add(UserVO_.name, Op.EQ, msg.getUserName());
        UserVO user = q.find();

        if (user == null) {
            reply.setError(err(IdentityErrors.AUTHENTICATION_ERROR,
                    "wrong account or username or password"
            ));
            bus.reply(msg, reply);
            return;
        }
        SessionInventory session = getSession(user.getAccountUuid(), user.getUuid());
        msg.setSession(session);
        reply.setInventory(session);
        bus.reply(msg, reply);
    }

    private void handle(APILogInByAccountMsg msg) {
        APILogInReply reply = new APILogInReply();

        AccountLoginStruct struct = null;

        SimpleQuery<AccountVO> q = dbf.createQuery(AccountVO.class);
        q.add(AccountVO_.name, Op.EQ, msg.getAccountName());
        q.add(AccountVO_.password, Op.EQ, msg.getPassword());
        AccountVO vo = q.find();

        // if accountName/password not exists or specific type is set
        // use extension to get login structure
        if (vo == null || (msg.getAccountType() != null && !msg.getAccountType().equals(AccountConstant.LOGIN_TYPE))) {
            for (AccountLoginExtensionPoint ext : pluginRgty.getExtensionList(AccountLoginExtensionPoint.class)) {
                struct = ext.getLoginEntry(msg.getAccountName(), msg.getPassword(), msg.getAccountType());

                if (struct != null) {
                    break;
                }
            }
        } else {
            struct = new AccountLoginStruct();
            struct.setAccountUuid(vo.getUuid());
            struct.setUserUuid(vo.getUuid());
            struct.setResourceType(AccountVO.class.getSimpleName());
        }

        if (struct == null) {
            reply.setError(err(IdentityErrors.AUTHENTICATION_ERROR, "wrong account name or password"));
            bus.reply(msg, reply);
            return;
        }

        for (AdditionalLoginExtensionPoint exp : pluginRgty.getExtensionList(AdditionalLoginExtensionPoint.class)){
            ErrorCode errorCode = exp.authenticate(msg, struct.getUserUuid(), struct.getResourceType());
            if (errorCode != null) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                return;
            }
        }

        SessionInventory session = getSession(struct.getAccountUuid(), struct.getUserUuid());
        msg.setSession(session);
        IdentityCanonicalEvents.AccountLoginData data = new IdentityCanonicalEvents.AccountLoginData();
        data.setAccountUuid(struct.getAccountUuid());
        data.setUserUuid(struct.getUserUuid());
        evtf.fire(IdentityCanonicalEvents.ACCOUNT_LOGIN_PATH, data);

        reply.setInventory(session);
        bus.reply(msg, reply);
    }

    private void handle(APICreateAccountMsg msg) {
        final AccountInventory inv = new SQLBatchWithReturn<AccountInventory>() {
            @Override
            protected AccountInventory scripts() {
                AccountVO vo = new AccountVO();
                if (msg.getResourceUuid() != null) {
                    vo.setUuid(msg.getResourceUuid());
                } else {
                    vo.setUuid(Platform.getUuid());
                }
                vo.setName(msg.getName());
                vo.setDescription(msg.getDescription());
                vo.setPassword(msg.getPassword());
                vo.setType(msg.getType() != null ? AccountType.valueOf(msg.getType()) : AccountType.Normal);
                persist(vo);
                reload(vo);

                PolicyVO p = new PolicyVO();
                p.setUuid(Platform.getUuid());
                p.setAccountUuid(vo.getUuid());
                p.setName("DEFAULT-READ");
                p.setData(IAMIdentityResourceGenerator.readAPIsForNormalAccountJSONStatement);
                persist(p);
                reload(p);

                List<Tuple> ts = Q.New(GlobalConfigVO.class).select(GlobalConfigVO_.name, GlobalConfigVO_.value)
                        .eq(GlobalConfigVO_.category, AccountConstant.QUOTA_GLOBAL_CONFIG_CATETORY).listTuple();

                for (Tuple t : ts) {
                    String rtype = t.get(0, String.class);
                    long quota = Long.parseLong(t.get(1, String.class));

                    QuotaVO qvo = new QuotaVO();
                    qvo.setUuid(Platform.getUuid());
                    qvo.setIdentityType(AccountVO.class.getSimpleName());
                    qvo.setIdentityUuid(vo.getUuid());
                    qvo.setName(rtype);
                    qvo.setValue(quota);
                    qvo.setAccountUuid(vo.getUuid());
                    persist(qvo);
                    reload(qvo);
                }

                reload(vo);
                return AccountInventory.valueOf(vo);
            }
        }.execute();


        CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterCreateAccountExtensionPoint.class),
                arg -> arg.afterCreateAccount(inv));

        APICreateAccountEvent evt = new APICreateAccountEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(AccountConstant.SERVICE_ID);
    }

    private void buildResourceTypes() throws ClassNotFoundException {
        for (String resourceTypeName : resourceTypeForAccountRef) {
            Class<?> rs = Class.forName(resourceTypeName);
            resourceTypes.add(rs);
        }
    }

    private void addResourceType() {
        for (AddtionalResourceTypeExtensionPoint ext: pluginRgty.getExtensionList(AddtionalResourceTypeExtensionPoint.class)) {
            List<String> list = ext.getAddtionalResourceType();
            if (list != null && list.size() > 0) {
                resourceTypeForAccountRef.addAll(list);
            }
        }

        Platform.getReflections().getSubTypesOf(OwnedByAccount.class)
                .forEach(clz -> resourceTypeForAccountRef.add(clz.getName()));
    }

    @Override
    public boolean start() {
        try {
            addResourceType();
            buildResourceTypes();
            buildActions();
            collectDefaultQuota();
            configureGlobalConfig();
            updateResourceVONameOnEntityUpdate();

            for (ReportApiAccountControlExtensionPoint ext : pluginRgty.getExtensionList(ReportApiAccountControlExtensionPoint.class)) {
                List<Class> apis = ext.reportApiAccountControl();
                DebugUtils.Assert(apis != null, String.format("%s.reportApiAccountControl() returns null", ext.getClass()));
                accountApiControlInternal.addAll(apis);
            }

        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return true;
    }

    private void updateResourceVONameOnEntityUpdate() {
        dbf.installEntityLifeCycleCallback(null, EntityEvent.PRE_UPDATE, (evt, o) -> {
            if (o instanceof ResourceVO) {
                ResourceVO rvo = (ResourceVO) o;
                if (rvo.hasNameField()) {
                    String name = rvo.getValueOfNameField();
                    if (name != null) {
                        rvo.setResourceName(name);
                    }
                }
            }
        });
    }

    private void configureGlobalConfig() {
        String v = IdentityGlobalConfig.ACCOUNT_API_CONTROL.value();
        String[] classNames = v.split(",");
        for (String cn : classNames) {
            cn = cn.trim();
            try {
                Class clz = Class.forName(cn);
                accountApiControl.add(clz);
            } catch (ClassNotFoundException e) {
                throw new CloudRuntimeException(String.format("no API found for %s", cn));
            }
        }

        IdentityGlobalConfig.ACCOUNT_API_CONTROL.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                if (newValue.isEmpty()) {
                    return;
                }

                String[] classNames = newValue.split(",");
                for (String cn : classNames) {
                    cn = cn.trim();
                    try {
                        Class.forName(cn);
                    } catch (ClassNotFoundException e) {
                        throw new GlobalConfigException(String.format("no API found for %s", cn));
                    }
                }
            }
        });

        IdentityGlobalConfig.ACCOUNT_API_CONTROL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                accountApiControl.clear();

                if (newConfig.value().isEmpty()) {
                    return;
                }

                String[] classNames = newConfig.value().split(",");
                for (String name : classNames) {
                    try {
                        name = name.trim();
                        Class clz = Class.forName(name);
                        accountApiControl.add(clz);
                    } catch (ClassNotFoundException e) {
                        throw new CloudRuntimeException(e);
                    }
                }
            }
        });
    }

    private void collectDefaultQuota() {
        Map<String, Long> defaultQuota = new HashMap<>();

        // Add quota and quota checker
        for (ReportQuotaExtensionPoint ext : pluginRgty.getExtensionList(ReportQuotaExtensionPoint.class)) {
            List<Quota> quotas = ext.reportQuota();
            DebugUtils.Assert(quotas != null, String.format("%s.getQuotaPairs() returns null", ext.getClass()));

            definedQuotas.addAll(quotas);

            for (Quota quota : quotas) {
                DebugUtils.Assert(quota.getQuotaPairs() != null,
                        String.format("%s reports a quota containing a null quotaPairs", ext.getClass()));

                for (QuotaPair p : quota.getQuotaPairs()) {
                    if (defaultQuota.containsKey(p.getName())) {
                        throw new CloudRuntimeException(String.format("duplicate DefaultQuota[resourceType: %s] reported by %s", p.getName(), ext.getClass()));
                    }

                    defaultQuota.put(p.getName(), p.getValue());
                    nameQuotaMap.put(p.getName(), quota);
                }

                for (Class clz : quota.getMessagesNeedValidation()) {
                    if (messageQuotaMap.containsKey(clz)) {
                        messageQuotaMap.get(clz).add(quota);
                    } else {
                        ArrayList<Quota> quotaArrayList = new ArrayList<>();
                        quotaArrayList.add(quota);
                        messageQuotaMap.put(clz, quotaArrayList);
                    }

                }
            }
        }

        // Add additional quota checker to quota
        for (RegisterQuotaCheckerExtensionPoint ext : pluginRgty.getExtensionList(RegisterQuotaCheckerExtensionPoint.class)) {
            // Map<quota name,Set<QuotaValidator>>
            Map<String, Set<Quota.QuotaValidator>> m = ext.registerQuotaValidator();
            for (Map.Entry<String, Set<Quota.QuotaValidator>> entry : m.entrySet()) {
                Quota quota = nameQuotaMap.get(entry.getKey());
                quota.addQuotaValidators(entry.getValue());
                for (Quota.QuotaValidator q : entry.getValue()) {
                    for (Class clz : q.getMessagesNeedValidation()) {
                        if (messageQuotaMap.containsKey(clz)) {
                            messageQuotaMap.get(clz).add(quota);
                        } else {
                            ArrayList<Quota> quotaArrayList = new ArrayList<>();
                            quotaArrayList.add(quota);
                            messageQuotaMap.put(clz, quotaArrayList);
                        }
                    }
                }
            }

        }

        repairAccountQuota(defaultQuota);
    }


    private void repairAccountQuota(Map<String, Long> defaultQuota) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                long count = q(AccountVO.class)
                        .eq(AccountVO_.type, AccountType.Normal)
                        .count();
                sql("select uuid from AccountVO account where account.type = :type", String.class)
                        .param("type", AccountType.Normal)
                        .limit(1000)
                        .paginate(count, (List<String> normalAccounts) -> {
                            List<QuotaVO> needPersistQuotas = new ArrayList<>();
                            normalAccounts.parallelStream().forEach(nA -> {
                                List<String> existingQuota = q(QuotaVO.class).select(QuotaVO_.name).eq(QuotaVO_.identityUuid, nA).listValues();
                                for (Map.Entry<String, Long> e : defaultQuota.entrySet()) {
                                    String rtype = e.getKey();
                                    Long value = e.getValue();
                                    if (existingQuota.contains(rtype)) {
                                        continue;
                                    }

                                    QuotaVO q = new QuotaVO();
                                    q.setUuid(Platform.getUuid());
                                    q.setAccountUuid(nA);
                                    q.setName(rtype);
                                    q.setIdentityUuid(nA);
                                    q.setIdentityType(AccountVO.class.getSimpleName());
                                    q.setValue(value);
                                    needPersistQuotas.add(q);

                                    if (logger.isTraceEnabled()) {
                                        logger.trace(String.format("create default quota[name: %s, value: %s] global config", rtype, value));
                                    }
                                }
                            });
                            needPersistQuotas.forEach(this::persist);
                        });
            }
        }.execute();
    }

    private void doAdminAdoptResource(List<String> resourceUuids, String originAccountUuid) {
        List<String> orphanedResources = new ArrayList<>();
        final List<TakeOverResourceExtensionPoint> exts = pluginRgty.getExtensionList(TakeOverResourceExtensionPoint.class);

        new SQLBatch() {
            @Override
            protected void scripts() {
                // use native SQL instead of JPQL here,
                // JPQL will join all sub-tables of ResourceVO, which
                // exceeds the limit of max tables MySQL can join
                List rvos = databaseFacade.getEntityManager().createNativeQuery("select uuid, resourceType, concreteResourceType from ResourceVO where uuid not in (select resourceUuid from AccountResourceRefVO)" +
                        " and resourceType in (:rtypes)")
                        .setParameter("rtypes", ResourceTypeMetadata.getAllBaseTypes().stream().map(Class::getSimpleName).collect(Collectors.toList()))
                        .getResultList();

                rvos.forEach(obj -> {
                    Object[] values = (Object[]) obj;
                    String ruuid = values[0].toString();
                    String rtype = values[1].toString();
                    String crtype = values[2].toString();

                    AccountResourceRefVO ref = new AccountResourceRefVO();
                    ref.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    ref.setResourceType(rtype);
                    ref.setConcreteResourceType(crtype);
                    ref.setResourceUuid(ruuid);
                    ref.setPermission(AccountConstant.RESOURCE_PERMISSION_WRITE);
                    ref.setOwnerAccountUuid(ref.getAccountUuid());
                    ref.setShared(false);
                    persist(ref);
                    orphanedResources.add(ruuid);
                });
            }
        }.execute();

        if (orphanedResources.size() == 0 || resourceUuids.size() == 0) {
            return;
        }
        List<String> uuids = resourceUuids.stream().filter(orphanedResources::contains).collect(Collectors.toList());

        CollectionUtils.forEach(exts, new ForEachFunction<TakeOverResourceExtensionPoint>() {
            @Override
            public void run(TakeOverResourceExtensionPoint ext) {
                ext.afterTakeOverResource(uuids, originAccountUuid, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
            }
        });
    }

    public void adminAdoptAllOrphanedResource(List<String> resourceUuid, String originAccountUuid){
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() {
                doAdminAdoptResource(resourceUuid, originAccountUuid);
                return null;
            }

            @Override
            public String getName() {
                return "admin-adopt-all-orphaned-resource";
            }

            @Override
            public String getSyncSignature() {
                return "admin-adopt-all-orphaned-resource";
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }
        });
    }

    @Override
    public Class getBaseResourceType(Class clz) {
        for (Class c : resourceTypes) {
            if (c.isAssignableFrom(clz)) {
                return c;
            }
        }

        return null;
    }

    private void buildActions() {
        BeanUtils.reflections.getSubTypesOf(APIMessage.class).forEach(clz -> {
            Action a = clz.getAnnotation(Action.class);
            if (a == null) {
                logger.debug(String.format("API message[%s] doesn't have annotation @Action, assume it's an admin only API", clz));
                MessageAction ma = new MessageAction();
                ma.adminOnly = true;
                ma.accountOnly = true;
                ma.accountControl = false;
                actions.put(clz, ma);
                return;
            }

            MessageAction ma = new MessageAction();
            ma.accountOnly = a.accountOnly();
            ma.adminOnly = a.adminOnly();
            ma.category = a.category();
            ma.actions = new ArrayList<String>();
            ma.accountControl = a.accountControl();
            ma.accountCheckFields = new ArrayList<AccountCheckField>();
            for (String ac : a.names()) {
                ma.actions.add(String.format("%s:%s", ma.category, ac));
            }

            List<Field> allFields = FieldUtils.getAllFields(clz);
            for (Field f : allFields) {
                APIParam at = f.getAnnotation(APIParam.class);
                if (at == null || !at.checkAccount()) {
                    continue;
                }

                if (!String.class.isAssignableFrom(f.getType()) && !Collection.class.isAssignableFrom(f.getType())) {
                    throw new CloudRuntimeException(String.format("@APIParam of %s.%s has checkAccount = true, however," +
                                    " the type of the field is not String or Collection but %s. " +
                                    "This field must be a resource UUID or a collection(e.g. List) of UUIDs",
                            clz.getName(), f.getName(), f.getType()));
                }

                AccountCheckField af = new AccountCheckField();
                f.setAccessible(true);
                af.field = f;
                af.param = at;
                ma.accountCheckFields.add(af);
            }

            ma.actions.add(String.format("%s:%s", ma.category, clz.getName()));
            ma.actions.add(String.format("%s:%s", ma.category, clz.getSimpleName()));
            actions.put(clz, ma);
        });
    }

    @Override
    public boolean stop() {
        if (expiredSessionCollector != null) {
            expiredSessionCollector.cancel(true);
        }
        return true;
    }

    @Override
    public boolean isResourceHavingAccountReference(Class entityClass) {
        for (Class clz : resourceTypes) {
            if (clz.isAssignableFrom(entityClass)) {
                return true;
            }
        }

        return false;
    }


    @Override
    @Transactional(readOnly = true)
    public List<String> getResourceUuidsCanAccessByAccount(String accountUuid, Class resourceType) {
        String sql = "select a.type from AccountVO a where a.uuid = :auuid";
        TypedQuery<AccountType> q = dbf.getEntityManager().createQuery(sql, AccountType.class);
        q.setParameter("auuid", accountUuid);
        List<AccountType> types = q.getResultList();
        if (types.isEmpty()) {
            throw new OperationFailureException(argerr("cannot find the account[uuid:%s]", accountUuid));
        }

        AccountType atype = types.get(0);
        if (AccountType.SystemAdmin == atype) {
            return null;
        }

        sql = "select r.resourceUuid from AccountResourceRefVO r where r.accountUuid = :auuid" +
                " and r.resourceType = :rtype";
        TypedQuery<String> rq = dbf.getEntityManager().createQuery(sql, String.class);
        rq.setParameter("auuid", accountUuid);
        rq.setParameter("rtype", resourceType.getSimpleName());
        List<String> ownResourceUuids = rq.getResultList();

        sql = "select r.resourceUuid from SharedResourceVO r where" +
                " (r.toPublic = :toPublic or r.receiverAccountUuid = :auuid) and r.resourceType = :rtype";
        TypedQuery<String> srq = dbf.getEntityManager().createQuery(sql, String.class);
        srq.setParameter("toPublic", true);
        srq.setParameter("auuid", accountUuid);
        srq.setParameter("rtype", resourceType.getSimpleName());
        List<String> shared = srq.getResultList();
        shared.addAll(ownResourceUuids);

        return shared;
    }

    @Override
    public String getOwnerAccountUuidOfResource(String resourceUuid) {
        try {
            SimpleQuery<AccountResourceRefVO> q = dbf.createQuery(AccountResourceRefVO.class);
            q.select(AccountResourceRefVO_.ownerAccountUuid);
            q.add(AccountResourceRefVO_.resourceUuid, Op.EQ, resourceUuid);
            String ownerUuid = q.findValue();
            DebugUtils.Assert(ownerUuid != null, String.format("cannot find owner uuid for resource[uuid:%s]", resourceUuid));
            return ownerUuid;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public List<Class> getEntityClassForSoftDeleteEntityExtension() {
        return resourceTypes;
    }

    @Override
    @Transactional
    public void postSoftDelete(Collection entityIds, Class entityClass) {
        String sql = "delete from AccountResourceRefVO ref where ref.resourceUuid in (:uuids) and ref.resourceType = :resourceType";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("uuids", entityIds);
        q.setParameter("resourceType", entityClass.getSimpleName());
        q.executeUpdate();
    }

    @Override
    public List<Class> getEntityClassForHardDeleteEntityExtension() {
        return resourceTypes;
    }

    @Override
    public void postHardDelete(Collection entityIds, Class entityClass) {
        if (resourceTypes.contains(entityClass)) {
            postSoftDelete(entityIds, entityClass);
        }
    }

    private void logOutSession(String sessionUuid) {
        SessionInventory session = Session.getSession(sessionUuid);
        if (session == null) {
            SessionVO svo = dbf.findByUuid(sessionUuid, SessionVO.class);
            session = svo == null ? null : SessionInventory.valueOf(svo);
        }

        if (session == null) {
            return;
        }

        Session.logout(sessionUuid);
    }

    @Transactional(readOnly = true)
    private Timestamp getCurrentSqlDate() {
        Query query = dbf.getEntityManager().createNativeQuery("select current_timestamp()");
        return (Timestamp) query.getSingleResult();
    }

    class Auth {
        APIMessage msg;
        SessionInventory session;
        MessageAction action;
        String username;

        void validate(APIMessage msg) {
            this.msg = msg;
            if (msg.getClass().isAnnotationPresent(SuppressCredentialCheck.class)) {
                return;
            }

            action = actions.get(msg.getClass());

            sessionCheck();
            policyCheck();

            msg.setSession(session);
        }

        void check(APIMessage msg) {
            this.msg = msg;
            if (msg.getClass().isAnnotationPresent(SuppressCredentialCheck.class)) {
                return;
            }

            DebugUtils.Assert(msg.getSession() != null, "session cannot be null");
            session = msg.getSession();

            action = actions.get(msg.getClass());
            policyCheck();
        }

        private void accountFieldCheck() throws IllegalAccessException {
            Set resourceUuids = new HashSet();
            Set operationTargetResourceUuids = new HashSet();

            for (AccountCheckField af : action.accountCheckFields) {
                Object value = af.field.get(msg);
                if (value == null) {
                    continue;
                }

                if (String.class.isAssignableFrom(af.field.getType())) {
                    if (af.param.operationTarget()) {
                        operationTargetResourceUuids.add(value);
                    } else {
                        resourceUuids.add(value);
                    }
                } else if (Collection.class.isAssignableFrom(af.field.getType())) {
                    if (af.param.operationTarget()) {
                        operationTargetResourceUuids.addAll((Collection) value);
                    } else {
                        resourceUuids.addAll((Collection) value);
                    }
                }
            }

            if (resourceUuids.isEmpty() && operationTargetResourceUuids.isEmpty()) {
                return;
            }

            // if a resource uuid represents an operation target, it cannot be bypassed by
            // the shared resources, as we don't support roles for cross-account sharing.
            if (!resourceUuids.isEmpty()) {
                SimpleQuery<SharedResourceVO> sq = dbf.createQuery(SharedResourceVO.class);
                sq.select(SharedResourceVO_.receiverAccountUuid, SharedResourceVO_.toPublic, SharedResourceVO_.resourceUuid);
                sq.add(SharedResourceVO_.resourceUuid, Op.IN, resourceUuids);
                List<Tuple> ts = sq.listTuple();
                for (Tuple t : ts) {
                    String ruuid = t.get(0, String.class);
                    Boolean toPublic = t.get(1, Boolean.class);
                    String resUuid = t.get(2, String.class);
                    if (toPublic || session.getAccountUuid().equals(ruuid)) {
                        // this resource is shared to the account
                        resourceUuids.remove(resUuid);
                    }
                }
            }

            resourceUuids.addAll(operationTargetResourceUuids);
            if (resourceUuids.isEmpty()) {
                return;
            }

            List<Tuple> ts = SQL.New(
                    " select avo.name ,arrf.accountUuid ,arrf.resourceUuid ,arrf.resourceType " +
                            "from AccountResourceRefVO arrf ,AccountVO avo " +
                            "where arrf.resourceUuid in (:resourceUuids) and avo.uuid = arrf.accountUuid",Tuple.class)
                    .param("resourceUuids",resourceUuids).list();

            for (Tuple t : ts) {
                String resourceOwnerName = t.get(0, String.class);
                String resourceOwnerAccountUuid = t.get(1, String.class);
                String resourceUuid = t.get(2, String.class);
                String resourceType = t.get(3, String.class);
                if (!session.getAccountUuid().equals(resourceOwnerAccountUuid)) {
                    throw new ApiMessageInterceptionException(err(IdentityErrors.PERMISSION_DENIED,
                            "operation denied. The resource[uuid: %s, type: %s,ownerAccountName:%s, ownerAccountUuid:%s] doesn't belong to the account[uuid: %s]",
                            resourceUuid, resourceType, resourceOwnerName, resourceOwnerAccountUuid, session.getAccountUuid()
                    ));
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("account-check pass. The resource[uuid: %s, type: %s] belongs to the account[uuid: %s]",
                                resourceUuid, resourceType, session.getAccountUuid()));
                    }
                }
            }
        }

        private void useDecision(Decision d, boolean userPolicy) {
            String policyCategory = userPolicy ? "user policy" : "group policy";

            if (d.effect == StatementEffect.Allow) {
                logger.debug(String.format("API[name: %s, action: %s] is approved by a %s[name: %s, uuid: %s]," +
                                " statement[name: %s, action: %s]", msg.getClass().getSimpleName(), d.action,
                        policyCategory, d.policy.getName(), d.policy.getUuid(), d.statement.getName(), d.actionRule));
            } else {
                logger.debug(String.format("API[name: %s, action: %s] is denied by a %s[name: %s, uuid: %s]," +
                                " statement[name: %s, action: %s]", msg.getClass().getSimpleName(), d.action,
                        policyCategory, d.policy.getName(), d.policy.getUuid(), d.statement.getName(), d.actionRule));

                throw new ApiMessageInterceptionException(err(IdentityErrors.PERMISSION_DENIED,
                        "%s denied. user[name: %s, uuid: %s] is denied to execute API[%s]",
                        policyCategory, username, session.getUuid(), msg.getClass().getSimpleName()
                ));
            }
        }

        private void policyCheck() {
            if (new QuotaUtil().isAdminAccount(session.getAccountUuid())) {
                return;
            }

            if (action.adminOnly) {
                throw new ApiMessageInterceptionException(err(IdentityErrors.PERMISSION_DENIED,
                        "API[%s] is admin only", msg.getClass().getSimpleName()));
            }

            if (action.accountOnly && !session.isAccountSession()) {
                throw new ApiMessageInterceptionException(err(IdentityErrors.PERMISSION_DENIED,
                        "API[%s] can only be called by an account, the current session is a user session[user uuid:%s]",
                        msg.getClass().getSimpleName(), session.getUserUuid()
                ));
            }

            if (action.accountCheckFields != null && !action.accountCheckFields.isEmpty()) {
                try {
                    accountFieldCheck();
                } catch (ApiMessageInterceptionException ae) {
                    throw ae;
                } catch (Exception e) {
                    throw new CloudRuntimeException(e);
                }
            }

            if (action.accountControl) {
                boolean allow = false;
                for (Class clz : accountApiControl) {
                    if (clz.isAssignableFrom(msg.getClass())) {
                        allow = true;
                        break;
                    }
                }

                if (!allow) {
                    for (Class clz : accountApiControlInternal) {
                        if (clz.isAssignableFrom(msg.getClass())) {
                            allow = true;
                            break;
                        }
                    }
                }

                if (!allow) {
                    throw new ApiMessageInterceptionException(err(IdentityErrors.PERMISSION_DENIED,
                            "the API[%s] is not allowed for normal accounts", msg.getClass()
                    ));
                }
            }

            if (session.isAccountSession()) {
                return;
            }

            SimpleQuery<UserVO> uq = dbf.createQuery(UserVO.class);
            uq.select(UserVO_.name);
            uq.add(UserVO_.uuid, Op.EQ, session.getUserUuid());
            username = uq.findValue();

            List<PolicyInventory> userPolicies = getUserPolicies();
            Decision d = decide(userPolicies);
            if (d != null) {
                useDecision(d, true);
                return;
            }

            List<PolicyInventory> groupPolicies = getGroupPolicies();
            d = decide(groupPolicies);
            if (d != null) {
                useDecision(d, false);
                return;
            }

            throw new ApiMessageInterceptionException(err(IdentityErrors.PERMISSION_DENIED,
                    "user[name: %s, uuid: %s] has no policy set for this operation, API[%s] is denied by default. You may either create policies for this user" +
                            " or add the user into a group with polices set", username, session.getUserUuid(), msg.getClass().getSimpleName()
            ));
        }


        @Transactional(readOnly = true)
        private List<PolicyInventory> getGroupPolicies() {
            String sql = "select p" +
                    " from PolicyVO p, UserGroupUserRefVO ref, UserGroupPolicyRefVO gref" +
                    " where p.uuid = gref.policyUuid" +
                    " and gref.groupUuid = ref.groupUuid" +
                    " and ref.userUuid = :uuid";
            TypedQuery<PolicyVO> q = dbf.getEntityManager().createQuery(sql, PolicyVO.class);
            q.setParameter("uuid", session.getUserUuid());
            return PolicyInventory.valueOf(q.getResultList());
        }

        class Decision {
            PolicyInventory policy;
            String action;
            PolicyStatement statement;
            String actionRule;
            StatementEffect effect;
        }

        private Decision decide(List<PolicyInventory> userPolicies) {
            for (String a : action.actions) {
                for (PolicyInventory p : userPolicies) {
                    for (PolicyStatement s : p.getStatements()) {
                        for (String ac : s.getActions()) {
                            Pattern pattern = Pattern.compile(ac);
                            Matcher m = pattern.matcher(a);
                            boolean ret = m.matches();
                            if (ret) {
                                Decision d = new Decision();
                                d.policy = p;
                                d.action = a;
                                d.statement = s;
                                d.actionRule = ac;
                                d.effect = s.getEffect();
                                return d;
                            }

                            if (logger.isTraceEnabled()) {
                                logger.trace(String.format("API[name: %s, action: %s] is not matched by policy[name: %s, uuid: %s" +
                                                ", statement[name: %s, action: %s, effect: %s]", msg.getClass().getSimpleName(),
                                        a, p.getName(), p.getUuid(), s.getName(), ac, s.getEffect()));
                            }
                        }
                    }
                }
            }

            return null;
        }

        @Transactional(readOnly = true)
        private List<PolicyInventory> getUserPolicies() {
            String sql = "select p from PolicyVO p, UserPolicyRefVO ref where ref.userUuid = :uuid and ref.policyUuid = p.uuid";
            TypedQuery<PolicyVO> q = dbf.getEntityManager().createQuery(sql, PolicyVO.class);
            q.setParameter("uuid", session.getUserUuid());
            return PolicyInventory.valueOf(q.getResultList());
        }

        private void sessionCheck() {
            if (msg.getSession() == null) {
                throw new ApiMessageInterceptionException(err(IdentityErrors.INVALID_SESSION,
                        "session of message[%s] is null", msg.getMessageName()));
            }

            if (msg.getSession().getUuid() == null) {
                throw new ApiMessageInterceptionException(err(IdentityErrors.INVALID_SESSION,
                        "session uuid is null"));
            }

            SessionInventory session = Session.getSession(msg.getSession().getUuid());
            if (session == null) {
                throw new ApiMessageInterceptionException(err(IdentityErrors.INVALID_SESSION,
                        "Session expired"));
            }

            Timestamp curr = getCurrentSqlDate();
            if (curr.after(session.getExpiredDate())) {
                logger.debug(String.format("session expired[%s < %s] for account[uuid:%s]", curr,
                        session.getExpiredDate(), session.getAccountUuid()));
                logOutSession(session.getUuid());
                throw new ApiMessageInterceptionException(err(IdentityErrors.INVALID_SESSION, "Session expired"));
            }

            this.session = session;
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        //new Auth().validate(msg);

        if (msg instanceof APIUpdateAccountMsg) {
            validate((APIUpdateAccountMsg) msg);
        } else if (msg instanceof APICreatePolicyMsg) {
            validate((APICreatePolicyMsg) msg);
        } else if (msg instanceof APIAddUserToGroupMsg) {
            validate((APIAddUserToGroupMsg) msg);
        } else if (msg instanceof APIAttachPolicyToUserGroupMsg) {
            validate((APIAttachPolicyToUserGroupMsg) msg);
        } else if (msg instanceof APIAttachPolicyToUserMsg) {
            validate((APIAttachPolicyToUserMsg) msg);
        } else if (msg instanceof APIDetachPolicyFromUserGroupMsg) {
            validate((APIDetachPolicyFromUserGroupMsg) msg);
        } else if (msg instanceof APIDetachPolicyFromUserMsg) {
            validate((APIDetachPolicyFromUserMsg) msg);
        } else if (msg instanceof APIShareResourceMsg) {
            validate((APIShareResourceMsg) msg);
        } else if (msg instanceof APIRevokeResourceSharingMsg) {
            validate((APIRevokeResourceSharingMsg) msg);
        } else if (msg instanceof APIUpdateUserMsg) {
            validate((APIUpdateUserMsg) msg);
        } else if (msg instanceof APIDeleteAccountMsg) {
            validate((APIDeleteAccountMsg) msg);
        } else if (msg instanceof APICreateAccountMsg) {
            validate((APICreateAccountMsg) msg);
        } else if (msg instanceof APICreateUserMsg) {
            validate((APICreateUserMsg) msg);
        } else if (msg instanceof APICreateUserGroupMsg) {
            validate((APICreateUserGroupMsg) msg);
        } else if (msg instanceof APILogInByUserMsg) {
            validate((APILogInByUserMsg) msg);
        } else if (msg instanceof APIGetAccountQuotaUsageMsg) {
            validate((APIGetAccountQuotaUsageMsg) msg);
        } else if (msg instanceof APIUpdateQuotaMsg) {
            validate((APIUpdateQuotaMsg) msg);
        }

        setServiceId(msg);

        return msg;
    }

    private void validate(APIGetAccountQuotaUsageMsg msg) {
        if (msg.getUuid() == null) {
            msg.setUuid(msg.getSession().getAccountUuid());
        }
    }

    private void validate(APILogInByUserMsg msg) {
        if (msg.getAccountName() == null && msg.getAccountUuid() == null) {
            throw new ApiMessageInterceptionException(argerr(
                    "accountName and accountUuid cannot both be null, you must specify at least one"
            ));
        }
    }

    private void validate(APICreateUserGroupMsg msg) {
        SimpleQuery<UserGroupVO> q = dbf.createQuery(UserGroupVO.class);
        q.add(UserGroupVO_.accountUuid, Op.EQ, msg.getAccountUuid());
        q.add(UserGroupVO_.name, Op.EQ, msg.getName());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(argerr("unable to create a group. A group called %s is already under the account[uuid:%s]",
                            msg.getName(), msg.getAccountUuid()));
        }
    }

    private void validate(APICreateUserMsg msg) {
        SimpleQuery<UserVO> q = dbf.createQuery(UserVO.class);
        q.add(UserVO_.accountUuid, Op.EQ, msg.getAccountUuid());
        q.add(UserVO_.name, Op.EQ, msg.getName());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(argerr("unable to create a user. A user called %s is already under the account[uuid:%s]",
                            msg.getName(), msg.getAccountUuid()));
        }
    }

    private void validate(APICreateAccountMsg msg) {
        SimpleQuery<AccountVO> q = dbf.createQuery(AccountVO.class);
        q.add(AccountVO_.name, Op.EQ, msg.getName());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(argerr("unable to create an account. An account already called %s", msg.getName()));
        }
    }

    private void validate(APIDeleteAccountMsg msg) {
        if (new QuotaUtil().isAdminAccount(msg.getUuid())) {
            if (msg.getAccountUuid().equals(msg.getSession().getAccountUuid())) {
                throw new ApiMessageInterceptionException(argerr(
                        "account cannot delete itself"
                ));
            }

            if (msg.getAccountUuid().equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)) {
                throw new ApiMessageInterceptionException(argerr(
                        "cannot delete builtin admin account."
                ));
            }
        }
        if(!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())){
            throw new ApiMessageInterceptionException(argerr(
                    "Only admin can delete account."
            ));
        }
    }

    private void validate(APIUpdateUserMsg msg) {
        if (msg.getUuid() == null && msg.getSession().isAccountSession()) {
            throw new ApiMessageInterceptionException(argerr(
                    "the current session is an account session. You need to specify the field 'uuid' of the user" +
                            " you want to update"
            ));
        }

        if (msg.getSession().isAccountSession()) {
            return;
        }

        if (AccountConstant.isAdminPermission(msg.getSession())) {
            return;
        }

        if (msg.getUuid() != null && !msg.getSession().getUserUuid().equals(msg.getUuid())) {
            throw new ApiMessageInterceptionException(argerr("your are login as a user, you cannot another user[uuid:%s]", msg.getUuid()));
        }

        msg.setUuid(msg.getSession().getUserUuid());
    }

    private void validate(APIRevokeResourceSharingMsg msg) {
        if (!msg.isAll() && (msg.getAccountUuids() == null || msg.getAccountUuids().isEmpty())) {
            throw new ApiMessageInterceptionException(argerr(
                    "all is set to false, accountUuids cannot be null or empty"
            ));
        }
    }

    private void validate(APIShareResourceMsg msg) {
        if (!msg.isToPublic() && (msg.getAccountUuids() == null || msg.getAccountUuids().isEmpty())) {
            throw new ApiMessageInterceptionException(argerr(
                    "toPublic is set to false, accountUuids cannot be null or empty"
            ));
        }
    }

    private void validate(APIDetachPolicyFromUserMsg msg) {
        PolicyVO policy = dbf.findByUuid(msg.getPolicyUuid(), PolicyVO.class);
        UserVO user = dbf.findByUuid(msg.getUserUuid(), UserVO.class);
        if (!policy.getAccountUuid().equals(msg.getAccountUuid())) {
            throw new ApiMessageInterceptionException(argerr("policy[name: %s, uuid: %s] doesn't belong to the account[uuid: %s]",
                            policy.getName(), policy.getUuid(), msg.getSession().getAccountUuid()));
        }
        if (!user.getAccountUuid().equals(msg.getAccountUuid())) {
            throw new ApiMessageInterceptionException(argerr("user[name: %s, uuid: %s] doesn't belong to the account[uuid: %s]",
                            user.getName(), user.getUuid(), msg.getSession().getAccountUuid()));
        }
    }

    private void validate(APIDetachPolicyFromUserGroupMsg msg) {
        PolicyVO policy = dbf.findByUuid(msg.getPolicyUuid(), PolicyVO.class);
        UserGroupVO group = dbf.findByUuid(msg.getGroupUuid(), UserGroupVO.class);
        if (!policy.getAccountUuid().equals(msg.getAccountUuid())) {
            throw new ApiMessageInterceptionException(argerr("policy[name: %s, uuid: %s] doesn't belong to the account[uuid: %s]",
                            policy.getName(), policy.getUuid(), msg.getSession().getAccountUuid()));
        }
        if (!group.getAccountUuid().equals(msg.getAccountUuid())) {
            throw new ApiMessageInterceptionException(argerr("group[name: %s, uuid: %s] doesn't belong to the account[uuid: %s]",
                            group.getName(), group.getUuid(), msg.getSession().getAccountUuid()));
        }
    }

    private void validate(APIAttachPolicyToUserMsg msg) {
        PolicyVO policy = dbf.findByUuid(msg.getPolicyUuid(), PolicyVO.class);
        UserVO user = dbf.findByUuid(msg.getUserUuid(), UserVO.class);
        if (!policy.getAccountUuid().equals(msg.getAccountUuid())) {
            throw new ApiMessageInterceptionException(argerr("policy[name: %s, uuid: %s] doesn't belong to the account[uuid: %s]",
                            policy.getName(), policy.getUuid(), msg.getSession().getAccountUuid()));
        }
        if (!user.getAccountUuid().equals(msg.getAccountUuid())) {
            throw new ApiMessageInterceptionException(argerr("user[name: %s, uuid: %s] doesn't belong to the account[uuid: %s]",
                            user.getName(), user.getUuid(), msg.getSession().getAccountUuid()));
        }
    }

    private void validate(APIAttachPolicyToUserGroupMsg msg) {
        PolicyVO policy = dbf.findByUuid(msg.getPolicyUuid(), PolicyVO.class);
        UserGroupVO group = dbf.findByUuid(msg.getGroupUuid(), UserGroupVO.class);
        if (!policy.getAccountUuid().equals(msg.getAccountUuid())) {
            throw new ApiMessageInterceptionException(argerr("policy[name: %s, uuid: %s] doesn't belong to the account[uuid: %s]",
                            policy.getName(), policy.getUuid(), msg.getSession().getAccountUuid()));
        }

        if (!group.getAccountUuid().equals(msg.getAccountUuid())) {
            throw new ApiMessageInterceptionException(argerr("group[name: %s, uuid: %s] doesn't belong to the account[uuid: %s]",
                            group.getName(), group.getUuid(), msg.getSession().getAccountUuid()));
        }
    }

    private void validate(APIAddUserToGroupMsg msg) {
        UserVO user = dbf.findByUuid(msg.getUserUuid(), UserVO.class);
        UserGroupVO group = dbf.findByUuid(msg.getGroupUuid(), UserGroupVO.class);
        if (!user.getAccountUuid().equals(msg.getAccountUuid())) {
            throw new ApiMessageInterceptionException(argerr("user[name: %s, uuid: %s] doesn't belong to the account[uuid: %s]",
                            user.getName(), user.getUuid(), msg.getSession().getAccountUuid()));
        }
        if (!group.getAccountUuid().equals(msg.getSession().getAccountUuid())) {
            throw new ApiMessageInterceptionException(argerr("group[name: %s, uuid: %s] doesn't belong to the account[uuid: %s]",
                            group.getName(), group.getUuid(), msg.getSession().getAccountUuid()));
        }
    }

    private void validate(APICreatePolicyMsg msg) {
        boolean sessionAccessToAdminActions = new CheckIfSessionCanOperationAdminPermission().check(msg.getSession());

        for (PolicyStatement s : msg.getStatements()) {
            if (s.getEffect() == null) {
                throw new ApiMessageInterceptionException(argerr("a statement must have effect field. Invalid statement[%s]", JSONObjectUtil.toJsonString(s)));
            }
            if (s.getActions() == null) {
                throw new ApiMessageInterceptionException(argerr("a statement must have action field. Invalid statement[%s]", JSONObjectUtil.toJsonString(s)));
            }
            if (s.getActions().isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("a statement must have a non-empty action field. Invalid statement[%s]",
                                JSONObjectUtil.toJsonString(s)));
            }

            if (sessionAccessToAdminActions) {
                continue;
            }

            if (s.getActions() != null) {
                s.getActions().forEach(as -> {
                    if (PolicyUtils.isAdminOnlyAction(as)) {
                        throw new OperationFailureException(err(IdentityErrors.PERMISSION_DENIED, "normal accounts can't create admin-only action polices[%s]", as));
                    }
                });
            }
        }
    }

    private void validate(APIUpdateAccountMsg msg) {
        AccountVO a = dbf.findByUuid(msg.getSession().getAccountUuid(), AccountVO.class);

        if (msg.getName() != null) {
            SimpleQuery<AccountVO> q = dbf.createQuery(AccountVO.class);
            q.add(AccountVO_.name, Op.EQ, msg.getName());
            if (q.isExists()) {
                throw new ApiMessageInterceptionException(argerr("unable to update name. An account already called %s", msg.getName()));
            }
        }

        if (msg.getUuid() == null) {
            msg.setUuid(msg.getSession().getAccountUuid());
        }
        AccountVO account = dbf.findByUuid(msg.getUuid(), AccountVO.class);


        if (msg.getOldPassword() != null && !msg.getOldPassword().equals(account.getPassword())) {
            throw new OperationFailureException(operr("old password is not equal to the original password, cannot update the password of account[uuid: %s]", msg.getUuid()));
        }

        if (a.getType() == AccountType.SystemAdmin) {
            if (msg.getName() != null && (msg.getUuid() == null || msg.getUuid().equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID))) {
                throw new OperationFailureException(operr(
                        "the name of admin account cannot be updated"
                ));
            }

            if (msg.getPassword() != null && (!AccountConstant.isAdminPermission(msg.getSession()))) {
                throw new OperationFailureException(operr("only admin account can update it's password"));
            }

            return;
        }

        if (!account.getUuid().equals(a.getUuid())) {
            throw new OperationFailureException(operr("account[uuid: %s, name: %s] is a normal account, it cannot reset the password of another account[uuid: %s]",
                            account.getUuid(), account.getName(), msg.getUuid()));
        }
    }

    private void validate(APIUpdateQuotaMsg msg) {
        QuotaVO quota = Q.New(QuotaVO.class)
                .eq(QuotaVO_.identityUuid, msg.getIdentityUuid())
                .eq(QuotaVO_.name, msg.getName())
                .find();
        if (quota == null) {
            throw new OperationFailureException(argerr("cannot find Quota[name: %s] for the account[uuid: %s]", msg.getName(), msg.getIdentityUuid()));
        }

        List<QuotaUpdateChecker> checkers = quotaChangeCheckers.stream()
                .filter(checker -> checker.type().contains(quota.getIdentityType())).collect(Collectors.toList());
        if (checkers.isEmpty()) {
            throw new ApiMessageInterceptionException(
                    argerr("can not find quota update checker for quota[uuid:%s, type:%s]", quota.getIdentityUuid(), quota.getIdentityType()));
        }

        for (QuotaUpdateChecker checker : checkers) {
            logger.debug(String.format("check quota[uuid:%s, type:%s] updated by %s",
                    quota.getIdentityUuid(), quota.getIdentityType(), checker.getClass().getSimpleName()));
            ErrorCode errorCode = checker.check(quota, msg.getValue());
            if (errorCode != null) {
                throw new ApiMessageInterceptionException(
                        operr(errorCode, "cannot update Quota[name: %s] for the account[uuid: %s]", msg.getName(), msg.getIdentityUuid()));
            }
        }

        msg.setQuotaVO(quota);
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof AccountMessage) {
            AccountMessage amsg = (AccountMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, AccountConstant.SERVICE_ID, amsg.getAccountUuid());
        }
    }

    public Map<String, SessionInventory> getSessionsCopy() {
        return Session.getSessionsCopy();
    }

    @Override
    public RestAuthenticationType getAuthenticationType() {
        return ACCOUNT_REST_AUTHENTICATION_TYPE;
    }

    @Override
    public SessionInventory doAuth(RestAuthenticationParams params) {
        SessionVO vo = Q.New(SessionVO.class).eq(SessionVO_.uuid, params.authKey).find();
        if (vo != null) {
            return SessionInventory.valueOf(vo);
        }

        /* invalid session error should be raised in ApiMessageProcessorImpl */
        SessionInventory session = new SessionInventory();
        session.setUuid(params.authKey);
        return session;
    }
}
