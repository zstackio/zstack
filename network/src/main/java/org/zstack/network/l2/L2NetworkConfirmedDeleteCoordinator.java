package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l2.L2DeleteConfirmExtensionPoint;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.NetworkDeletionContext;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.inerr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORK_L2_10023;

public class L2NetworkConfirmedDeleteCoordinator {
    private static final CLogger logger = Utils.getLogger(L2NetworkConfirmedDeleteCoordinator.class);

    @Autowired
    private PluginRegistry pluginRgty;

    public void prepare(CascadeAction action, List<L2NetworkInventory> inventories,
                        Completion completion) {
        List<ConfirmedDelete> begun = new ArrayList<>();
        new While<>(inventories == null ? Collections.<L2NetworkInventory>emptyList() : inventories)
                .each((inventory, inventoryCompletion) -> prepareInventory(action, inventory, begun,
                        inventoryCompletion))
                .run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errors) {
                        if (errors.getCauses().isEmpty()) {
                            completion.success();
                        } else {
                            failPrepared(action, begun, errors.getCauses().get(0), completion);
                        }
                    }
                });
    }

    public void cancel(CascadeAction action, List<L2NetworkInventory> inventories,
                       Completion completion) {
        List<ConfirmedDelete> prepared = new ArrayList<>();
        try {
            List<L2NetworkInventory> candidates = inventories == null
                    ? Collections.emptyList() : inventories;
            for (L2NetworkInventory inventory : candidates) {
                NetworkDeletionContext context = NetworkDeletionContexts.get(action, inventory.getUuid());
                if (context == null || !NetworkDeletionContexts.isPrepared(action, inventory.getUuid())) {
                    continue;
                }
                for (L2DeleteConfirmExtensionPoint extension : confirmedExtensions(inventory)) {
                    prepared.add(new ConfirmedDelete(inventory, extension, context));
                }
            }
        } catch (RuntimeException e) {
            completion.fail(toError(e));
            return;
        }
        cancelPrepared(action, prepared, completion);
    }

    public void complete(CascadeAction action, List<L2NetworkInventory> inventories,
                         Completion completion) {
        List<ConfirmedDelete> prepared = new ArrayList<>();
        try {
            List<L2NetworkInventory> candidates = inventories == null
                    ? Collections.emptyList() : inventories;
            for (L2NetworkInventory inventory : candidates) {
                NetworkDeletionContext context = NetworkDeletionContexts.get(action, inventory.getUuid());
                if (context == null) {
                    continue;
                }
                for (L2DeleteConfirmExtensionPoint extension : confirmedExtensions(inventory)) {
                    prepared.add(new ConfirmedDelete(inventory, extension, context));
                }
            }
        } catch (RuntimeException e) {
            completion.fail(toError(e));
            return;
        }
        complete(prepared, completion);
    }

    public List<L2DeleteConfirmExtensionPoint> confirmedExtensions(L2NetworkInventory inventory) {
        return pluginRgty.getExtensionList(L2DeleteConfirmExtensionPoint.class).stream()
                .filter(extension -> extension.supports(inventory))
                .collect(Collectors.toList());
    }

    public boolean isPrepared(CascadeAction action, List<L2NetworkInventory> inventories) {
        for (L2NetworkInventory inventory : inventories == null
                ? Collections.<L2NetworkInventory>emptyList() : inventories) {
            if (!confirmedExtensions(inventory).isEmpty()
                    && !NetworkDeletionContexts.isPrepared(action, inventory.getUuid())) {
                return false;
            }
        }
        return true;
    }

    private void prepareInventory(CascadeAction action, L2NetworkInventory inventory,
                                  List<ConfirmedDelete> begun, WhileCompletion completion) {
        List<L2DeleteConfirmExtensionPoint> extensions;
        try {
            extensions = confirmedExtensions(inventory);
        } catch (RuntimeException e) {
            completion.addError(toError(e));
            completion.allDone();
            return;
        }
        if (extensions.isEmpty()) {
            completion.done();
            return;
        }

        NetworkDeletionContext context = NetworkDeletionContexts.get(action, inventory.getUuid());
        if (context == null) {
            context = new NetworkDeletionContext(NetworkDeletionContext.Origin.WHOLE_L2_SEGMENT_DELETE,
                    Platform.getUuid(), inventory.getUuid(), action.getRootIssuer());
            context.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            NetworkDeletionContexts.put(action, context);
        }

        NetworkDeletionContext finalContext = context;
        if (NetworkDeletionContexts.isPrepared(action, inventory.getUuid())) {
            for (L2DeleteConfirmExtensionPoint extension : extensions) {
                begun.add(new ConfirmedDelete(inventory, extension, finalContext));
            }
            check(inventory, extensions, finalContext, completion);
            return;
        }
        begin(action, extensions, begun, inventory, finalContext, completion);
    }

    private void begin(CascadeAction action, List<L2DeleteConfirmExtensionPoint> extensions,
                       List<ConfirmedDelete> begun, L2NetworkInventory inventory,
                       NetworkDeletionContext context, WhileCompletion completion) {
        new While<>(extensions).each((extension, extensionCompletion) -> {
            try {
                extension.begin(inventory, context, new Completion(extensionCompletion) {
                    @Override
                    public void success() {
                        begun.add(new ConfirmedDelete(inventory, extension, context));
                        extensionCompletion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        extensionCompletion.addError(errorCode);
                        extensionCompletion.allDone();
                    }
                });
            } catch (RuntimeException e) {
                extensionCompletion.addError(toError(e));
                extensionCompletion.allDone();
            }
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                if (!errors.getCauses().isEmpty()) {
                    completion.addError(errors.getCauses().get(0));
                    completion.allDone();
                    return;
                }
                NetworkDeletionContexts.markPrepared(action, inventory.getUuid(), true);
                check(inventory, extensions, context, completion);
            }
        });
    }

    private void check(L2NetworkInventory inventory, List<L2DeleteConfirmExtensionPoint> extensions,
                       NetworkDeletionContext context, WhileCompletion completion) {
        new While<>(extensions).each((extension, extensionCompletion) -> {
            try {
                extension.check(inventory, context, new Completion(extensionCompletion) {
                    @Override
                    public void success() {
                        extensionCompletion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        extensionCompletion.addError(errorCode);
                        extensionCompletion.allDone();
                    }
                });
            } catch (RuntimeException e) {
                extensionCompletion.addError(toError(e));
                extensionCompletion.allDone();
            }
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                if (errors.getCauses().isEmpty()) {
                    completion.done();
                } else {
                    completion.addError(errors.getCauses().get(0));
                    completion.allDone();
                }
            }
        });
    }

    private void failPrepared(CascadeAction action, List<ConfirmedDelete> begun, ErrorCode error,
                              Completion completion) {
        cancelPrepared(action, begun, new Completion(completion) {
            @Override
            public void success() {
                completion.fail(error);
            }

            @Override
            public void fail(ErrorCode cancelError) {
                completion.fail(error);
            }
        });
    }

    private void cancelPrepared(CascadeAction action, List<ConfirmedDelete> prepared, Completion completion) {
        List<ConfirmedDelete> reverse = new ArrayList<>(prepared);
        Collections.reverse(reverse);
        new While<>(reverse).each((delete, cancelCompletion) -> invokeCancel(delete,
                new Completion(cancelCompletion) {
                    @Override
                    public void success() {
                        NetworkDeletionContexts.markPrepared(action, delete.inventory.getUuid(), false);
                        cancelCompletion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to cancel confirmed L2Network deletion[uuid:%s]: %s",
                                delete.inventory.getUuid(), errorCode));
                        NetworkDeletionContexts.markPrepared(action, delete.inventory.getUuid(), false);
                        cancelCompletion.done();
                    }
                })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                completion.success();
            }
        });
    }

    private void complete(List<ConfirmedDelete> prepared, Completion completion) {
        new While<>(prepared).each((delete, deleteCompletion) -> {
            try {
                delete.extension.delete(delete.inventory, delete.context, new Completion(deleteCompletion) {
                    @Override
                    public void success() {
                        deleteCompletion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        deleteCompletion.addError(errorCode);
                        deleteCompletion.allDone();
                    }
                });
            } catch (RuntimeException e) {
                deleteCompletion.addError(toError(e));
                deleteCompletion.allDone();
            }
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                if (errors.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errors.getCauses().get(0));
                }
            }
        });
    }

    private void invokeCancel(ConfirmedDelete delete, Completion completion) {
        try {
            delete.extension.cancel(delete.inventory, delete.context, completion);
        } catch (RuntimeException e) {
            completion.fail(toError(e));
        }
    }

    private ErrorCode toError(RuntimeException exception) {
        if (exception instanceof OperationFailureException) {
            return ((OperationFailureException) exception).getErrorCode();
        }
        return inerr(ORG_ZSTACK_NETWORK_L2_10023,
                "failed to coordinate confirmed L2Network deletion: %s", exception.getMessage());
    }

    private static class ConfirmedDelete {
        private final L2NetworkInventory inventory;
        private final L2DeleteConfirmExtensionPoint extension;
        private final NetworkDeletionContext context;

        private ConfirmedDelete(L2NetworkInventory inventory,
                                L2DeleteConfirmExtensionPoint extension,
                                NetworkDeletionContext context) {
            this.inventory = inventory;
            this.extension = extension;
            this.context = context;
        }
    }
}
