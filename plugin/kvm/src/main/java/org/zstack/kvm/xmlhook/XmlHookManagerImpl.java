package org.zstack.kvm.xmlhook;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.directory.DirectoryMessage;
import org.zstack.directory.ResourceDirectoryRefVO;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.vm.VmInstanceBeforeStartExtensionPoint;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import static org.zstack.core.Platform.err;

public class XmlHookManagerImpl extends AbstractService implements XmlHookManager, Component,
        PrepareDbInitialValueExtensionPoint, VmInstanceBeforeStartExtensionPoint {
    private static final CLogger logger = Utils.getLogger(XmlHookManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof XmlHookMessage) {
            passThrough((XmlHookMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateVmUserDefinedXmlHookScriptMsg) {
            handle((APICreateVmUserDefinedXmlHookScriptMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(XmlHookMessage msg) {
        XmlHookVO vo = dbf.findByUuid(msg.getXmlHookUuid(), XmlHookVO.class);
        if (vo == null) {
            bus.replyErrorByMessageType((Message) msg, err(SysErrors.RESOURCE_NOT_FOUND, "unable to find xmlHook[uuid=%s]", msg.getXmlHookUuid()));
            return;
        }

        XmlHookBase base = new XmlHookBase(vo);
        base.handleMessage((Message) msg);
    }

    private void handle(APICreateVmUserDefinedXmlHookScriptMsg msg) {
        APICreateVmUserDefinedXmlHookScriptEvent event = new APICreateVmUserDefinedXmlHookScriptEvent(msg.getId());
        XmlHookVO vo = new XmlHookVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setHookScript(msg.getHookScript());
        vo.setType(XmlHookType.Customization);
        vo.setCreateDate(new Timestamp(new Date().getTime()));
        dbf.persist(vo);
        event.setInventory(XmlHookInventory.valueOf(vo));
        bus.publish(event);
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
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    public void prepareDbInitialValue() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                String name = String.format("%s (for libvirt %s)", XmlHookConstant.SET_GPU_MEMORY, XmlHookConstant.LIBVIT_VERSION_4_9_0);
                if (!q(XmlHookVO.class)
                        .eq(XmlHookVO_.name, name)
                        .eq(XmlHookVO_.type, XmlHookType.System).isExists()) {
                    XmlHookVO vo = new XmlHookVO();
                    vo.setUuid(Platform.getUuid());
                    vo.setName(name);
                    vo.setType(XmlHookType.System);
                    vo.setHookScript(XmlHookConstant.SET_GPU_MEMORY_HOOK);
                    vo.setLibvirtVersion(XmlHookConstant.LIBVIT_VERSION_4_9_0);
                    persist(vo);
                    flush();

                    logger.debug(String.format("Created initial system xml hook[name:%s]", name));
                }

                name = String.format("%s (for libvirt %s)", XmlHookConstant.SET_GPU_MEMORY, XmlHookConstant.LIBVIT_VERSION_6_0_0);
                if (!q(XmlHookVO.class)
                        .eq(XmlHookVO_.name, name)
                        .eq(XmlHookVO_.type, XmlHookType.System).isExists()) {
                    XmlHookVO vo = new XmlHookVO();
                    vo.setUuid(Platform.getUuid());
                    vo.setName(name);
                    vo.setType(XmlHookType.System);
                    vo.setHookScript(XmlHookConstant.SET_GPU_MEMORY_HOOK);
                    vo.setLibvirtVersion(XmlHookConstant.LIBVIT_VERSION_6_0_0);
                    persist(vo);
                    flush();

                    logger.debug(String.format("Created initial system xml hook[name:%s]", name));
                }

                name = String.format("%s (for libvirt %s)", XmlHookConstant.SET_GPU_MEMORY, XmlHookConstant.LIBVIT_VERSION_8_0_0);
                if (!q(XmlHookVO.class)
                        .eq(XmlHookVO_.name, name)
                        .eq(XmlHookVO_.type, XmlHookType.System).isExists()) {
                    XmlHookVO vo = new XmlHookVO();
                    vo.setUuid(Platform.getUuid());
                    vo.setName(name);
                    vo.setType(XmlHookType.System);
                    vo.setHookScript(XmlHookConstant.SET_GPU_MEMORY_HOOK);
                    vo.setLibvirtVersion(XmlHookConstant.LIBVIT_VERSION_8_0_0);
                    persist(vo);
                    flush();

                    logger.debug(String.format("Created initial system xml hook[name:%s]", name));
                }
            }
        }.execute();
    }

    @Override
    public ErrorCode handleSystemTag(String vmUuid, List<String> tags) {
        PatternedSystemTag tag = VmSystemTags.XML_HOOK;
        String token = VmSystemTags.XML_HOOK_TOKEN;

        String xmlhookUuid = SystemTagUtils.findTagValue(tags, tag, token);
        if (StringUtils.isEmpty(xmlhookUuid)) {
            return null;
        }
        XmlHookVmInstanceRefVO refVO = new XmlHookVmInstanceRefVO();
        refVO.setXmlHookUuid(xmlhookUuid);
        refVO.setVmInstanceUuid(vmUuid);
        refVO.setLastOpDate(new Timestamp(new Date().getTime()));
        refVO.setCreateDate(new Timestamp(new Date().getTime()));
        dbf.persist(refVO);
        return null;
    }
}
