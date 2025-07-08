package org.zstack.resourceattribute;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.resourceattribute.api.APICreateResourceAttributeValueEvent;
import org.zstack.header.resourceattribute.api.APICreateResourceAttributeValueMsg;
import org.zstack.header.resourceattribute.api.APIDeleteResourceAttributeKeyEvent;
import org.zstack.header.resourceattribute.api.APIDeleteResourceAttributeKeyMsg;
import org.zstack.header.resourceattribute.api.APIDeleteResourceAttributeValueEvent;
import org.zstack.header.resourceattribute.api.APIDeleteResourceAttributeValueMsg;
import org.zstack.header.resourceattribute.api.APIUpdateResourceAttributeKeyEvent;
import org.zstack.header.resourceattribute.api.APIUpdateResourceAttributeKeyMsg;
import org.zstack.header.resourceattribute.entity.CreateResourceAttributeResult;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyInventory;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO_;
import org.zstack.header.resourceattribute.entity.ResourceAttributeValueVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeValueVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ResourceAttributeBase {
    private static final CLogger logger = Utils.getLogger(ResourceAttributeBase.class);

    private ResourceAttributeKeyVO self;
    @Autowired
    private CloudBus bus;
    @Autowired
    protected ThreadFacade threadFacade;
    @Autowired
    private DatabaseFacade databaseFacade;

    private String syncThreadName;

    ResourceAttributeBase(ResourceAttributeKeyVO vo) {
        self = vo;
        syncThreadName = "resource-attribute-" + self.getUuid();
    }

    void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateResourceAttributeValueMsg) {
            handle((APICreateResourceAttributeValueMsg) msg);
        } else if (msg instanceof APIDeleteResourceAttributeValueMsg){
            handle((APIDeleteResourceAttributeValueMsg) msg);
        } else if (msg instanceof APIUpdateResourceAttributeKeyMsg) {
            handle((APIUpdateResourceAttributeKeyMsg) msg);
        } else if (msg instanceof APIDeleteResourceAttributeKeyMsg) {
            handle((APIDeleteResourceAttributeKeyMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateResourceAttributeValueMsg msg) {
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("create-resource-attribute-value-with-key-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                CreateResourceAttributeValueContext context = new CreateResourceAttributeValueContext();
                context.value = msg.getValue();
                context.resourceUuids = msg.getResourceUuids();
                createResourceAttributeValue(context);

                APICreateResourceAttributeValueEvent event = new APICreateResourceAttributeValueEvent(msg.getId());
                event.setInventories(CreateResourceAttributeResult.valueOf(context.values));
                bus.publish(event);

                chain.next();
            }
        });
    }

    private void handle(APIDeleteResourceAttributeValueMsg msg) {
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("delete-resource-attribute-value-with-key-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                DeleteResourceAttributeValueContext context = new DeleteResourceAttributeValueContext();
                context.resourceUuids = msg.getResourceUuids();
                deleteResourceAttributeValue(context);

                APIDeleteResourceAttributeValueEvent event = new APIDeleteResourceAttributeValueEvent(msg.getId());
                bus.publish(event);

                chain.next();
            }
        });
    }

    private void handle(APIUpdateResourceAttributeKeyMsg msg) {
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("update-resource-attribute-key-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                if (msg.getDescription() != null) {
                    self.setDescription(msg.getDescription());
                }
                self = databaseFacade.updateAndRefresh(self);

                APIUpdateResourceAttributeKeyEvent event = new APIUpdateResourceAttributeKeyEvent(msg.getId());
                event.setInventory(ResourceAttributeKeyInventory.valueOf(self));
                bus.publish(event);
                chain.next();
            }
        });
    }

    private void handle(APIDeleteResourceAttributeKeyMsg msg) {
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("delete-resource-attribute-key-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                SQL.New(ResourceAttributeKeyVO.class)
                        .eq(ResourceAttributeKeyVO_.uuid, msg.getUuid())
                        .delete();
                APIDeleteResourceAttributeKeyEvent event = new APIDeleteResourceAttributeKeyEvent(msg.getId());
                bus.publish(event);
                chain.next();
            }
        });
    }

    static class CreateResourceAttributeValueContext {
        String value;
        List<String> resourceUuids;

        // results
        List<ErrorableValue<ResourceAttributeValueVO>> values;
    }

    private void createResourceAttributeValue(CreateResourceAttributeValueContext context) {
        context.values = new ArrayList<>(context.resourceUuids.size());
        Map<String, String> resourceUuidTypeMap = buildResourceUuidTypeMap(context.resourceUuids);

        List<ResourceAttributeValueVO> values = new ArrayList<>();
        for (String resourceUuid : context.resourceUuids) {
            ResourceAttributeValueVO value = new ResourceAttributeValueVO();
            value.setKeyUuid(self.getUuid());
            value.setResourceUuid(resourceUuid);
            value.setResourceType(resourceUuidTypeMap.get(resourceUuid));
            value.setValue(context.value);
            values.add(value);
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                for (List<String> sub : Lists.partition(context.resourceUuids, 100)) { // equals to update value
                    sql(ResourceAttributeValueVO.class)
                            .eq(ResourceAttributeValueVO_.keyUuid, self.getUuid())
                            .in(ResourceAttributeValueVO_.resourceUuid, sub)
                            .delete();
                }

                for (ResourceAttributeValueVO item : values) {
                    persist(item);
                    context.values.add(ErrorableValue.of(reload(item)));
                }
            }
        }.execute();
    }

    static class DeleteResourceAttributeValueContext {
        List<String> resourceUuids;
    }

    private void deleteResourceAttributeValue(DeleteResourceAttributeValueContext context) {
        for (List<String> sub : Lists.partition(context.resourceUuids, 100)) { // equals to update value
            SQL.New(ResourceAttributeValueVO.class)
                    .eq(ResourceAttributeValueVO_.keyUuid, self.getUuid())
                    .in(ResourceAttributeValueVO_.resourceUuid, sub)
                    .delete();
        }
    }

    // utils

    @Transactional(readOnly = true)
    protected Map<String, String> buildResourceUuidTypeMap(List<String> resourceUuids) {
        Map<String, String> resourceType = new HashMap<>();

        for (List<String> sub : Lists.partition(resourceUuids, 100)) {
            List<Tuple> types = Q.New(ResourceVO.class).select(ResourceVO_.uuid, ResourceVO_.resourceType).in(ResourceVO_.uuid, sub).listTuple();
            types.forEach(t -> resourceType.put((String) t.get(0), (String) t.get(1)));
        }

        return resourceType;
    }
}
