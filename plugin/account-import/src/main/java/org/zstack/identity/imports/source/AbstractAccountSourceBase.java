package org.zstack.identity.imports.source;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO;
import org.zstack.identity.imports.header.CreateAccountSpec;
import org.zstack.identity.imports.header.UnbindThirdPartyAccountSpecItem;
import org.zstack.identity.imports.header.ImportAccountResult;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.header.ImportAccountSpec;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;
import org.zstack.identity.imports.header.SyncTaskSpec;
import org.zstack.identity.imports.header.UnbindThirdPartyAccountResult;
import org.zstack.identity.imports.header.UnbindThirdPartyAccountsSpec;
import org.zstack.identity.imports.message.BindThirdPartyAccountMsg;
import org.zstack.identity.imports.message.ImportThirdPartyAccountMsg;
import org.zstack.identity.imports.message.ImportThirdPartyAccountReply;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO_;
import org.zstack.identity.imports.message.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.List;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.CreateAccountMsg;
import org.zstack.header.identity.CreateAccountReply;
import org.zstack.header.identity.DeleteAccountMsg;
import org.zstack.header.message.MessageReply;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.identity.imports.AccountImportsManager.accountSourceQueueSyncSignature;
import static org.zstack.identity.imports.AccountImportsManager.accountSourceSyncTaskSignature;
import static org.zstack.utils.CollectionUtils.*;

