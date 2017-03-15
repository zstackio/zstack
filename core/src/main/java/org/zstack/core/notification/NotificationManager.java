package org.zstack.core.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.message.Message;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by xing5 on 2017/3/15.
 */
public class NotificationManager extends AbstractService {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    void send(NotificationBuilder builder) {
        NotificationVO vo = new NotificationVO();
        vo.setName(builder.notificationName);
        vo.setArguments(JSONObjectUtil.toJsonString(builder.arguments));
        vo.setContent(builder.content);
        vo.setResourceType(builder.resourceType);
        vo.setResourceUuid(builder.resourceUuid);
        vo.setSender(builder.sender);
        vo.setStatus(NotificationStatus.Unread);
        vo.setType(builder.type);
        vo.setUuid(Platform.getUuid());
        vo.setTime(System.currentTimeMillis());
        vo = dbf.persistAndRefresh(vo);

        //TODO: send to bus
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
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(NotificationConstant.SERVICE_ID);
    }

}
