package org.zstack.identity;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.db.TransactionalCallback.Operation;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.AbstractService;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.search.SearchOp;
import org.zstack.search.GetQuery;
import org.zstack.search.SearchQuery;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AccountManagerImpl extends AbstractService implements AccountManager, PrepareDbInitialValueExtensionPoint,
        SoftDeleteEntityExtensionPoint, HardDeleteEntityExtensionPoint {
    private static final CLogger logger = Utils.getLogger(AccountManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private DbEntityLister dl;
    @Autowired
    private CredentialChecker credentialChecker;
    @Autowired
    private ErrorFacade errf;

    private static final String DEFAULT_ACCOUNT_ROLE = "DefaultAccountRole";

    private List<String> resourceTypeForAccountRef;
    private List<Class> resourceTypes;

    @Override
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

    private void passThrough(AccountMessage msg) {
        AccountVO vo = dbf.findByUuid(msg.getAccountUuid(), AccountVO.class);
        if (vo == null) {
            String err = String.format("unable to find account[uuid=%s]", msg.getAccountUuid());
            bus.replyErrorByMessageType((Message) msg, errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND, err));
            return;
        }

        AccountBase base = new AccountBase(vo);
        base.handleMessage((Message) msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateAccountMsg) {
            handle((APICreateAccountMsg) msg);
        } else if (msg instanceof APIListAccountMsg) {
            handle((APIListAccountMsg) msg);
        } else if (msg instanceof APIListUserMsg) {
            handle((APIListUserMsg) msg);
        } else if (msg instanceof APIListPolicyMsg) {
            handle((APIListPolicyMsg) msg);
        } else if (msg instanceof AccountMessage) {
            passThrough((AccountMessage) msg);
        } else if (msg instanceof APILogInByAccountMsg) {
            handle((APILogInByAccountMsg) msg);
        } else if (msg instanceof APILogInByUserMsg) {
            handle((APILogInByUserMsg)msg);
        } else if (msg instanceof APILogOutMsg) {
            handle((APILogOutMsg) msg);
        } else if (msg instanceof APISearchAccountMsg) {
            handle((APISearchAccountMsg) msg);
        } else if (msg instanceof APIGetAccountMsg) {
            handle((APIGetAccountMsg) msg);
        } else if (msg instanceof APIGetUserMsg) {
            handle((APIGetUserMsg) msg);
        } else if (msg instanceof APIGetUserGroupMsg) {
            handle((APIGetUserGroupMsg) msg);
        } else if (msg instanceof APIGetPolicyMsg) {
            handle((APIGetPolicyMsg) msg);
        } else if (msg instanceof APIValidateSessionMsg) {
            handle((APIValidateSessionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIValidateSessionMsg msg) {
        APIValidateSessionReply reply = new APIValidateSessionReply();
        try {
            credentialChecker.validateSession(msg);
            reply.setValidSession(true);
        } catch (BadCredentialsException e) {
            reply.setValidSession(false);
        }
        bus.reply(msg, reply);
    }

    private void handle(APIGetPolicyMsg msg) {
        SearchQuery<PolicyInventory> q = new SearchQuery<PolicyInventory>(PolicyInventory.class);
        q.addAccountAsAnd(msg);
        q.add("uuid", SearchOp.AND_EQ, msg.getUuid());
        List<PolicyInventory> invs = q.list();
        APIGetPolicyReply reply = new APIGetPolicyReply();
        if (!invs.isEmpty()) {
            reply.setInventory(JSONObjectUtil.toJsonString(invs.get(0)));
        }
        bus.reply(msg, reply);
    }

    private void handle(APIGetUserGroupMsg msg) {
        SearchQuery<UserGroupInventory> q = new SearchQuery<UserGroupInventory>(UserGroupInventory.class);
        q.addAccountAsAnd(msg);
        q.add("uuid", SearchOp.AND_EQ, msg.getUuid());
        List<UserGroupInventory> invs = q.list();
        APIGetUserGroupReply reply = new APIGetUserGroupReply();
        if (!invs.isEmpty()) {
            reply.setInventory(JSONObjectUtil.toJsonString(invs.get(0)));
        }
        bus.reply(msg, reply);
    }

    private void handle(APIGetUserMsg msg) {
        SearchQuery<UserInventory> q = new SearchQuery(UserInventory.class);
        q.addAccountAsAnd(msg);
        q.add("uuid", SearchOp.AND_EQ, msg.getUuid());
        List<UserInventory> invs = q.list();
        APIGetUserReply reply = new APIGetUserReply();
        if (!invs.isEmpty()) {
            UserInventory inv = invs.get(0);
            reply.setInventory(JSONObjectUtil.toJsonString(inv));
        }
        bus.reply(msg, reply);
    }

    private void handle(APIGetAccountMsg msg) {
        GetQuery q = new GetQuery();
        String res = q.getAsString(msg, AccountInventory.class);
        APIGetAccountReply reply = new APIGetAccountReply();
        reply.setInventory(res);
        bus.reply(msg, reply);
    }

    private void handle(APISearchAccountMsg msg) {
        SearchQuery<AccountInventory> query = SearchQuery.create(msg, AccountInventory.class);
        String content = query.listAsString();
        APISearchAccountReply reply = new APISearchAccountReply();
        reply.setContent(content);
        bus.reply(msg, reply);
    }

    private void handle(APILogOutMsg msg) {
        APILogOutReply reply = new APILogOutReply();
        credentialChecker.logOutSession(msg.getSessionUuid());
        bus.reply(msg, reply);
    }

    private void handle(APILogInByUserMsg msg) {
        APILogInReply reply = new APILogInReply();
        try {
            SessionInventory inv = credentialChecker.authenticateByUser(msg.getAccountUuid(), msg.getUserName(), msg.getPassword());
            reply.setInventory(inv);
        } catch (CredentialDeniedException e) {
            logger.trace("", e);
            reply.setError(errf.instantiateErrorCode(IdentityErrors.AUTHENTICATION_ERROR, e.getError()));
        }
        bus.reply(msg, reply);
    }

    private void handle(APILogInByAccountMsg msg) {
        APILogInReply reply = new APILogInReply();
        try {
            SessionInventory inv = credentialChecker.authenticateByAccount(msg.getAccountName(), msg.getPassword());
            reply.setInventory(inv);
        } catch (CredentialDeniedException e) {
            logger.trace("", e);
            reply.setError(errf.instantiateErrorCode(IdentityErrors.AUTHENTICATION_ERROR, e.getError()));
        }
        bus.reply(msg, reply);
    }

    private void handle(APIListPolicyMsg msg) {
        List<PolicyVO> vos = dl.listByApiMessage(msg, PolicyVO.class);
        List<PolicyInventory> invs = PolicyInventory.valueOf(vos);
        APIListPolicyReply reply = new APIListPolicyReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void handle(APIListUserMsg msg) {
        List<UserVO> vos = dl.listByApiMessage(msg, UserVO.class);
        List<UserInventory> invs = UserInventory.valueOf(vos);
        APIListUserReply reply = new APIListUserReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void handle(APIListAccountMsg msg) {
        List<AccountVO> vos = dl.listByApiMessage(msg, AccountVO.class);
        List<AccountInventory> invs = AccountInventory.valueOf(vos);
        APIListAccountReply reply = new APIListAccountReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private String getDefaultAccountRoleDoc() {
        List<String> roles = new ArrayList<String>();
        roles.add(IdentityRoles.ALL_ACCOUNT_ROLES);
        return PolicyDoc.generatePolicyXmlData(AccountConstant.StatementEffect.Allow.toString(), roles);
    }

    @Transactional
    private void handle(APICreateAccountMsg msg) {
        dbf.entityForTranscationCallback(Operation.PERSIST, AccountVO.class, UserVO.class, UserPolicyRefVO.class);
        AccountVO vo = new AccountVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setPassword(msg.getPassword());
        vo.setType(AccountType.Normal);
        dbf.getEntityManager().persist(vo);
        UserVO uvo = new UserVO();
        uvo.setUuid(vo.getUuid());
        uvo.setAccountUuid(vo.getUuid());
        uvo.setName(vo.getName());
        uvo.setPassword(vo.getPassword());
        dbf.getEntityManager().persist(uvo);

        SimpleQuery<PolicyVO> pq = dbf.createQuery(PolicyVO.class);
        pq.select(PolicyVO_.uuid);
        pq.add(PolicyVO_.name, Op.EQ, DEFAULT_ACCOUNT_ROLE);
        pq.add(PolicyVO_.type, Op.EQ, PolicyType.System);
        String policyUUid = pq.findValue();

        UserPolicyRefVO upvo = new UserPolicyRefVO();
        upvo.setPolicyUuid(policyUUid);
        upvo.setUserUuid(uvo.getUuid());
        dbf.getEntityManager().persist(upvo);

        dbf.getEntityManager().flush();
        dbf.getEntityManager().refresh(vo);

        AccountInventory inv = AccountInventory.valueOf(vo);
        APICreateAccountEvent evt = new APICreateAccountEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(AccountConstant.SERVICE_ID);
    }

    private void buildResourceTypes() throws ClassNotFoundException {
        resourceTypes = new ArrayList<Class>();
        for (String resrouceTypeName : resourceTypeForAccountRef) {
            Class<?> rs = Class.forName(resrouceTypeName);
            resourceTypes.add(rs);
        }
    }

    @Override
    public boolean start() {
        try {
            buildResourceTypes();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Transactional
    private void createInitialSystemAdminAccountAndUser(String defaultAccountRoleData) throws IOException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("SystemAdminPolicy.xml");
        if (is == null) {
            throw new CloudRuntimeException(String.format("Cannot find default SystemAdminPolicy.xml in classpath"));
        }
        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer);
        String policyData = writer.toString();

        dbf.entityForTranscationCallback(Operation.PERSIST, AccountVO.class, UserVO.class, PolicyVO.class, UserPolicyRefVO.class);
        AccountVO vo = new AccountVO();
        vo.setUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        vo.setName(AccountConstant.INITIAL_SYSTEM_ADMIN_NAME);
        vo.setPassword(AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD);
        vo.setType(AccountType.SystemAdmin);
        dbf.getEntityManager().persist(vo);
        UserVO uvo = new UserVO();
        uvo.setUuid(vo.getUuid());
        uvo.setAccountUuid(vo.getUuid());
        uvo.setName(AccountConstant.INITIAL_SYSTEM_ADMIN_NAME);
        uvo.setPassword(AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD);
        dbf.getEntityManager().persist(uvo);
        PolicyVO pvo = new PolicyVO();
        pvo.setUuid(Platform.getUuid());
        pvo.setType(PolicyType.System);
        pvo.setAccountUuid(vo.getUuid());
        pvo.setData(policyData);
        pvo.setName("SystemAdminPolicy");
        pvo.setDescription("Default policy granting all permission to system admin");
        dbf.getEntityManager().persist(pvo);
        UserPolicyRefVO upvo = new UserPolicyRefVO();
        upvo.setPolicyUuid(pvo.getUuid());
        upvo.setUserUuid(uvo.getUuid());
        dbf.getEntityManager().persist(upvo);
        PolicyVO apvo = new PolicyVO();
        apvo.setUuid(Platform.getUuid());
        apvo.setAccountUuid(vo.getUuid());
        apvo.setType(PolicyType.System);
        apvo.setData(defaultAccountRoleData);
        apvo.setName(DEFAULT_ACCOUNT_ROLE);
        apvo.setDescription(DEFAULT_ACCOUNT_ROLE);
        dbf.getEntityManager().persist(apvo);
    }

    @Override
    public void prepareDbInitialValue() {
        try {
            SimpleQuery<AccountVO> q = dbf.createQuery(AccountVO.class);
            q.add(AccountVO_.name, Op.EQ, AccountConstant.INITIAL_SYSTEM_ADMIN_NAME);
            long account = q.count();
            if (account == 0) {
                createInitialSystemAdminAccountAndUser(getDefaultAccountRoleDoc());
                logger.debug(String.format("Created initial system admin account[name:%s]", AccountConstant.INITIAL_SYSTEM_ADMIN_NAME));
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to create default system admin account", e);
        }
    }

    private boolean isAccountExisted(String accountIdentify, boolean isUuid) {
        SimpleQuery<AccountVO> q = dbf.createQuery(AccountVO.class);
        if (isUuid) {
            q.add(AccountVO_.uuid, Op.EQ, accountIdentify);
        } else {
            q.add(AccountVO_.name, Op.EQ, accountIdentify);
        }
        long count = q.count();
        return count != 0;
    }

    @Transactional
    private boolean isUserExisted(String userUuid) {
        SimpleQuery<UserVO> q = dbf.createQuery(UserVO.class);
        q.add(UserVO_.uuid, Op.EQ, userUuid);
        long count = q.count();
        return count != 0;
    }

    public void setResourceTypeForAccountRef(List<String> resourceTypeForAccountRef) {
        this.resourceTypeForAccountRef = resourceTypeForAccountRef;
    }

    @Override
    public void createAccountResourceRef(String accountUuid, String resourceUuid, Class<?> resourceClass) {
        if (!resourceTypes.contains(resourceClass)) {
           throw new CloudRuntimeException(String.format("%s is not listed in resourceTypeForAccountRef of AccountManager.xml that is spring configuration. you forgot it???", resourceClass.getName()));
        }

        AccountResourceRefVO ref = AccountResourceRefVO.newOwn(accountUuid, resourceUuid, resourceClass);
        dbf.persist(ref);
    }

    @Override
    public boolean isResourceHavingAccountReference(Class entityClass) {
        return resourceTypes.contains(entityClass);
    }

    @Override
    public List<String> getSiblingResourceUuids(String res1Uuid, String res1Type, String res2Type) {
        String sql = "select ref2.resourceUuid from AccountResourceRefVO ref1, AccountResourceRefVO ref2 where (ref1.accountUuid = ref2.accountUuid or ref1.accountUuid = ref2.ownerAccountUuid or ref1.ownerAccountUuid = ref2.accountUuid or ref1.ownerAccountUuid = ref2.ownerAccountUuid) and ref1.resourceUuid = :res1Uuid and ref1.resourceType = :res1Type and ref2.resourceType = :res2Type";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("res1Uuid", res1Uuid);
        q.setParameter("res1Type", res1Type);
        q.setParameter("res2Type", res2Type);
        return q.getResultList();
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
}
