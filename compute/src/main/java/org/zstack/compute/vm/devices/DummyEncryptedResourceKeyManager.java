package org.zstack.compute.vm.devices;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class DummyEncryptedResourceKeyManager implements EncryptedResourceKeyManager {
    private static final CLogger logger = Utils.getLogger(DummyEncryptedResourceKeyManager.class);

    @Override
    public KeyBinding findKeyBinding(String resourceType, String resourceUuid) {
        return null;
    }

    @Override
    public Map<String, KeyBinding> findKeyBindings(String resourceType, Collection<String> resourceUuids) {
        return Collections.emptyMap();
    }

    @Override
    public void removeKeyBinding(String resourceType, String resourceUuid) {
    }

    @Override
    public void getOrCreateKey(GetOrCreateResourceKeyContext ctx,
                               ReturnValueCompletion<ResourceKeyResult> completion) {
        logger.warn(String.format("crypto module not installed, cannot create resource key for %s[uuid:%s]",
                ctx.getResourceType(), ctx.getResourceUuid()));
        completion.fail(operr(ORG_ZSTACK_COMPUTE_VM_DEVICES_10009,
                "crypto module is not installed, cannot manage resource encryption keys"));
    }

    @Override
    public ResourceKeyResult getKey(GetOrCreateResourceKeyContext ctx) {
        logger.warn(String.format("crypto module not installed, cannot get resource key for %s[uuid:%s]",
                ctx.getResourceType(), ctx.getResourceUuid()));
        throw new OperationFailureException(operr(ORG_ZSTACK_COMPUTE_VM_DEVICES_10009,
                "crypto module is not installed, cannot manage resource encryption keys"));
    }

    @Override
    public void rollbackCreatedKey(ResourceKeyResult result, Completion completion) {
        completion.success();
    }
}
