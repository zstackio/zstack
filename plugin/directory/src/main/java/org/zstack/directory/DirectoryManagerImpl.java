package org.zstack.directory;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * @author shenjin
 * @date 2022/11/28 17:54
 */
public class DirectoryManagerImpl extends AbstractService implements DirectoryManager, Component {
    private static final CLogger logger = Utils.getLogger(DirectoryManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof DirectoryMessage) {
            passThrough((DirectoryMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateDirectoryMsg) {
            handle((APICreateDirectoryMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(DirectoryMessage msg) {
        DirectoryVO vo = dbf.findByUuid(msg.getDirectoryUuid(), DirectoryVO.class);
        if (vo == null) {
            bus.replyErrorByMessageType((Message) msg, err(SysErrors.RESOURCE_NOT_FOUND, "unable to find directory[uuid=%s]", msg.getDirectoryUuid()));
            return;
        }

        DirectoryBase base = new DirectoryBase(vo);
        base.handleMessage((Message) msg);
    }

    private void handle(APICreateDirectoryMsg msg) {
        APICreateDirectoryEvent event = new APICreateDirectoryEvent(msg.getId());

        DirectoryVO vo = new DirectoryVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        String name = msg.getName();
        vo.setName(name);
        List<DirectoryVO> directoryVOS;
        if(StringUtils.isEmpty(msg.getParentUuid())){
            //first-level directory
            vo.setGroupName(name);
            //judge whether the same level directory has the same name
            directoryVOS = Q.New(DirectoryVO.class).isNull(DirectoryVO_.parentUuid).list();
        } else {
            String groupName = String.format("/%s", msg.getName());
            DirectoryVO parentVO = dbf.findByUuid(msg.getParentUuid(), DirectoryVO.class);
            vo.setGroupName(parentVO.getGroupName() + groupName);
            vo.setParentUuid(msg.getParentUuid());
            //judge whether the same level directory has the same name
            directoryVOS = Q.New(DirectoryVO.class).eq(DirectoryVO_.parentUuid, parentVO.getUuid()).list();
        }
        List<DirectoryVO> list = directoryVOS.stream().filter(DirectoryVO -> DirectoryVO.getName().equals(name)).collect(Collectors.toList());
        if (!list.isEmpty()) {
            event.setError(operr("duplicate directory name, directory[uuid: %s] with name %s already exists", list.get(0).getUuid(), msg.getName()));
            bus.publish(event);
            return;
        }
        //judge whether the maximum level is exceeded
        String[] split = vo.getGroupName().split("/");
        // the directory cannot exceed 4 floors (contains the default directory)
        if(split.length > 3) {
            event.setError(operr("fail to create directory, directories are up to four levels"));
            bus.publish(event);
            return;
        }
        vo.setType(msg.getType());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo.setRootDirectoryUuid(Platform.getUuid());
        vo.setZoneUuid(msg.getZoneUuid());
        vo.setCreateDate(new Timestamp(new Date().getTime()));
        dbf.persist(vo);
        event.setInventory(DirectoryInventory.valueOf(vo));
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
}
