package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
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
        prepare(action, inventories == null ? Collections.emptyList() : inventories,
                0, new ArrayList<>(), completion);
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
        cancel(action, prepared, prepared.size() - 1, completion);
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
        complete(prepared, 0, completion);
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

    private void prepare(CascadeAction action, List<L2NetworkInventory> inventories, int inventoryIndex,
                         List<ConfirmedDelete> begun, Completion completion) {
        if (inventoryIndex == inventories.size()) {
            completion.success();
            return;
        }

        L2NetworkInventory inventory = inventories.get(inventoryIndex);
        List<L2DeleteConfirmExtensionPoint> extensions;
        try {
            extensions = confirmedExtensions(inventory);
        } catch (RuntimeException e) {
            failPrepared(action, begun, toError(e), completion);
            return;
        }
        if (extensions.isEmpty()) {
            prepare(action, inventories, inventoryIndex + 1, begun, completion);
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
            check(action, inventories, inventoryIndex, extensions, 0, begun, inventory,
                    finalContext, completion);
            return;
        }
        begin(action, inventories, inventoryIndex, extensions, 0, begun, inventory,
                finalContext, completion);
    }

    private void begin(CascadeAction action, List<L2NetworkInventory> inventories, int inventoryIndex,
                       List<L2DeleteConfirmExtensionPoint> extensions, int extensionIndex,
                       List<ConfirmedDelete> begun, L2NetworkInventory inventory,
                       NetworkDeletionContext context, Completion completion) {
        if (extensionIndex == extensions.size()) {
            NetworkDeletionContexts.markPrepared(action, inventory.getUuid(), true);
            check(action, inventories, inventoryIndex, extensions, 0, begun, inventory,
                    context, completion);
            return;
        }

        L2DeleteConfirmExtensionPoint extension = extensions.get(extensionIndex);
        invoke(() -> extension.begin(inventory, context, new Completion(completion) {
            @Override
            public void success() {
                begun.add(new ConfirmedDelete(inventory, extension, context));
                begin(action, inventories, inventoryIndex, extensions, extensionIndex + 1,
                        begun, inventory, context, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                failPrepared(action, begun, errorCode, completion);
            }
        }), begun, action, completion);
    }

    private void check(CascadeAction action, List<L2NetworkInventory> inventories, int inventoryIndex,
                       List<L2DeleteConfirmExtensionPoint> extensions, int extensionIndex,
                       List<ConfirmedDelete> begun, L2NetworkInventory inventory,
                       NetworkDeletionContext context, Completion completion) {
        if (extensionIndex == extensions.size()) {
            prepare(action, inventories, inventoryIndex + 1, begun, completion);
            return;
        }

        L2DeleteConfirmExtensionPoint extension = extensions.get(extensionIndex);
        invoke(() -> extension.check(inventory, context, new Completion(completion) {
            @Override
            public void success() {
                check(action, inventories, inventoryIndex, extensions, extensionIndex + 1,
                        begun, inventory, context, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                failPrepared(action, begun, errorCode, completion);
            }
        }), begun, action, completion);
    }

    private void failPrepared(CascadeAction action, List<ConfirmedDelete> begun, ErrorCode error,
                              Completion completion) {
        cancel(action, begun, begun.size() - 1, new Completion(completion) {
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

    private void cancel(CascadeAction action, List<ConfirmedDelete> prepared, int index,
                        Completion completion) {
        if (index < 0) {
            completion.success();
            return;
        }

        ConfirmedDelete delete = prepared.get(index);
        invokeCancel(delete, new Completion(completion) {
            @Override
            public void success() {
                NetworkDeletionContexts.markPrepared(action, delete.inventory.getUuid(), false);
                cancel(action, prepared, index - 1, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to cancel confirmed L2Network deletion[uuid:%s]: %s",
                        delete.inventory.getUuid(), errorCode));
                NetworkDeletionContexts.markPrepared(action, delete.inventory.getUuid(), false);
                cancel(action, prepared, index - 1, completion);
            }
        });
    }

    private void complete(List<ConfirmedDelete> prepared, int index, Completion completion) {
        if (index == prepared.size()) {
            completion.success();
            return;
        }
        ConfirmedDelete delete = prepared.get(index);
        try {
            delete.extension.delete(delete.inventory, delete.context, new Completion(completion) {
                @Override
                public void success() {
                    complete(prepared, index + 1, completion);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
        } catch (RuntimeException e) {
            completion.fail(toError(e));
        }
    }

    private void invoke(Runnable runnable, List<ConfirmedDelete> begun, CascadeAction action,
                        Completion completion) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            failPrepared(action, begun, toError(e), completion);
        }
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
