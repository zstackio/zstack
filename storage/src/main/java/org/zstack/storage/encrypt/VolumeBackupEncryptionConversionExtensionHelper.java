package org.zstack.storage.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.volume.VolumeBackupEncryptionConversionContext;
import org.zstack.header.volume.VolumeBackupEncryptionConversionExtensionPoint;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.zstack.core.Platform.operr;

public class VolumeBackupEncryptionConversionExtensionHelper {
    private static final CLogger logger = Utils.getLogger(VolumeBackupEncryptionConversionExtensionHelper.class);
    @Autowired
    private PluginRegistry pluginRgty;

    public static class Context {
        private final VolumeBackupEncryptionConversionExtensionPoint extension;
        private final VolumeBackupEncryptionConversionContext context;

        public Context(VolumeBackupEncryptionConversionExtensionPoint extension,
                       VolumeBackupEncryptionConversionContext context) {
            this.extension = extension;
            this.context = context;
        }
    }

    public void prepare(VolumeInventory volume, boolean targetEncrypted, String hostUuid,
                        ReturnValueCompletion<List<Context>> completion) {
        List<VolumeBackupEncryptionConversionExtensionPoint> extensions =
                pluginRgty.getExtensionList(VolumeBackupEncryptionConversionExtensionPoint.class);
        List<Context> contexts = Collections.synchronizedList(new ArrayList<>());
        new While<>(extensions).each((ext, whileCompletion) ->
                prepareExtension(ext, volume, targetEncrypted, hostUuid, contexts, whileCompletion)
        ).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success(new ArrayList<>(contexts));
                    return;
                }

                rollback(contexts, volume.getUuid());
                completion.fail(errorCodeList.getCauses().get(0));
            }
        });
    }

    public void commit(List<Context> contexts) {
        for (Context context : safeContexts(contexts)) {
            context.extension.commitVolumeBackupEncryptionConversion(context.context);
        }
    }

    public void rollback(List<Context> contexts, String volumeUuid) {
        List<Context> reversed = new ArrayList<>(safeContexts(contexts));
        Collections.reverse(reversed);
        for (Context context : reversed) {
            try {
                context.extension.rollbackVolumeBackupEncryptionConversion(context.context);
            } catch (RuntimeException e) {
                logger.warn(String.format("failed to rollback volume backup encryption conversion for volume[uuid:%s]: %s",
                        volumeUuid, e.getMessage()), e);
            }
        }
    }

    public void cleanupCommitted(List<Context> contexts, String volumeUuid) {
        for (Context context : safeContexts(contexts)) {
            try {
                context.extension.afterVolumeBackupEncryptionConversionCommitted(context.context);
            } catch (RuntimeException e) {
                logger.warn(String.format("failed to run volume backup encryption conversion cleanup for volume[uuid:%s]: %s",
                        volumeUuid, e.getMessage()), e);
            }
        }
    }

    private List<Context> safeContexts(List<Context> contexts) {
        return contexts == null ? Collections.emptyList() : contexts;
    }

    private Context makeContext(VolumeBackupEncryptionConversionExtensionPoint extension,
                                VolumeBackupEncryptionConversionContext context) {
        return new Context(extension, context);
    }

    private void prepareExtension(VolumeBackupEncryptionConversionExtensionPoint ext,
                                  VolumeInventory volume,
                                  boolean targetEncrypted,
                                  String hostUuid,
                                  List<Context> contexts,
                                  WhileCompletion whileCompletion) {
        try {
            ext.prepareVolumeBackupEncryptionConversion(volume, targetEncrypted, hostUuid,
                    new ReturnValueCompletion<VolumeBackupEncryptionConversionContext>(whileCompletion) {
                        @Override
                        public void success(VolumeBackupEncryptionConversionContext context) {
                            if (context != null) {
                                contexts.add(makeContext(ext, context));
                            }
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            whileCompletion.addError(errorCode);
                            whileCompletion.allDone();
                        }
                    }
            );
        } catch (RuntimeException e) {
            whileCompletion.addError(operr("failed to prepare volume backup encryption conversion for volume[uuid:%s]: %s",
                    volume.getUuid(), e.getMessage()));
            whileCompletion.allDone();
        }
    }
}
