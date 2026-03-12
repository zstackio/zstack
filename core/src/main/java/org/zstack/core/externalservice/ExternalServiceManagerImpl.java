package org.zstack.core.externalservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.GlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.external.service.APIAddExternalServiceConfigurationEvent;
import org.zstack.header.core.external.service.APIAddExternalServiceConfigurationMsg;
import org.zstack.header.core.external.service.APIDeleteExternalServiceConfigurationEvent;
import org.zstack.header.core.external.service.APIDeleteExternalServiceConfigurationMsg;
import org.zstack.header.core.external.service.APIGetExternalServicesMsg;
import org.zstack.header.core.external.service.APIGetExternalServicesReply;
import org.zstack.header.core.external.service.APIReloadExternalServiceEvent;
import org.zstack.header.core.external.service.APIReloadExternalServiceMsg;
import org.zstack.header.core.external.service.APIUpdateExternalServiceConfigurationEvent;
import org.zstack.header.core.external.service.APIUpdateExternalServiceConfigurationMsg;
import org.zstack.header.core.external.service.ApplyExternalConfigurationResult;
import org.zstack.header.core.external.service.ApplyExternalServiceConfigurationMsg;
import org.zstack.header.core.external.service.ApplyExternalServiceConfigurationReply;
import org.zstack.header.core.external.service.ExternalServiceConfigurationInventory;
import org.zstack.header.core.external.service.ExternalServiceConfigurationVO;
import org.zstack.header.core.external.service.ExternalServiceInventory;
import org.zstack.header.core.external.service.ExternalServiceStatus;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class ExternalServiceManagerImpl extends AbstractService implements ExternalServiceManager {
    @Autowired
    public CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    private final Map<String, ExternalService> services = new ConcurrentHashMap<>();

    @Override
    public ExternalService registerService(ExternalService service) {
        if (services.containsKey(service.getName())) {
            throw new OperationFailureException(operr(ORG_ZSTACK_CORE_EXTERNALSERVICE_10000, "service[%s] has been registered", service.getName()));
        }

        services.put(service.getName(), service);
        return service;
    }

    @Override
    public void deregisterService(String name) {
        services.remove(name);
    }

    @Override
    public ExternalService getService(String name) {
        return services.get(name);
    }

    @Override
    public ExternalService getService(String name, Supplier<ExternalService> supplier) {
        ExternalService service = services.get(name);
        if (service != null) {
            return service;
        }

        service = supplier.get();
        return registerService(service);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        }  else {
            handleLocalMessage(msg);
        }
    }

    public void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetExternalServicesMsg) {
            handle((APIGetExternalServicesMsg) msg);
        } else if (msg instanceof APIReloadExternalServiceMsg) {
            handle((APIReloadExternalServiceMsg) msg);
        } else if (msg instanceof APIAddExternalServiceConfigurationMsg){
            handle((APIAddExternalServiceConfigurationMsg) msg);
        } else if (msg instanceof APIUpdateExternalServiceConfigurationMsg) {
            handle((APIUpdateExternalServiceConfigurationMsg) msg);
        } else if (msg instanceof APIDeleteExternalServiceConfigurationMsg) {
            handle((APIDeleteExternalServiceConfigurationMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof ApplyExternalServiceConfigurationMsg) {
            handle((ApplyExternalServiceConfigurationMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIReloadExternalServiceMsg msg) {
        APIReloadExternalServiceEvent event = new APIReloadExternalServiceEvent(msg.getId());
        ExternalService service = services.get(msg.getName());
        if (service == null) {
            event.setError(operr(ORG_ZSTACK_CORE_EXTERNALSERVICE_10001, "service[%s] is not registered", msg.getName()));
            bus.publish(event);
            return;
        }

        if (!service.getExternalServiceCapabilities().isReloadConfig()) {
            event.setError(operr(ORG_ZSTACK_CORE_EXTERNALSERVICE_10002, "service[%s] does not support reload config", msg.getName()));
        }

        if (service.isAlive()) {
            service.reload();
        } else {
            event.setError(operr(ORG_ZSTACK_CORE_EXTERNALSERVICE_10003, "service[%s] is not running", msg.getName()));
        }

        bus.publish(event);
    }

    private void handle(APIGetExternalServicesMsg msg) {
        APIGetExternalServicesReply reply = new APIGetExternalServicesReply();
        reply.setInventories(new ArrayList<>());

        services.forEach((name, service) -> {
            ExternalServiceInventory inv = new ExternalServiceInventory();
            inv.setName(name);
            inv.setStatus(service.isAlive() ? ExternalServiceStatus.RUNNING.toString() : ExternalServiceStatus.STOPPED.toString());
            inv.setCapabilities(service.getExternalServiceCapabilities());
            inv.setServiceType(service.getServiceType());
            reply.getInventories().add(inv);
        });

        bus.reply(msg, reply);
    }

    private void handle(APIAddExternalServiceConfigurationMsg msg ){
        APIAddExternalServiceConfigurationEvent event = new APIAddExternalServiceConfigurationEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                createExternalServiceConfiguration(msg, event, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("create-update-delete-external-service-configuration-%s", msg.getExternalServiceType());
            }

            @Override
            public String getName() {
                return String.format("create-external-service-configuration-type-%s", msg.getExternalServiceType());
            }
        });

    }

    private void createExternalServiceConfiguration(APIAddExternalServiceConfigurationMsg msg, APIAddExternalServiceConfigurationEvent evt, Completion completion) {
        // create db record
        ExternalServiceConfigurationVO configurationVO = new ExternalServiceConfigurationVO();
        configurationVO.setUuid(msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid());
        configurationVO.setServiceType(msg.getExternalServiceType());
        configurationVO.setConfiguration(msg.getConfiguration());
        configurationVO.setDescription(msg.getDescription());
        configurationVO = dbf.persistAndRefresh(configurationVO);

        ExternalServiceConfigurationInventory inv = ExternalServiceConfigurationInventory.valueOf(configurationVO);

        applyExternalServiceConfigurationToAllNodes(configurationVO.getServiceType(), new ReturnValueCompletion<List<ApplyExternalConfigurationResult>>(completion) {
            @Override
            public void success(List<ApplyExternalConfigurationResult> returnValue) {
                evt.setInventory(inv);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void handle(APIUpdateExternalServiceConfigurationMsg msg){
        APIUpdateExternalServiceConfigurationEvent event = new APIUpdateExternalServiceConfigurationEvent(msg.getId());
        ExternalServiceConfigurationVO vo = dbf.findByUuid(msg.getUuid(), ExternalServiceConfigurationVO.class);
        final String syncKey = vo != null ? vo.getServiceType() : msg.getUuid();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                updateExternalServiceConfiguration(msg, event, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("create-update-delete-external-service-configuration-%s", syncKey);
            }

            @Override
            public String getName() {
                return String.format("update-external-service-configuration-%s", msg.getUuid());
            }
        });
    }

    private void updateExternalServiceConfiguration(APIUpdateExternalServiceConfigurationMsg msg, APIUpdateExternalServiceConfigurationEvent evt, Completion completion) {
        ExternalServiceConfigurationVO vo = dbf.findByUuid(msg.getUuid(), ExternalServiceConfigurationVO.class);

        if (vo == null) {
            completion.fail(operr("unable to find external service configuration with uuid [%s]", msg.getUuid()));
            return;
        }

        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }
        vo = dbf.updateAndRefresh(vo);

        evt.setInventory(ExternalServiceConfigurationInventory.valueOf(vo));
        completion.success();
    }

    private void handle(APIDeleteExternalServiceConfigurationMsg msg) {
        APIDeleteExternalServiceConfigurationEvent event = new APIDeleteExternalServiceConfigurationEvent(msg.getId());
        ExternalServiceConfigurationVO vo = dbf.findByUuid(msg.getUuid(), ExternalServiceConfigurationVO.class);
        final String syncKey = vo != null ? vo.getServiceType() : msg.getUuid();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                deleteExternalServiceConfiguration(msg, event, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("create-update-delete-external-service-configuration-%s", syncKey);
            }

            @Override
            public String getName() {
                return String.format("delete-external-service-configuration-%s", msg.getUuid());
            }
        });
    }

    private void deleteExternalServiceConfiguration(APIDeleteExternalServiceConfigurationMsg msg, APIDeleteExternalServiceConfigurationEvent evt, Completion completion) {
        // delete db record
        ExternalServiceConfigurationVO vo = dbf.findByUuid(msg.getUuid(), ExternalServiceConfigurationVO.class);
        String serviceType;
        if (vo != null) {
            serviceType = vo.getServiceType();
            dbf.remove(vo);
        } else {
            completion.success();
            return;
        }

        applyExternalServiceConfigurationToAllNodes(serviceType, new ReturnValueCompletion<List<ApplyExternalConfigurationResult>>(completion) {
            @Override
            public void success(List<ApplyExternalConfigurationResult> returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void applyExternalServiceConfigurationToAllNodes(String serviceType, ReturnValueCompletion<List<ApplyExternalConfigurationResult>> completion) {
        final List<ApplyExternalConfigurationResult> results = Collections.synchronizedList(new ArrayList<>());

        FlowChain chain = new SimpleFlowChain();
        chain.setName("apply-external-service-configuration-to-all-nodes");
        chain.then(new Flow() {
            String __name__ = "apply-external-service-configuration";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> mnUuids = Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid).listValues();

                final ErrorCode[] errorCode = new ErrorCode[1];
                new While<>(mnUuids).each((mnUuid, whileCompletion) -> {
                    ApplyExternalServiceConfigurationMsg amsg = new ApplyExternalServiceConfigurationMsg();
                    amsg.setServiceType(serviceType);
                    bus.makeServiceIdByManagementNodeId(amsg, SERVICE_ID, mnUuid);
                    bus.send(amsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            ApplyExternalConfigurationResult result = new ApplyExternalConfigurationResult();
                            result.setManagementNodeUuid(mnUuid);
                            results.add(result);

                            if (!reply.isSuccess()) {
                                result.setErrorCode(reply.getError());
                                errorCode[0] = reply.getError();
                                whileCompletion.allDone();
                                return;
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCode[0] != null) {
                            trigger.fail(errorCode[0]);
                            return;
                        }
                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(results);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(ApplyExternalServiceConfigurationMsg msg) {
        ApplyExternalServiceConfigurationReply reply = new ApplyExternalServiceConfigurationReply();

        regenerateExternalServiceConfiguration(msg.getServiceType(), new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String returnValue) {
                reply.setValue(returnValue);
                reply.setManagementNodeUuid(Platform.getManagementServerId());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void regenerateExternalServiceConfiguration(String serviceType, ReturnValueCompletion<String> completion) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            completion.success(serviceType);
            return;
        }
        for (ExternalService service : services.values()) {
            if (serviceType.equals(service.getServiceType())) {
                try{
                    service.externalConfig(serviceType);
                    completion.success(serviceType);
                } catch (Exception e) {
                    completion.fail(operr("failed to apply external service configuration for type [%s]: %s", serviceType, e.getMessage()));
                }
                return;
            }
        }
        completion.fail(operr("unable to find external service type [%s]", serviceType));
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }
}
