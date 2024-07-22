package org.zstack.kvm.xmlhook;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.util.StringUtils;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.CheckAndStartVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.utils.CollectionUtils;

import java.util.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class XmlHookBase {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;

    protected XmlHookVO self;

    protected String syncThreadName;

    public XmlHookBase(XmlHookVO self) {
        this.self = self;
        this.syncThreadName = "Xml-Hook-" + self.getUuid();
    }

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
        if (msg instanceof APIUpdateVmUserDefinedXmlHookScriptMsg) {
            handle((APIUpdateVmUserDefinedXmlHookScriptMsg) msg);
        } else if (msg instanceof APIExpungeVmUserDefinedXmlHookScriptMsg) {
            handle((APIExpungeVmUserDefinedXmlHookScriptMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIExpungeVmUserDefinedXmlHookScriptMsg msg) {
        APIExpungeVmUserDefinedXmlHookScriptEvent event = new APIExpungeVmUserDefinedXmlHookScriptEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                expungeVmUserDefinedXmlHook(msg, new Completion(chain) {
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
            public String getName() {
                return String.format("expunge-xml-hook-%s",msg.getUuid());
            }
        });

    }

    private void expungeVmUserDefinedXmlHook(APIExpungeVmUserDefinedXmlHookScriptMsg msg, Completion completion) {
        dbf.remove(self);
        completion.success();
    }

    private void handle(APIUpdateVmUserDefinedXmlHookScriptMsg msg) {
        APIUpdateVmUserDefinedXmlHookScriptEvent event = new APIUpdateVmUserDefinedXmlHookScriptEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                updateVmUserDefinedXmlHook(msg, new ReturnValueCompletion<XmlHookInventory>(chain) {
                    @Override
                    public void success(XmlHookInventory inv) {
                        event.setInventory(inv);
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
            public String getName() {
                return String.format("update-xml-hook-%s", msg.getUuid());
            }
        });
    }

    private void updateVmUserDefinedXmlHook(APIUpdateVmUserDefinedXmlHookScriptMsg msg, ReturnValueCompletion<XmlHookInventory> completion) {
        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("update-user-defined-xml-hook-%s-script", msg.getXmlHookUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "refresh-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (msg.getName() != null) {
                    self.setName(msg.getName());
                }
                if (msg.getDescription() != null) {
                    self.setDescription(msg.getDescription());
                }
                if (msg.getHookScript() != null) {
                    self.setHookScript(msg.getHookScript());
                }
                dbf.updateAndRefresh(self);
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "startup-vm-instances";

            @Override
            public boolean skip(Map data) {
                return !Objects.equals(msg.getStartupStrategy(), VmInstanceConstant.VmOperation.Reboot.toString());
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> vmUuids = Q.New(XmlHookVmInstanceRefVO.class)
                        .select(XmlHookVmInstanceRefVO_.vmInstanceUuid)
                        .eq(XmlHookVmInstanceRefVO_.xmlHookUuid, msg.getXmlHookUuid()).listValues();
                if (CollectionUtils.isEmpty(vmUuids)) {
                    trigger.next();
                    return;
                }

                List<ErrorCode> errs = new ArrayList<>();
                new While<>(vmUuids).each((vmUuid, wcompl) -> {
                    CheckAndStartVmInstanceMsg msg = new CheckAndStartVmInstanceMsg();
                    msg.setVmInstanceUuid(vmUuid);
                    bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
                    bus.send(msg, new CloudBusCallBack(wcompl) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errs.add(reply.getError());
                            } else {
                                wcompl.done();
                            }
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errs.isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errs.get(0));
                        }
                    }
                });
            }
        });
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(XmlHookInventory.valueOf(dbf.findByUuid(msg.getXmlHookUuid(), XmlHookVO.class)));
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }
}

