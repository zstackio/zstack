package org.zstack.core.config.resourceconfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.argerr;

import javax.persistence.Tuple;
import java.lang.reflect.Field;
import java.util.*;

public class ResourceConfigFacadeImpl extends AbstractService implements ResourceConfigFacade, PrepareDbInitialValueExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ResourceConfigFacadeImpl.class);

    private Map<String, Set<String>> boundResourceConfigTypes = new HashMap<>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private GlobalConfigFacade gcf;

    @Override
    public void prepareDbInitialValue() {
        BeanUtils.reflections.getFieldsAnnotatedWith(BindResourceConfig.class).forEach(field -> {
            try {
                buildBoundTypes(field);
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        });

        new SQLBatch() {
            @Override
            protected void scripts() {
               List<Tuple> tss = sql("select vo.category, vo.name from ResourceConfigVO vo group by vo.category,vo.name").list();
               tss.forEach(ts -> {
                   String category = ts.get(0, String.class);
                   String name = ts.get(1, String.class);
                   int n = sql("delete from ResourceConfigVO vo where vo.category = :category and vo.name = :name")
                           .param("category", category).param("name", name).execute();
                   logger.debug(String.format("delete %s stale items[category:%s, name:%s] from resource config", n, category, name));
               });
            }
        }.execute();
    }

    private void buildBoundTypes(Field field) throws Exception {
        BindResourceConfig at = field.getAnnotation(BindResourceConfig.class);
        GlobalConfig gc = (GlobalConfig) field.get(null);
        Set<String> types = boundResourceConfigTypes.computeIfAbsent(gc.getCanonicalName(), x->new HashSet<>());
        for (Class clz : at.value()) {
            types.add(clz.getName());
        }
    }

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
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDeleteResourceConfigMsg msg) {
    }

    private void handle(APIUpdateResourceConfigMsg msg) {
        GlobalConfig gc = gcf.getAllConfig().get(String.format("%s.%s", msg.getCategory(), msg.getName()));
        if (gc == null) {
            throw new OperationFailureException(argerr("no global config[category:%s, name:%s] found", msg.getCategory(), msg.getName()));
        }

        gc.getValidators().forEach(v -> v.validateGlobalConfig(msg.getCategory(), msg.getName(), gc.value(), msg.getValue()));

        ResourceConfigInventory inv = new SQLBatchWithReturn<ResourceConfigInventory>() {
            @Override
            protected ResourceConfigInventory scripts() {
                ResourceVO resourceVO = q(ResourceVO.class).eq(ResourceVO_.uuid, msg.getResourceUuid()).find();
                if (resourceVO == null) {
                    throw new OperationFailureException(argerr("no resource[uuid:%s] found", msg.getResourceUuid()));
                }

                ResourceConfigVO vo = q(ResourceConfigVO.class)
                        .eq(ResourceConfigVO_.name, msg.getName())
                        .eq(ResourceConfigVO_.category, msg.getCategory())
                        .eq(ResourceConfigVO_.resourceUuid, msg.getResourceUuid())
                        .find();

                if (vo != null) {
                    vo.setValue(msg.getValue());
                    merge(vo);
                    vo = reload(vo);
                    return vo.toInventory();
                }

                vo = new ResourceConfigVO();
                vo.setUuid(Platform.getUuid());
                vo.setCategory(msg.getCategory());
                vo.setName(msg.getName());
                vo.setValue(msg.getValue());
                vo.setDescription(gc.getDescription());
                vo.setResourceUuid(msg.getResourceUuid());
                vo.setResourceType(vo.getResourceType());
                persist(vo);
                vo = reload(vo);
                return vo.toInventory();
            }
        }.execute();

        APIUpdateResourceConfigEvent evt = new APIUpdateResourceConfigEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