/**
 * Created by Wenhao.Zhang on 2024/05/31
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractAccountSourceBase {
    private static final CLogger logger = Utils.getLogger(AbstractAccountSourceBase.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade databaseFacade;
    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired
    private ThreadFacade threadFacade;

    protected AbstractAccountSourceBase(ThirdPartyAccountSourceVO self) {
        this.self = Objects.requireNonNull(self);
    }

    protected ThirdPartyAccountSourceVO self;
    public abstract String type();

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof ImportThirdPartyAccountMsg) {
            handle(((ImportThirdPartyAccountMsg) msg));
        } else if (msg instanceof BindThirdPartyAccountMsg) {
            handle(((BindThirdPartyAccountMsg) msg));
        } else if (msg instanceof UnbindThirdPartyAccountMsg) {
            handle(((UnbindThirdPartyAccountMsg) msg));
        } else if (msg instanceof SyncThirdPartyAccountMsg) {
            handle(((SyncThirdPartyAccountMsg) msg));
        } else if (msg instanceof DestroyThirdPartyAccountSourceMsg) {
            handle(((DestroyThirdPartyAccountSourceMsg) msg));
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    public void handle(ImportThirdPartyAccountMsg message) {
        ImportThirdPartyAccountReply reply = new ImportThirdPartyAccountReply();
        final String sourceUuid = message.getSourceUuid();

        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                importAccounts(message.getSpec(), new ReturnValueCompletion<List<ImportAccountResult>>(chain) {
                    @Override
                    public void success(List<ImportAccountResult> results) {
                        chain.next();
                        reply.setResults(results);
                        bus.reply(message, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(message, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return accountSourceQueueSyncSignature(sourceUuid);
            }

            @Override
            public String getName() {
                return "import-accounts-from-source-" + sourceUuid;
            }
        });
    }

    @SuppressWarnings("rawtypes")
    public final void importAccounts(ImportAccountSpec spec, ReturnValueCompletion<List<ImportAccountResult>> completion) {
        List<ImportThirdPartyAccountContext> contexts = new ArrayList<>(spec.getAccountList().size());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("chain-with-importing-accounts-from-source-%s", spec.getSourceUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "validate-spec";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                buildContextsFromSpec();
                validateIfAccountNotPresent();
                validateIfAccountBindOtherSource();
                trigger.next();
            }

            private void buildContextsFromSpec() {
                final Set<String> accountUuidSet =
                        transformToSetAndRemoveNull(spec.getAccountList(), CreateAccountSpec::getAccountUuid);

                final Map<String, AccountVO> uuidExistingAccountMap = new HashMap<>();
                if (!accountUuidSet.isEmpty()) {
                    int totalSize = accountUuidSet.size();

                    SQL.New("select account from AccountVO account where uuid in (:uuidSet)", AccountVO.class)
                            .param("uuidSet", accountUuidSet)
                            .limit(50)
                            .paginate(totalSize, (List<AccountVO> accounts) -> {
                                for (AccountVO account : accounts) {
                                    uuidExistingAccountMap.put(account.getUuid(), account);
                                }
                            });
                }

                for (CreateAccountSpec accountSpec : spec.getAccountList()) {
                    final ImportThirdPartyAccountContext context = new ImportThirdPartyAccountContext();
                    context.spec = accountSpec;

                    AccountVO account = uuidExistingAccountMap.get(accountSpec.getAccountUuid());
                    if (account != null) {
                        context.account = AccountInventory.valueOf(account);
                        context.bindToExistingAccount = true;
                    }
                    contexts.add(context);
                }
            }

            private void validateIfAccountNotPresent() {
                for (ImportThirdPartyAccountContext context : contexts) {
                    final CreateAccountSpec accountSpec = context.spec;

                    if (accountSpec.isCreateIfNotExist()) {
                        continue;
                    }

                    if (accountSpec.getAccountUuid() == null) {
                        context.errorForValidation = operr("invalid account spec: accountUuid is null");
                        continue;
                    }

                    if (context.account == null) {
                        context.errorForValidation = operr("invalid account spec: failed to find account[uuid=%s]",
                                accountSpec.getAccountUuid());
                    }
                }
            }

            private void validateIfAccountBindOtherSource() {
                final List<ImportThirdPartyAccountContext> filteredContexts = contexts.stream()
                        .filter(context -> !context.hasError())
                        .filter(context -> context.bindToExistingAccount)
                        .collect(Collectors.toList());
                if (filteredContexts.isEmpty()) {
                    return;
                }

                final List<String> accountUuidList = transform(filteredContexts, context -> context.account.getUuid());
                final List<Tuple> accountsAlreadyBinding = Q.New(AccountThirdPartyAccountSourceRefVO.class)
                        .in(AccountThirdPartyAccountSourceRefVO_.accountUuid, accountUuidList)
                        .select(AccountThirdPartyAccountSourceRefVO_.accountUuid, AccountThirdPartyAccountSourceRefVO_.accountSourceUuid)
                        .listTuple();
                for (Tuple tuple : accountsAlreadyBinding) {
                    String accountUuid = tuple.get(0, String.class);
                    String sourceUuid = tuple.get(1, String.class);
                    ImportThirdPartyAccountContext context =
                            findOneOrNull(contexts, c -> Objects.equals(c.spec.getAccountUuid(), accountUuid));
                    if (Objects.equals(sourceUuid, self.getUuid())) {
                        logger.debug(String.format(
                                "account[uuid=%s] has already binding third party source[uuid=%s], skip binding execution",
                                accountUuid, sourceUuid));
                        context.skipBinding = true;
                    } else {
                        context.errorForValidation =
                                operr("account[uuid=%s] has already binding other third party source[uuid=%s]",
                                accountUuid, sourceUuid);
                    }
                }
            }
        }).then(new Flow() {
            String __name__ = "generate-accounts";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(spec.getAccountList()).each((accountSpec, whileCompletion) -> {
                    final ImportThirdPartyAccountContext context = findOneOrNull(contexts, c -> c.spec == accountSpec);
                    if (context.hasError() || context.bindToExistingAccount) {
                        whileCompletion.done();
                        return;
                    }

                    context.readyToCreateAccount = true;
                    String accountUuid = Platform.getUuid();

                    CreateAccountMsg message = new CreateAccountMsg();
                    message.setUuid(accountUuid);
                    message.setName(accountSpec.getUsername());
                    message.setPassword(accountSpec.getPassword());
                    message.setType(accountSpec.getAccountType().toString());
                    bus.makeLocalServiceId(message, AccountConstant.SERVICE_ID);
                    bus.send(message, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                context.account = ((CreateAccountReply) reply).getInventory();
                            } else {
                                context.errorForCreatingAccount = reply.getError();
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            logger.warn("failed to generate accounts when imports accounts but still continue: "
                                    + errorCodeList.getCauses().get(0).getDetails());
                        }
                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                new While<>(contexts).each((context, whileCompletion) -> {
                    if (context.account != null || !context.readyToCreateAccount) {
                        whileCompletion.done();
                        return;
                    }

                    DeleteAccountMsg message = new DeleteAccountMsg();
                    message.setUuid(context.account.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(message, AccountConstant.SERVICE_ID, context.account.getUuid());
                    bus.send(message, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("destroy account[uuid=%s, name=%s] failed",
                                        context.account.getUuid(), context.account.getName()));
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            logger.warn("failed to rollback imports accounts: " + errorCodeList.getCauses().get(0).getDetails());
                        }
                        trigger.rollback();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "bind-accounts-with-import-source";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<ImportThirdPartyAccountContext> filteredContexts =
                        filter(contexts, context -> !context.hasError() && !context.skipBinding);

                if (filteredContexts.isEmpty()) {
                    trigger.next();
                    return;
                }

                for (ImportThirdPartyAccountContext context : filteredContexts) {
                    AccountThirdPartyAccountSourceRefVO ref = new AccountThirdPartyAccountSourceRefVO();
                    ref.setCredentials(context.spec.getCredentials());
                    ref.setAccountSourceUuid(spec.getSourceUuid());
                    ref.setAccountUuid(context.account.getUuid());
                    context.ref = ref;
                }

                databaseFacade.persistCollection(transform(filteredContexts, result -> result.ref));
                trigger.next();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(transform(contexts, ImportThirdPartyAccountContext::makeResult));
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(operr(errCode, "failed to import account from source[uuid=%s, type=%s]",
                        spec.getSourceUuid(), spec.getSourceType()));
            }
        }).start();
    }

    private void handle(BindThirdPartyAccountMsg message) {
        BindThirdPartyAccountReply reply = new BindThirdPartyAccountReply();
        final String sourceUuid = message.getSourceUuid();

        ImportAccountSpec batch = new ImportAccountSpec();
        batch.setSourceUuid(sourceUuid);
        batch.setSourceType(this.type());

        CreateAccountSpec spec = new CreateAccountSpec();
        spec.setAccountUuid(message.getAccountUuid());
        spec.setCreateIfNotExist(false);
        spec.setCredentials(Objects.requireNonNull(message.getCredentials()));
        batch.getAccountList().add(spec);

        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                importAccounts(batch, new ReturnValueCompletion<List<ImportAccountResult>>(chain) {
                    @Override
                    public void success(List<ImportAccountResult> results) {
                        chain.next();
                        reply.setResults(results);
                        bus.reply(message, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(message, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return accountSourceQueueSyncSignature(sourceUuid);
            }

            @Override
            public String getName() {
                return "bind-account-from-source-" + sourceUuid;
            }
        });
    }

    private void handle(UnbindThirdPartyAccountMsg message) {
        UnbindThirdPartyAccountReply reply = new UnbindThirdPartyAccountReply();
        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                unbindingAccount(message.getSpec(), new ReturnValueCompletion<List<UnbindThirdPartyAccountResult>>(chain) {
                    @Override
                    public void success(List<UnbindThirdPartyAccountResult> results) {
                        chain.next();
                        reply.setResults(results);
                        bus.reply(message, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(message, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return accountSourceQueueSyncSignature(self.getUuid());
            }

            @Override
            public String getName() {
                return "unbinding-account-from-source-" + self.getUuid();
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private void unbindingAccount(UnbindThirdPartyAccountsSpec spec, ReturnValueCompletion<List<UnbindThirdPartyAccountResult>> completion) {
        if (spec.getItems().isEmpty()) {
            completion.success(new ArrayList<>());
            return;
        }

        List<UnbindThirdPartyAccountsContext> contexts = new ArrayList<>();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("chain-with-unbinding-accounts-from-source-%s", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "unbinding-account";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                final List<String> accountUuidList = transform(spec.getItems(),
                        UnbindThirdPartyAccountSpecItem::getAccountUuid);

                SQL.New(AccountThirdPartyAccountSourceRefVO.class)
                        .in(AccountThirdPartyAccountSourceRefVO_.accountUuid, accountUuidList)
                        .eq(AccountThirdPartyAccountSourceRefVO_.accountSourceUuid, self.getUuid())
                        .delete();

                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-account-if-needed";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                for (UnbindThirdPartyAccountSpecItem item : spec.getItems()) {
                    UnbindThirdPartyAccountsContext context = new UnbindThirdPartyAccountsContext();
                    context.specItem = item;
                    context.sourceUuid = self.getUuid();
                    contexts.add(context);
                }

                new While<>(contexts).each((context, whileCompletion) -> {
                    if (!context.specItem.needDeleteAccount()) {
                        whileCompletion.done();
                        return;
                    }

                    final String accountUuid = context.specItem.getAccountUuid();

                    DeleteAccountMsg message = new DeleteAccountMsg();
                    message.setUuid(accountUuid);
                    bus.makeTargetServiceIdByResourceUuid(message, AccountConstant.SERVICE_ID, accountUuid);
                    bus.send(message, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("destroy account[uuid=%s] failed, still continue", accountUuid));
                                context.errorForDeleteAccount = reply.getError();
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            logger.warn("failed to delete accounts but still continue: "
                                    + errorCodeList.getCauses().get(0).getDetails());
                        }
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(transform(contexts, UnbindThirdPartyAccountsContext::makeResult));
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(operr(errCode, "failed to unbinding accounts from source[uuid=%s, type=%s]",
                        self.getUuid(), self.getType()));
            }
        }).start();
    }

    private void handle(SyncThirdPartyAccountMsg message) {
        SyncThirdPartyAccountReply reply = new SyncThirdPartyAccountReply();

        SyncCreatedAccountStrategy createStrategy = message.getCreateAccountStrategy() == null ?
                self.getCreateAccountStrategy() : message.getCreateAccountStrategy();
        SyncDeletedAccountStrategy deleteStrategy = message.getDeleteAccountStrategy() == null ?
                self.getDeleteAccountStrategy() : message.getDeleteAccountStrategy();

        SyncTaskSpec spec = new SyncTaskSpec();
        spec.setSourceUuid(self.getUuid());
        spec.setSourceType(type());
        spec.setCreateAccountStrategy(createStrategy);
        spec.setDeleteAccountStrategy(deleteStrategy);

        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                syncAccountsFromSource(spec, new Completion(chain) {
                    @Override
                    public void success() {
                        chain.next();
                        bus.reply(message, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(message, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return accountSourceSyncTaskSignature();
            }

            @Override
            public String getName() {
                return "sync-accounts-from-source-" + self.getUuid();
            }
        });
    }

    protected abstract void syncAccountsFromSource(SyncTaskSpec spec, Completion completion);

    private void handle(DestroyThirdPartyAccountSourceMsg message) {
        DestroyThirdPartyAccountSourceReply reply = new DestroyThirdPartyAccountSourceReply();
        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                destroySource(new Completion(chain) {
                    @Override
                    public void success() {
                        chain.next();
                        bus.reply(message, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(message, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return accountSourceQueueSyncSignature(self.getUuid());
            }

            @Override
            public String getName() {
                return "destroy-source-" + self.getUuid();
            }
        });
    }

    protected abstract void destroySource(Completion completion);
}
