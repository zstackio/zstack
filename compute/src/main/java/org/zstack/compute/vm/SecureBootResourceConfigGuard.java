package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.devices.VmTpmManager;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ImageArchitecture;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.resourceconfig.APIDeleteResourceConfigMsg;
import org.zstack.resourceconfig.APIUpdateResourceConfigMsg;
import org.zstack.resourceconfig.APIUpdateResourceConfigsMsg;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.resourceconfig.ResourceConfigInventory;

import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_VM_10372;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_VM_10373;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_VM_10374;

public class SecureBootResourceConfigGuard implements Component, GlobalApiMessageInterceptor {
    private static final List<VmInstanceState> ALLOWED_STATES = Arrays.asList(VmInstanceState.Created, VmInstanceState.Stopped);

    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public boolean start() {
        ResourceConfig resourceConfig = rcf.getResourceConfig(VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT.getIdentity());
        resourceConfig.installValidatorExtension((resourceUuid, oldValue, newValue) -> {
            if (rcf.getResourceConfigValue(VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT, resourceUuid, String.class).equals(newValue)) {
                return;
            }

            VmInstanceState state = vmStateOf(resourceUuid);
            if (state != null && !ALLOWED_STATES.contains(state)) {
                throw new GlobalConfigException(String.format("cannot change %s of vm[uuid:%s] in state %s, the vm must be in state %s",
                        VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT.getCanonicalName(), resourceUuid, state, ALLOWED_STATES));
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(APIDeleteResourceConfigMsg.class, APIUpdateResourceConfigMsg.class, APIUpdateResourceConfigsMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeleteResourceConfigMsg) {
            validate((APIDeleteResourceConfigMsg) msg);
        }
        if (msg instanceof APIUpdateResourceConfigMsg) {
            APIUpdateResourceConfigMsg update = (APIUpdateResourceConfigMsg) msg;
            validateBootMode(update.getResourceUuid(), update.getIdentity(), update.getValue());
        }
        if (msg instanceof APIUpdateResourceConfigsMsg) {
            APIUpdateResourceConfigsMsg update = (APIUpdateResourceConfigsMsg) msg;
            update.getResourceConfigs().forEach(config -> validateBootMode(update.getResourceUuid(),
                    GlobalConfig.produceIdentity(config.getCategory(), config.getName()), config.getValue()));
        }
        return msg;
    }

    public static ErrorCode checkCreateArchitecture(VmInstanceInventory vm, String clusterUuid, ResourceConfigFacade rcf) {
        if (ImageArchitecture.x86_64.toString().equals(vm.getArchitecture())) {
            return null;
        }

        ResourceConfig config = rcf.getResourceConfig(VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT.getIdentity());
        // With automatic placement the selected cluster can override the global default.
        String sourceUuid = config.resourceConfigCreated(vm.getUuid()) ? vm.getUuid() : clusterUuid;
        if (sourceUuid == null || !Boolean.TRUE.equals(config.getResourceConfigValue(sourceUuid, Boolean.class))) {
            return null;
        }

        String bootMode = VmSystemTags.BOOT_MODE.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.BOOT_MODE_TOKEN);
        if (!VmTpmManager.isUefiBootMode(bootMode)) {
            return null;
        }
        return operr(ORG_ZSTACK_COMPUTE_VM_10374, "Secure Boot requires an x86_64 VM, but vm[uuid:%s] has architecture %s",
                vm.getUuid(), vm.getArchitecture());
    }

    private void validateBootMode(String resourceUuid, String identity, String value) {
        if (!VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT.getIdentity().equals(identity) || !"true".equals(value)
                || vmStateOf(resourceUuid) == null) {
            return;
        }

        String bootMode = VmSystemTags.BOOT_MODE.getTokenByResourceUuid(resourceUuid, VmSystemTags.BOOT_MODE_TOKEN);
        if (!VmTpmManager.isUefiBootMode(bootMode)) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10373,
                    "cannot enable Secure Boot on vm[uuid:%s], UEFI boot mode is required", resourceUuid));
        }
    }

    private void validate(APIDeleteResourceConfigMsg msg) {
        if (!VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT.getIdentity().equals(msg.getIdentity())) {
            return;
        }

        ResourceConfig resourceConfig = rcf.getResourceConfig(VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT.getIdentity());
        List<ResourceConfigInventory> configs = resourceConfig.getEffectiveResourceConfigs(msg.getResourceUuid());
        if (configs.isEmpty() || !msg.getResourceUuid().equals(configs.get(0).getResourceUuid())) {
            return;
        }

        String fallbackValue = configs.size() > 1 ? configs.get(1).getValue() : resourceConfig.defaultValue(String.class);
        if (configs.get(0).getValue().equals(fallbackValue)) {
            return;
        }

        VmInstanceState state = vmStateOf(msg.getResourceUuid());
        if (state != null && !ALLOWED_STATES.contains(state)) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10372,
                    "cannot delete %s of vm[uuid:%s] in state %s, the vm must be in state %s",
                    VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT.getCanonicalName(), msg.getResourceUuid(), state, ALLOWED_STATES));
        }
    }

    private static VmInstanceState vmStateOf(String resourceUuid) {
        return Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.state)
                .eq(VmInstanceVO_.uuid, resourceUuid)
                .findValue();
    }
}
