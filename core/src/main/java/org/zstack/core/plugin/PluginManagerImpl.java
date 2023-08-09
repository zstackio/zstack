package org.zstack.core.plugin;

import org.apache.commons.codec.digest.DigestUtils;
import org.zstack.abstraction.PluginCapabilityState;
import org.zstack.abstraction.PluginRegister;
import org.zstack.abstraction.PluginValidator;
import org.zstack.core.Platform;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Method;
import java.util.*;

import static org.zstack.core.Platform.operr;

/**
 * PluginManagerImpl implementation of PluginManager.
 */
public class PluginManagerImpl implements PluginManager, Component {
    private static final CLogger logger = Utils.getLogger(PluginManagerImpl.class);

    private final Set<Class<? extends PluginRegister>> pluginMetadata = new HashSet<>();
    private final Map<String, PluginRegister> pluginInstances = new HashMap<>();
    private final Map<Class<? extends PluginRegister>, List<PluginRegister>>
            pluginRegisterMap = new HashMap<>();
    private final Map<Class<? extends PluginRegister>, PluginValidator>
            pluginValidatorMap = new HashMap<>();

    private void collectPluginProtocolMetadata() {
        Platform.getReflections().getSubTypesOf(PluginRegister.class).forEach(clz -> {
            if (!clz.getCanonicalName().contains("org.zstack.abstraction")
                    || !clz.isInterface()) {
                return;
            }

            if (pluginMetadata.contains(clz)) {
                throw new CloudRuntimeException(
                        String.format("duplicate PluginProtocol[name: %s]", clz));
            }

            pluginMetadata.add(clz);
        });
    }

    private void registerPluginAsSingleton(
            Class<? extends PluginRegister> pluginRegisterClz) {
        try {
            PluginRegister pluginRegister = pluginRegisterClz
                    .getConstructor()
                    .newInstance();
            if (pluginInstances.containsKey(pluginRegister.productKey())) {
                throw new CloudRuntimeException(
                        String.format("duplicate plugin[class: %s]", pluginRegisterClz));
            }

            if (pluginValidatorMap.containsKey(pluginRegisterClz)) {
                pluginValidatorMap.get(pluginRegisterClz).validate(pluginRegister);
            }

            logger.debug(
                    String.format("detect plugin class: %s, name: %s, version: %s," +
                                    " capabilities: \n %s",
                            pluginRegisterClz.getCanonicalName(),
                            pluginRegister.version(),
                            pluginRegister.productName(),
                            JSONObjectUtil.toJsonString(pluginRegister.capabilities())));

            verifyPluginProduct(pluginRegister);
            pluginInstances.put(pluginRegister.productKey(), pluginRegister);
            pluginRegisterMap.computeIfAbsent(pluginRegisterClz, k -> new ArrayList<>());
            pluginRegisterMap.get(pluginRegisterClz).add(pluginRegister);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private void getPluginInterfaceSingletons(Class<? extends PluginRegister> clz) {
        Platform.getReflections()
                .getSubTypesOf(clz)
                .forEach(this::registerPluginAsSingleton);
    }

    private void loadPluginsFromMetadata() {
        pluginMetadata.forEach(this::getPluginInterfaceSingletons);

        pluginRegisterMap.forEach((pluginClazz, instanceList) -> {
            PluginValidator validator = pluginValidatorMap.get(pluginClazz);
            if (validator == null) {
                return;
            }

            validator.validateAllPlugins(instanceList);
        });
    }

    private void verifyPluginProduct(PluginRegister pluginRegister) {
        if (!PluginGlobalConfig.ALLOW_UNKNOWN_PRODUCT_PLUGIN.value(Boolean.class)
                && pluginRegister.productKey() == null) {
            throw new OperationFailureException(operr("unknown product plugin name: %s",
                    pluginRegister.productName()));
        }

        doVerification(pluginRegister.productName(), pluginRegister.productKey());
    }

    private void doVerification(String productName, String productKey) {
        if (!DigestUtils.md5Hex(productName).equals(productKey)) {
            throw new OperationFailureException(operr("failed to verify product: %s",
                    productName));
        }
    }

    private void collectPluginValidators() {
        Platform.getReflections().getSubTypesOf(PluginValidator.class).forEach(clz -> {
            if (!clz.getCanonicalName().contains("org.zstack.abstraction")
                    || !clz.isInterface()) {
                return;
            }

            if (pluginMetadata.contains(clz)) {
                throw new CloudRuntimeException(
                        String.format("duplicate PluginValidator[name: %s]", clz));
            }

            try {
                PluginValidator pluginValidator = clz
                        .getConstructor()
                        .newInstance();

                pluginValidatorMap.put(pluginValidator.pluginClass(), pluginValidator);
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        });
    }

    @Override
    public boolean start() {
        collectPluginProtocolMetadata();
        collectPluginValidators();
        loadPluginsFromMetadata();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public boolean isCapabilitySupported(String pluginProductKey,
                                         String capability) {
        return pluginInstances.get(pluginProductKey)
                .capabilities()
                .get(capability) == PluginCapabilityState.SUPPORTED;
    }

    @Override
    public <T extends PluginRegister> T getPlugin(String pluginProductKey) {
        if (!pluginInstances.containsKey(pluginProductKey)) {
            throw new CloudRuntimeException(String.format("Unsupported plugin %s",
                    pluginProductKey));
        }

        return (T) pluginInstances.get(pluginProductKey);
    }

    @Override
    public <T extends PluginRegister> List<T> getPluginList(Class<? extends PluginRegister> pluginClass) {
        return (List<T>) pluginRegisterMap.get(pluginClass);
    }
}
