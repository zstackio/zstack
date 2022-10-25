package org.zstack.identity;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.IdentityCanonicalEvents.AccountDeletedData;
import org.zstack.header.identity.IdentityCanonicalEvents.UserDeletedData;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.identity.role.RolePolicyStatementVO;
import org.zstack.header.identity.role.RolePolicyStatementVO_;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.RoleVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.CollectionDSL.list;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AccountBase extends AbstractAccount {
    private static final CLogger logger = Utils.getLogger(AccountBase.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private EventFacade evtf;

    private AccountVO self;

    public AccountBase(AccountVO self) {
        this.self = self;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handle(APIUpdateAccountMsg msg) {
        AccountVO account = dbf.findByUuid(msg.getUuid(), AccountVO.class);

        if (msg.getPassword() != null) {
            for(PasswordUpdateExtensionPoint ext : pluginRgty.getExtensionList(PasswordUpdateExtensionPoint.class)) {
                ext.preUpdatePassword(account.getUuid(), account.getPassword(), msg.getPassword());
            }
        }

        if (msg.getName() != null) {
            account.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            account.setDescription(msg.getDescription());
        }

        boolean passwordUpdated = false;
        String oldPassword = null;
        if (msg.getPassword() != null && !msg.getPassword().equals(account.getPassword())) {
            oldPassword = account.getPassword();
            account.setPassword(msg.getPassword());
            passwordUpdated = true;

        }
        account = dbf.updateAndRefresh(account);

        if (passwordUpdated) {
            for(PasswordUpdateExtensionPoint ext : pluginRgty.getExtensionList(PasswordUpdateExtensionPoint.class)) {
                ext.afterUpdatePassword(account.getUuid(), oldPassword);
            }
        }

        // execute tf extension point
        final AccountInventory inventory = AccountInventory.valueOf(account);
        CollectionUtils.safeForEach(pluginRgty.getExtensionList(BeforeUpdateAccountExtensionPoint.class),
                arg -> arg.beforeUpdateAccount(inventory));

        APIUpdateAccountEvent evt = new APIUpdateAccountEvent(msg.getId());
        evt.setInventory(AccountInventory.valueOf(account));
        bus.publish(evt);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof AccountDeletionMsg) {
            handle((AccountDeletionMsg) msg);
        } else if (msg instanceof DeleteAccountMsg) {
            handle((DeleteAccountMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(DeleteAccountMsg msg) {
        DeleteAccountReply reply = new DeleteAccountReply();
        deleteAccount(new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void deleteAccount(Completion completion) {
        final String issuer = AccountVO.class.getSimpleName();
        final List<AccountInventory> ctx = list(AccountInventory.valueOf(self));
        List<String> resourceUuids = Q.New(AccountResourceRefVO.class)
                                        .select(AccountResourceRefVO_.resourceUuid)
                                        .eq(AccountResourceRefVO_.ownerAccountUuid, self.getUuid())
                                        .listValues();
        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-account-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        dbf.remove(self);
                        acntMgr.adminAdoptAllOrphanedResource(resourceUuids, self.getUuid());

                        AccountDeletedData evtData = new AccountDeletedData();
                        evtData.setAccountUuid(self.getUuid());
                        evtData.setInventory(AccountInventory.valueOf(self));
                        evtf.fire(IdentityCanonicalEvents.ACCOUNT_DELETED_PATH, evtData);

                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(final APIDeleteAccountMsg msg) {
        final APIDeleteAccountEvent evt = new APIDeleteAccountEvent(msg.getId());

        deleteAccount(new Completion(msg) {
            @Override
            public void success() {
                // execute tf extension point
                final AccountInventory inventory = new AccountInventory();
                inventory.setUuid(msg.getUuid() );
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(BeforeDeleteAccountExtensionPoint.class),
                        arg -> arg.beforeDeleteAccount(inventory));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void deleteRelatedResources() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(QuotaVO.class)
                        .eq(QuotaVO_.identityType, AccountVO.class.getSimpleName())
                        .eq(QuotaVO_.identityUuid, self.getUuid())
                        .delete();

                sql(UserVO.class)
                        .eq(UserVO_.accountUuid, self.getUuid())
                        .delete();

                sql(UserGroupVO.class)
                        .eq(UserGroupVO_.accountUuid, self.getUuid())
                        .delete();

                sql(PolicyVO.class)
                        .eq(PolicyVO_.accountUuid, self.getUuid())
                        .delete();

                sql("delete from SharedResourceVO s where s.ownerAccountUuid = :uuid or s.receiverAccountUuid = :uuid")
                        .param("uuid", self.getUuid())
                        .execute();

                List<String> resourceUuids = q(AccountResourceRefVO.class)
                        .select(AccountResourceRefVO_.resourceUuid)
                        .eq(AccountResourceRefVO_.accountUuid, self.getUuid())
                        .eq(AccountResourceRefVO_.resourceType, RoleVO.class.getSimpleName())
                        .listValues();

                if (!resourceUuids.isEmpty()) {
                    sql(RolePolicyStatementVO.class)
                            .in(RolePolicyStatementVO_.roleUuid, resourceUuids)
                            .delete();

                    sql(RoleVO.class)
                            .in(RoleVO_.uuid, resourceUuids)
                            .delete();
                }
            }
        }.execute();
    }

    private void handle(AccountDeletionMsg msg) {
        AccountDeletionReply reply = new AccountDeletionReply();
        deleteRelatedResources();
        bus.reply(msg, reply);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateAccountMsg) {
            handle((APIUpdateAccountMsg) msg);
        } else if (msg instanceof APICreateUserMsg) {
            handle((APICreateUserMsg) msg);
        } else if (msg instanceof APICreatePolicyMsg) {
            handle((APICreatePolicyMsg) msg);
        } else if (msg instanceof APIAttachPolicyToUserMsg) {
            handle((APIAttachPolicyToUserMsg) msg);
        } else if (msg instanceof APICreateUserGroupMsg) {
            handle((APICreateUserGroupMsg) msg);
        } else if (msg instanceof APIAttachPolicyToUserGroupMsg) {
            handle((APIAttachPolicyToUserGroupMsg) msg);
        } else if (msg instanceof APIAddUserToGroupMsg) {
            handle((APIAddUserToGroupMsg) msg);
        } else if (msg instanceof APIDeleteUserGroupMsg) {
            handle((APIDeleteUserGroupMsg) msg);
        } else if (msg instanceof APIDeleteUserMsg) {
            handle((APIDeleteUserMsg) msg);
        } else if (msg instanceof APIDeletePolicyMsg) {
            handle((APIDeletePolicyMsg) msg);
        } else if (msg instanceof APIDetachPolicyFromUserMsg) {
            handle((APIDetachPolicyFromUserMsg) msg);
        } else if (msg instanceof APIDetachPolicyFromUserGroupMsg) {
            handle((APIDetachPolicyFromUserGroupMsg) msg);
        } else if (msg instanceof APIRemoveUserFromGroupMsg) {
            handle((APIRemoveUserFromGroupMsg) msg);
        } else if (msg instanceof APIUpdateUserMsg) {
            handle((APIUpdateUserMsg) msg);
        } else if (msg instanceof APIShareResourceMsg) {
            handle((APIShareResourceMsg) msg);
        } else if (msg instanceof APIRevokeResourceSharingMsg) {
            handle((APIRevokeResourceSharingMsg) msg);
        } else if (msg instanceof APIUpdateQuotaMsg) {
            handle((APIUpdateQuotaMsg) msg);
        } else if (msg instanceof APIDeleteAccountMsg) {
            handle((APIDeleteAccountMsg) msg);
        } else if (msg instanceof APIGetAccountQuotaUsageMsg) {
            handle((APIGetAccountQuotaUsageMsg) msg);
        } else if (msg instanceof APIAttachPoliciesToUserMsg) {
            handle((APIAttachPoliciesToUserMsg) msg);
        } else if (msg instanceof APIDetachPoliciesFromUserMsg) {
            handle((APIDetachPoliciesFromUserMsg) msg);
        } else if (msg instanceof APIUpdateUserGroupMsg) {
            handle((APIUpdateUserGroupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateUserGroupMsg msg) {
        UserGroupVO group = dbf.findByUuid(msg.getUuid(), UserGroupVO.class);

        if (!AccountConstant.INITIAL_SYSTEM_ADMIN_UUID.equals(msg.getAccountUuid()) &&
                !group.getAccountUuid().equals(msg.getAccountUuid())) {
            throw new OperationFailureException(argerr("the user group[uuid:%s] does not belong to the account[uuid:%s]", group.getUuid(), msg.getAccountUuid()));
        }

        boolean update = false;
        if (msg.getName() != null) {
            group.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            group.setDescription(msg.getDescription());
            update = true;
        }

        if (update) {
            group = dbf.updateAndRefresh(group);
        }

        APIUpdateUserGroupEvent evt = new APIUpdateUserGroupEvent(msg.getId());
        evt.setInventory(UserGroupInventory.valueOf(group));
        bus.publish(evt);
    }

    @Transactional
    private void handle(APIDetachPoliciesFromUserMsg msg) {
        String sql = "delete from UserPolicyRefVO ref where ref.policyUuid in (:puuids) and ref.userUuid = :userUuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("puuids", msg.getPolicyUuids());
        q.setParameter("userUuid", msg.getUserUuid());
        q.executeUpdate();

        APIDetachPoliciesFromUserEvent evt = new APIDetachPoliciesFromUserEvent(msg.getId());
        bus.publish(evt);
    }

    @Transactional
    private void handle(APIAttachPoliciesToUserMsg msg) {
        for (String puuid : msg.getPolicyUuids()) {
            try {
                UserPolicyRefVO refVO = new UserPolicyRefVO();
                refVO.setUserUuid(msg.getUserUuid());
                refVO.setPolicyUuid(puuid);
                dbf.getEntityManager().persist(refVO);
                dbf.getEntityManager().flush();
            } catch (Throwable t) {
                if (!ExceptionDSL.isCausedBy(t, ConstraintViolationException.class)) {
                    throw t;
                }

                // the policy is already attached
            }
        }

        APIAttachPoliciesToUserEvent evt = new APIAttachPoliciesToUserEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIGetAccountQuotaUsageMsg msg) {
        APIGetAccountQuotaUsageReply reply = new APIGetAccountQuotaUsageReply();

        List<Quota.QuotaUsage> usages = new ArrayList<>();
        for (QuotaDefinition q : acntMgr.getQuotasDefinitions().values()) {
            Long used = q.getQuotaUsage(msg.getAccountUuid());

            if (used == null) {
                continue;
            }

            Quota.QuotaUsage usage = new Quota.QuotaUsage();
            usage.setUsed(used);
            usage.setName(q.getName());
            usages.add(usage);
        }

        Map<String, Quota.QuotaUsage> umap = new HashMap<String, Quota.QuotaUsage>();
        for (Quota.QuotaUsage usage : usages) {
            umap.put(usage.getName(), usage);
        }

        SimpleQuery<QuotaVO> q = dbf.createQuery(QuotaVO.class);
        q.add(QuotaVO_.identityUuid, Op.EQ, msg.getAccountUuid());
        q.add(QuotaVO_.identityType, Op.EQ, AccountVO.class.getSimpleName());
        q.add(QuotaVO_.name, Op.IN, umap.keySet());
        List<QuotaVO> vos = q.list();
        Map<String, QuotaVO> vmap = new HashMap<String, QuotaVO>();
        for (QuotaVO vo : vos) {
            vmap.put(vo.getName(), vo);
        }

        for (Map.Entry<String, Quota.QuotaUsage> e : umap.entrySet()) {
            Quota.QuotaUsage u = e.getValue();
            QuotaVO vo = vmap.get(u.getName());
            u.setTotal(vo == null ? 0 : vo.getValue());
        }

        List<Quota.QuotaUsage> ret = new ArrayList<Quota.QuotaUsage>();
        ret.addAll(umap.values());
        reply.setUsages(ret);
        bus.reply(msg, reply);
    }

    private void handle(APIUpdateQuotaMsg msg) {
        QuotaVO quota = msg.getQuotaVO();
        quota.setValue(msg.getValue());
        quota = dbf.updateAndRefresh(quota);

        APIUpdateQuotaEvent evt = new APIUpdateQuotaEvent(msg.getId());
        evt.setInventory(QuotaInventory.valueOf(quota));
        bus.publish(evt);
    }

    @Transactional
    private void handle(APIRevokeResourceSharingMsg msg) {

        Map<String, String> addUuidType = getUuidTypeMapByResourceUuids(msg.getResourceUuids());
        List<String> additionUuids = new ArrayList<>();
        for (ResourceSharingExtensionPoint extp : pluginRgty.getExtensionList(
                ResourceSharingExtensionPoint.class)) {
            additionUuids.addAll(extp.beforeResourceSharingExtensionPoint(addUuidType));
        }
        if (!additionUuids.isEmpty()) {
            additionUuids.addAll(msg.getResourceUuids());
            msg.setResourceUuids(additionUuids);
        }

        Query q = null;
        if (msg.isAll()) {
            String sql = "delete from SharedResourceVO vo where vo.ownerAccountUuid = :auuid and vo.resourceUuid in (:resUuids)";
            q = dbf.getEntityManager().createQuery(sql);
            q.setParameter("auuid", self.getUuid());
            q.setParameter("resUuids", msg.getResourceUuids());
        }

        if (msg.isToPublic()) {
            String sql = "delete from SharedResourceVO vo where vo.toPublic = :public and vo.ownerAccountUuid = :auuid and vo.resourceUuid in (:resUuids)";
            q = dbf.getEntityManager().createQuery(sql);
            q.setParameter("public", msg.isToPublic());
            q.setParameter("auuid", self.getUuid());
            q.setParameter("resUuids", msg.getResourceUuids());
        }

        if (msg.getAccountUuids() != null && !msg.getAccountUuids().isEmpty()) {
            String sql = "delete from SharedResourceVO vo where vo.receiverAccountUuid in (:ruuids) and vo.ownerAccountUuid = :auuid and vo.resourceUuid in (:resUuids)";
            q = dbf.getEntityManager().createQuery(sql);
            q.setParameter("auuid", self.getUuid());
            q.setParameter("ruuids", msg.getAccountUuids());
            q.setParameter("resUuids", msg.getResourceUuids());
        }

        if (q != null) {
            q.executeUpdate();
        }

        Map<String, String> uuidType = getUuidTypeMapByResourceUuids(msg.getResourceUuids());
        for (String ruuid : msg.getResourceUuids()) {
            String resourceType = uuidType.get(ruuid);

            if (msg.getAccountUuids() != null) {
                for (String auuid : msg.getAccountUuids()) {
                    logger.debug(String.format("Revoke Shared resource[uuid:%s type:%s] of account[uuid:%s] from account[uuid:%s]", ruuid, resourceType, self.getUuid(), auuid));
                }
            } else {
                logger.debug(String.format("Revoke Shared resource[uuid:%s type:%s] of account[uuid:%s]", ruuid, resourceType, self.getUuid()));
            }

        }

        APIRevokeResourceSharingEvent evt = new APIRevokeResourceSharingEvent(msg.getId());
        bus.publish(evt);
    }

    private Map<String, String> getUuidTypeMapByResourceUuids(List<String> resourceUuids) {
        List<Tuple> ts = Q.New(AccountResourceRefVO.class)
                .select(AccountResourceRefVO_.resourceUuid, AccountResourceRefVO_.resourceType)
                .in(AccountResourceRefVO_.resourceUuid, resourceUuids)
                .listTuple();
        Map<String, String> uuidType = new HashMap<String, String>();
        for (Tuple t : ts) {
            String resUuid = t.get(0, String.class);
            String resType = t.get(1, String.class);
            uuidType.put(resUuid, resType);
        }

        return uuidType;
    }

    private void handle(APIShareResourceMsg msg) {
        Map<String, String> addUuidType = getUuidTypeMapByResourceUuids(msg.getResourceUuids());
        List<String> additionUuids = new ArrayList<>();
        for (ResourceSharingExtensionPoint extp : pluginRgty.getExtensionList(
                ResourceSharingExtensionPoint.class)) {
            additionUuids.addAll(extp.beforeResourceSharingExtensionPoint(addUuidType));
        }
        if (!additionUuids.isEmpty()) {
            additionUuids.addAll(msg.getResourceUuids());
            msg.setResourceUuids(additionUuids);
        }

        Map<String, String> uuidType = getUuidTypeMapByResourceUuids(msg.getResourceUuids());

        for (String ruuid : msg.getResourceUuids()) {
            if (!uuidType.containsKey(ruuid)) {
                throw new OperationFailureException(argerr("the account[uuid: %s] doesn't have a resource[uuid: %s]", self.getUuid(), ruuid));
            }
        }

        new SQLBatch(){
            @Override
            protected void scripts() {
                if (msg.isToPublic()) {
                    for (String ruuid : msg.getResourceUuids()) {
                        if(Q.New(SharedResourceVO.class)
                                .eq(SharedResourceVO_.ownerAccountUuid, msg.getAccountUuid())
                                .eq(SharedResourceVO_.resourceUuid, ruuid)
                                .eq(SharedResourceVO_.toPublic, msg.isToPublic())
                                .isExists()){
                            continue;
                        }
                        String resourceType = uuidType.get(ruuid);

                        SharedResourceVO svo = new SharedResourceVO();
                        svo.setOwnerAccountUuid(msg.getAccountUuid());
                        svo.setResourceType(resourceType);
                        svo.setResourceUuid(ruuid);
                        svo.setToPublic(true);
                        dbf.persist(svo);
                        logger.debug(String.format("Shared resource[uuid:%s type:%s] to public", ruuid, resourceType));
                    }
                } else {
                    for (String ruuid : msg.getResourceUuids()) {
                        String resourceType = uuidType.get(ruuid);
                        for (String auuid : msg.getAccountUuids()) {
                            if (! Q.New(SharedResourceVO.class)
                                    .eq(SharedResourceVO_.ownerAccountUuid, msg.getAccountUuid())
                                    .eq(SharedResourceVO_.resourceUuid, ruuid)
                                    .eq(SharedResourceVO_.receiverAccountUuid,auuid)
                                    .isExists()){
                                SharedResourceVO svo = new SharedResourceVO();
                                svo.setOwnerAccountUuid(msg.getAccountUuid());
                                svo.setResourceType(resourceType);
                                svo.setResourceUuid(ruuid);
                                svo.setReceiverAccountUuid(auuid);
                                dbf.persist(svo);
                                logger.debug(String.format("Shared resource[uuid:%s type:%s] to account[uuid:%s]", ruuid, resourceType, auuid));
                            }
                        }
                    }
                }
            }
        }.execute();

        for (ResourceSharingExtensionPoint extp : pluginRgty.getExtensionList(
                ResourceSharingExtensionPoint.class)) {
            extp.afterResourceSharingExtensionPoint(uuidType,msg.getAccountUuids(),msg.isToPublic());
        }

        APIShareResourceEvent evt = new APIShareResourceEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIUpdateUserMsg msg) {
        UserVO user = dbf.findByUuid(msg.getUuid(), UserVO.class);

        if (!AccountConstant.INITIAL_SYSTEM_ADMIN_UUID.equals(msg.getAccountUuid()) && !user.getAccountUuid().equals(msg.getAccountUuid())) {
            throw new OperationFailureException(argerr("the user[uuid:%s] does not belong to the" +
                    " account[uuid:%s]", user.getUuid(), msg.getAccountUuid()));
        }

        if (msg.getOldPassword() != null && !msg.getOldPassword().equals(user.getPassword())){
            throw new OperationFailureException(argerr("old password is not equal to the original password, cannot update the password of user[uuid:%s]", user.getUuid()));
        }

        boolean update = false;
        if (msg.getName() != null) {
            user.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            user.setDescription(msg.getDescription());
            update = true;
        }
        if (msg.getPassword() != null) {
            user.setPassword(msg.getPassword());
            update = true;
        }
        if (update) {
            user = dbf.updateAndRefresh(user);
        }

        APIUpdateUserEvent evt = new APIUpdateUserEvent(msg.getId());
        evt.setInventory(UserInventory.valueOf(user));
        bus.publish(evt);
    }

    private void handle(APIRemoveUserFromGroupMsg msg) {
        SimpleQuery<UserGroupUserRefVO> q = dbf.createQuery(UserGroupUserRefVO.class);
        q.add(UserGroupUserRefVO_.groupUuid, Op.EQ, msg.getGroupUuid());
        q.add(UserGroupUserRefVO_.userUuid, Op.EQ, msg.getUserUuid());
        UserGroupUserRefVO ref = q.find();
        if (ref != null) {
            dbf.remove(ref);
        }

        bus.publish(new APIRemoveUserFromGroupEvent(msg.getId()));
    }

    private void handle(APIDetachPolicyFromUserGroupMsg msg) {
        SimpleQuery<UserGroupPolicyRefVO> q = dbf.createQuery(UserGroupPolicyRefVO.class);
        q.add(UserGroupPolicyRefVO_.groupUuid, Op.EQ, msg.getGroupUuid());
        q.add(UserGroupPolicyRefVO_.policyUuid, Op.EQ, msg.getPolicyUuid());
        UserGroupPolicyRefVO ref = q.find();
        if (ref != null) {
            dbf.remove(ref);
        }

        bus.publish(new APIDetachPolicyFromUserGroupEvent(msg.getId()));
    }

    private void handle(APIDetachPolicyFromUserMsg msg) {
        SimpleQuery<UserPolicyRefVO> q = dbf.createQuery(UserPolicyRefVO.class);
        q.add(UserPolicyRefVO_.policyUuid, Op.EQ, msg.getPolicyUuid());
        q.add(UserPolicyRefVO_.userUuid, Op.EQ, msg.getUserUuid());
        UserPolicyRefVO ref = q.find();
        if (ref != null) {
            dbf.remove(ref);
        }

        bus.publish(new APIDetachPolicyFromUserEvent(msg.getId()));
    }

    private void handle(APIDeletePolicyMsg msg) {
        dbf.removeByPrimaryKey(msg.getUuid(), PolicyVO.class);
        APIDeletePolicyEvent evt = new APIDeletePolicyEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIDeleteUserMsg msg) {
        UserVO user = dbf.findByUuid(msg.getUuid(), UserVO.class);
        if (user != null) {
            UserInventory inv = UserInventory.valueOf(user);
            UserDeletedData d = new UserDeletedData();
            d.setInventory(inv);
            d.setUserUuid(inv.getUuid());
            evtf.fire(IdentityCanonicalEvents.USER_DELETED_PATH, d);

            dbf.remove(user);
        }

        APIDeleteUserEvent evt = new APIDeleteUserEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIDeleteUserGroupMsg msg) {
        dbf.removeByPrimaryKey(msg.getUuid(), UserGroupVO.class);
        APIDeleteUserGroupEvent evt = new APIDeleteUserGroupEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIAddUserToGroupMsg msg) {
        UserGroupUserRefVO ugvo = new UserGroupUserRefVO();
        ugvo.setGroupUuid(msg.getGroupUuid());
        ugvo.setUserUuid(msg.getUserUuid());
        dbf.persist(ugvo);
        APIAddUserToGroupEvent evt = new APIAddUserToGroupEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIAttachPolicyToUserGroupMsg msg) {
        UserGroupPolicyRefVO grvo = new UserGroupPolicyRefVO();
        grvo.setGroupUuid(msg.getGroupUuid());
        grvo.setPolicyUuid(msg.getPolicyUuid());

        try {
            dbf.persist(grvo);
        } catch (Throwable t) {
            if (!ExceptionDSL.isCausedBy(t, ConstraintViolationException.class)) {
                throw t;
            }

            // the policy is already attached
        }

        APIAttachPolicyToUserGroupEvent evt = new APIAttachPolicyToUserGroupEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APICreateUserGroupMsg msg) {
        UserGroupVO gvo = new UserGroupVO();
        if (msg.getResourceUuid() != null) {
            gvo.setUuid(msg.getResourceUuid());
        } else {
            gvo.setUuid(Platform.getUuid());
        }
        gvo.setAccountUuid(self.getUuid());
        gvo.setDescription(msg.getDescription());
        gvo.setName(msg.getName());

        UserGroupVO finalGvo = gvo;
        gvo = new SQLBatchWithReturn<UserGroupVO>() {
            @Override
            protected UserGroupVO scripts() {
                persist(finalGvo);
                reload(finalGvo);
                return finalGvo;
            }
        }.execute();

        UserGroupInventory inv = UserGroupInventory.valueOf(gvo);
        APICreateUserGroupEvent evt = new APICreateUserGroupEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    private void handle(APIAttachPolicyToUserMsg msg) {
        UserPolicyRefVO upvo = new UserPolicyRefVO();
        upvo.setPolicyUuid(msg.getPolicyUuid());
        upvo.setUserUuid(msg.getUserUuid());
        try {
            dbf.persist(upvo);
        } catch (Throwable t) {
            if (!ExceptionDSL.isCausedBy(t, ConstraintViolationException.class)) {
                throw t;
            }

            // the policy is already attached
        }

        APIAttachPolicyToUserEvent evt = new APIAttachPolicyToUserEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APICreatePolicyMsg msg) {
        PolicyVO pvo = new PolicyVO();
        if (msg.getResourceUuid() != null) {
            pvo.setUuid(msg.getResourceUuid());
        } else {
            pvo.setUuid(Platform.getUuid());
        }
        pvo.setAccountUuid(self.getUuid());
        pvo.setName(msg.getName());
        pvo.setData(JSONObjectUtil.toJsonString(msg.getStatements()));
        pvo.setType(PolicyType.Customized);

        PolicyVO finalPvo = pvo;
        pvo = new SQLBatchWithReturn<PolicyVO>() {
            @Override
            protected PolicyVO scripts() {
                persist(finalPvo);
                reload(finalPvo);
                return finalPvo;
            }
        }.execute();

        PolicyInventory pinv = PolicyInventory.valueOf(pvo);
        APICreatePolicyEvent evt = new APICreatePolicyEvent(msg.getId());
        evt.setInventory(pinv);
        bus.publish(evt);
    }

    private void handle(APICreateUserMsg msg) {
        APICreateUserEvent evt = new APICreateUserEvent(msg.getId());

        UserVO uvo = new SQLBatchWithReturn<UserVO>() {
            @Override
            protected UserVO scripts() {
                UserVO uvo = new UserVO();
                if (msg.getResourceUuid() != null) {
                    uvo.setUuid(msg.getResourceUuid());
                } else {
                    uvo.setUuid(Platform.getUuid());
                }
                uvo.setAccountUuid(self.getUuid());
                uvo.setName(msg.getName());
                uvo.setPassword(msg.getPassword());
                uvo.setDescription(msg.getDescription());
                persist(uvo);
                reload(uvo);

                PolicyVO p = Q.New(PolicyVO.class).eq(PolicyVO_.name, "DEFAULT-READ")
                        .eq(PolicyVO_.accountUuid, self.getUuid()).find();
                if (p != null) {
                    UserPolicyRefVO uref = new UserPolicyRefVO();
                    uref.setPolicyUuid(p.getUuid());
                    uref.setUserUuid(uvo.getUuid());
                    persist(uref);
                }

                p = Q.New(PolicyVO.class).eq(PolicyVO_.name, "USER-RESET-PASSWORD")
                        .eq(PolicyVO_.accountUuid, self.getUuid()).find();
                if (p != null) {
                    UserPolicyRefVO uref = new UserPolicyRefVO();
                    uref.setPolicyUuid(p.getUuid());
                    uref.setUserUuid(uvo.getUuid());
                    persist(uref);
                }

                return uvo;
            }
        }.execute();

        final UserInventory inv = UserInventory.valueOf(uvo);

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterCreateUserExtensionPoint.class), new ForEachFunction<AfterCreateUserExtensionPoint>() {
            @Override
            public void run(AfterCreateUserExtensionPoint arg) {
                arg.afterCreateUser(inv);
            }
        });

        evt.setInventory(inv);
        bus.publish(evt);
    }
}
