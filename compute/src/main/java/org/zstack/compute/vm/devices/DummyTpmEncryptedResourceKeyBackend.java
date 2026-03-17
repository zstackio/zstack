package org.zstack.compute.vm.devices;

import org.zstack.header.core.Completion;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class DummyTpmEncryptedResourceKeyBackend implements TpmEncryptedResourceKeyBackend {
    private static final CLogger logger = Utils.getLogger(DummyTpmEncryptedResourceKeyBackend.class);

    @Override
    public void attachKeyProviderToTpm(String tpmUuid, String keyProviderUuid) {
        logger.debug("ignore attach key provider to TPM request for TPM uuid " + tpmUuid +
                " and key provider uuid " + keyProviderUuid);
    }

    @Override
    public void detachKeyProviderFromTpm(String tpmUuid) {
        logger.debug("ignore detach key provider from TPM request for TPM uuid " + tpmUuid);
    }

    @Override
    public String findKeyProviderUuidByTpm(String tpmUuid) {
        return null;
    }

    @Override
    public String findKeyProviderNameByTpm(String tpmUuid) {
        return null;
    }

    @Override
    public void cloneEncryptedResourceKey(CloneEncryptedResourceKeyContext context, Completion completion) {
        // do nothing
        logger.debug("ignore clone encrypted resource key request for TPM uuid "
                + context.srcTpmUuid + " -> " + context.dstTpmUuid);
        completion.success();
    }
}
