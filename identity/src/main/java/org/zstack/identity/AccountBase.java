package org.zstack.identity;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.IdentityCanonicalEvents.AccountDeletedData;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.identity.role.RolePolicyStatementVO;
import org.zstack.header.identity.role.RolePolicyStatementVO_;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.RoleVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.Pair;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AccountBase extends AbstractAccount {
    private static final CLogger logger = Utils.getLogger(AccountBase.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
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
        APIUpdateAccountEvent evt = new APIUpdateAccountEvent(msg.getId());

        UpdateAccountMsg innerMsg = new UpdateAccountMsg();
        innerMsg.setUuid(msg.getUuid());
        innerMsg.setName(msg.getName());
        innerMsg.setPassword(msg.getPassword());
        innerMsg.setDescription(msg.getDescription());
        if (msg.getState() != null) {
            innerMsg.setState(AccountState.valueOf(msg.getState()));
        }

        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountConstant.SERVICE_ID, msg.getUuid());
        bus.send(innerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    evt.setInventory(((UpdateAccountReply) reply).getInventory());
                } else {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(UpdateAccountMsg msg) {
        UpdateAccountReply reply = new UpdateAccountReply();
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
        if (msg.getState() != null) {
            account.setState(msg.getState());
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
        CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterUpdateAccountExtensionPoint.class),
                arg -> arg.afterUpdateAccount(inventory));

        reply.setInventory(AccountInventory.valueOf(account));
        bus.reply(msg, reply);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof AccountDeletionMsg) {
            handle((AccountDeletionMsg) msg);
        } else if (msg instanceof UpdateAccountMsg) {
            handle((UpdateAccountMsg) msg);
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
                                        .eq(AccountResourceRefVO_.accountUuid, self.getUuid())
                                        .eq(AccountResourceRefVO_.type, AccessLevel.Own)
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
                        .eq(AccountResourceRefVO_.type, AccessLevel.Own)
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
        } else if (msg instanceof APICreatePolicyMsg) {
            handle((APICreatePolicyMsg) msg);
        } else if (msg instanceof APIDeletePolicyMsg) {
            handle((APIDeletePolicyMsg) msg);
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
        } else {
            bus.dealWithUnknownMessage(msg);
        }
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
        Map<String, QuotaVO> vmap = new HashMap<>();
        for (QuotaVO vo : vos) {
            vmap.put(vo.getName(), vo);
        }

        for (Map.Entry<String, Quota.QuotaUsage> e : umap.entrySet()) {
            Quota.QuotaUsage u = e.getValue();
            QuotaVO vo = vmap.get(u.getName());
            u.setTotal(vo == null ? 0 : vo.getValue());
        }

        reply.setUsages(new ArrayList<>(umap.values()));
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
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .listTuple();
        Map<String, String> uuidType = new HashMap<>();
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

        final ShareResourcePermission permission = msg.getPermission() == null ?
                ShareResourcePermission.READ : ShareResourcePermission.valueOf(msg.getPermission());

        new SQLBatch(){
            @Override
            protected void scripts() {
                if (msg.isToPublic()) {
                    shareToPublic(msg.getResourceUuids());
                } else {
                    shareToAccount(msg.getResourceUuids(), msg.getAccountUuids());
                }
            }

            void shareToPublic(List<String> resourceUuidList) {
                List<String> existsUuidList = q(SharedResourceVO.class)
                        .eq(SharedResourceVO_.ownerAccountUuid, msg.getAccountUuid())
                        .in(SharedResourceVO_.resourceUuid, resourceUuidList)
                        .eq(SharedResourceVO_.toPublic, true)
                        .select(SharedResourceVO_.resourceUuid)
                        .listValues();
                Set<String> existsUuidSet = new HashSet<>(existsUuidList);

                List<SharedResourceVO> sharedList = resourceUuidList.stream()
                        .filter(uuid -> !existsUuidSet.contains(uuid))
                        .map(uuid -> {
                            SharedResourceVO shared = new SharedResourceVO();
                            shared.setOwnerAccountUuid(msg.getAccountUuid());
                            shared.setResourceType(uuidType.get(uuid));
                            shared.setResourceUuid(uuid);
                            shared.setToPublic(true);
                            shared.setPermission(permission.code);
                            return shared;
                        })
                        .collect(Collectors.toList());
                if (sharedList.isEmpty()) {
                    return;
                }

                dbf.persistCollection(sharedList);
                String texts = StringUtils.join(transform(sharedList,
                        shared -> String.format("\tuuid:%s type:%s", shared.getResourceUuid(), shared.getResourceType())), "\n");
                logger.debug(String.format("Shared below resources to public: \n%s", texts));
            }

            void shareToAccount(List<String> resourceUuidList, List<String> receiverUuidList) {
                List<Tuple> tuples = q(SharedResourceVO.class)
                        .eq(SharedResourceVO_.ownerAccountUuid, msg.getAccountUuid())
                        .in(SharedResourceVO_.resourceUuid, resourceUuidList)
                        .in(SharedResourceVO_.receiverAccountUuid, receiverUuidList)
                        .select(SharedResourceVO_.resourceUuid, SharedResourceVO_.receiverAccountUuid)
                        .listTuple();

                Set<Pair<String, String>> PairSet = transformToSet(tuples,
                        tuple -> new Pair<>(tuple.get(0, String.class), tuple.get(1, String.class)));

                List<SharedResourceVO> sharedList = new ArrayList<>();
                for (String resourceUuid : resourceUuidList) {
                    String resourceType = uuidType.get(resourceUuid);
                    for (String receiverUuid : receiverUuidList) {
                        if (PairSet.contains(new Pair<>(resourceUuid, receiverUuid))) {
                            continue;
                        }

                        SharedResourceVO shared = new SharedResourceVO();
                        shared.setOwnerAccountUuid(msg.getAccountUuid());
                        shared.setResourceType(resourceType);
                        shared.setResourceUuid(resourceUuid);
                        shared.setReceiverAccountUuid(receiverUuid);
                        shared.setPermission(permission.code);
                        sharedList.add(shared);
                    }
                }

                if (sharedList.isEmpty()) {
                    return;
                }

                dbf.persistCollection(sharedList);
                String texts = StringUtils.join(transform(sharedList,
                        shared -> String.format("\tuuid:%s type:%s", shared.getResourceUuid(), shared.getResourceType())), "\n");
                logger.debug(String.format("Shared below resources to account[uuid:%s]: \n%s", receiverUuidList, texts));
            }
        }.execute();

        for (ResourceSharingExtensionPoint extp : pluginRgty.getExtensionList(
                ResourceSharingExtensionPoint.class)) {
            extp.afterResourceSharingExtensionPoint(uuidType,msg.getAccountUuids(),msg.isToPublic());
        }

        APIShareResourceEvent evt = new APIShareResourceEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIDeletePolicyMsg msg) {
        dbf.removeByPrimaryKey(msg.getUuid(), PolicyVO.class);
        APIDeletePolicyEvent evt = new APIDeletePolicyEvent(msg.getId());
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
}
