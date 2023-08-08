package org.zstack.kvm.xmlhook;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.util.StringUtils;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.directory.*;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

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
        XmlHookVO vo = dbf.findByUuid(msg.getUuid(), XmlHookVO.class);
        dbf.remove(vo);
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
                updateVmUserDefinedXmlHook(msg, event, new Completion(chain) {
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
                return String.format("update-xml-hook-%s", msg.getUuid());
            }
        });
    }

    private void updateVmUserDefinedXmlHook(APIUpdateVmUserDefinedXmlHookScriptMsg msg, APIUpdateVmUserDefinedXmlHookScriptEvent event, Completion completion) {
        XmlHookVO vo = Q.New(XmlHookVO.class).eq(XmlHookVO_.uuid, msg.getUuid()).find();
        if (msg.getName() != null) {
            vo.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            vo.setName(msg.getDescription());
        }
        if (msg.getHookScript() != null) {
            vo.setHookScript(msg.getHookScript());
        }
        XmlHookVO updateVO = dbf.updateAndRefresh(vo);
        event.setInventory(XmlHookInventory.valueOf(updateVO));
        completion.success();
    }
}

