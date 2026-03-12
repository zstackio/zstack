package org.zstack.kvm.tpm;

import org.zstack.header.core.Completion;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class DummyTpmEncryptedResourceKeyBackend implements TpmEncryptedResourceKeyBackend {
    private static final CLogger logger = Utils.getLogger(DummyTpmEncryptedResourceKeyBackend.class);

    @Override
    public void cloneEncryptedResourceKey(CloneEncryptedResourceKeyContext context, Completion completion) {
        // do nothing
        logger.debug("ignore clone encrypted resource key request for TPM uuid "
                + context.srcTpmUuid + " -> " + context.dstTpmUuid);
        completion.success();
    }
}
