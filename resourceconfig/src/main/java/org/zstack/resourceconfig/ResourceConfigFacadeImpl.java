package org.zstack.resourceconfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.*;
import org.zstack.header.AbstractService;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class ResourceConfigFacadeImpl extends AbstractService implements ResourceConfigFacade {
    private static final CLogger logger = Utils.getLogger(ResourceConfigFacadeImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private GlobalConfigFacade gcf;

    protected Map<String, ResourceConfig> resourceConfigs = new HashMap<>();

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateResourceConfigMsg) {
            handle((APIUpdateResourceConfigMsg) msg);
        } else if (msg instanceof APIDeleteResourceConfigMsg) {
            handle((APIDeleteResourceConfigMsg) msg);
        } else if (msg instanceof APIGetResourceBindableConfigMsg) {
            handle((APIGetResourceBindableConfigMsg) msg);
        } else if (msg instanceof APIGetResourceConfigMsg) {
            handle((APIGetResourceConfigMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDeleteResourceConfigMsg msg) {
        ResourceConfig rc = getResourceConfig(msg.getIdentity());
        rc.deleteValue(msg.getResourceUuid());
        bus.publish(new APIDeleteResourceConfigEvent(msg.getId()));
    }

    private void handle(APIUpdateResourceConfigMsg msg) {
        ResourceConfig rc = getResourceConfig(msg.getIdentity());
        rc.updateValue(msg.getResourceUuid(), msg.getValue());

        APIUpdateResourceConfigEvent evt = new APIUpdateResourceConfigEvent(msg.getId());
        evt.setInventory(ResourceConfigInventory.valueOf(rc.loadConfig(msg.getResourceUuid())));
        bus.publish(evt);
    }

    private void handle(APIGetResourceBindableConfigMsg msg) {
        APIGetResourceBindableConfigReply reply = new APIGetResourceBindableConfigReply();
        List<APIGetResourceBindableConfigReply.ResourceBindableConfigStruct> results = new ArrayList<>();
        if (msg.getCategory() == null) {
            results.addAll(APIGetResourceBindableConfigReply.ResourceBindableConfigStruct.valueOf(resourceConfigs.values()));
        } else {
            List<String> ids = resourceConfigs.keySet().stream().filter(it -> it.startsWith(msg.getCategory() + ".")).collect(Collectors.toList());
            ids.forEach(id -> results.add(APIGetResourceBindableConfigReply.ResourceBindableConfigStruct.valueOf(resourceConfigs.get(id))));
        }

        reply.setBindableConfigs(results);
        bus.reply(msg, reply);
    }

    private void handle(APIGetResourceConfigMsg msg) {
        ResourceConfig rc = getResourceConfig(msg.getIdentity());
        APIGetResourceConfigReply reply = new APIGetResourceConfigReply();
        List<ResourceConfigInventory> configs = rc.getEffectiveResourceConfigs(msg.getResourceUuid());
        reply.setEffectiveConfigs(configs);
        reply.setValue(configs.isEmpty() ? rc.defaultValue(String.class) : configs.get(0).getValue());
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    public boolean start() {
        BeanUtils.reflections.getFieldsAnnotatedWith(BindResourceConfig.class).forEach(field -> {
            try {
                buildResourceConfig(field);
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        });

        initResourceConfig();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public ResourceConfig getResourceConfig(String identity) {
        return resourceConfigs.get(identity);
    }

    @Override
    public <T> T getResourceConfigValue(GlobalConfig gc, String resourceUuid, Class<T> clz) {
        ResourceConfig rc = resourceConfigs.get(gc.getIdentity());
        if (rc == null) {
            logger.debug(String.format("resource[uuid:%s] is not bound to global config[category:%s, name:%s], use global config instead", resourceUuid, gc.getCategory(), gc.getName()));
            return gc.value(clz);
        }

        return rc.getResourceConfigValue(resourceUuid, clz);
    }

    protected void buildResourceConfig(Field field) throws Exception {
        BindResourceConfig at = field.getAnnotation(BindResourceConfig.class);
        GlobalConfig gc = (GlobalConfig) field.get(null);
        resourceConfigs.putIfAbsent(gc.getIdentity(), ResourceConfig.valueOf(gc, at));
    }

    private void initResourceConfig() {
        resourceConfigs.values().forEach(ResourceConfig::init);
    }
}
