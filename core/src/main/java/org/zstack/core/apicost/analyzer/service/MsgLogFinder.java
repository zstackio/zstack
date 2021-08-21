package org.zstack.core.apicost.analyzer.service;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.apicost.analyzer.entity.*;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Created by huaxin on 2021/7/15.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MsgLogFinder {

    public static int UPDATED = 1;
    public static int NOT_UPDATE = 0;

    @Autowired
    private DatabaseFacade dbf;

    @Transactional
    public List<MsgLogVO> listNotUpdateApiIds(int pageNum, int pageSize) {
        // TODO: 此处有分页逻辑
        return Q.New(MsgLogVO.class)
                .eq(MsgLogVO_.status, NOT_UPDATE)
                .orderBy(MsgLogVO_.startTime, SimpleQuery.Od.ASC)
                .groupBy(MsgLogVO_.apiId)
                .list();
    }

    @Transactional
    public List<MsgLogVO> listOrderedMsgLogsByApiId(String apiId) {
        return Q.New((MsgLogVO.class))
                .eq(MsgLogVO_.apiId, apiId)
                .orderBy(MsgLogVO_.startTime, SimpleQuery.Od.ASC)
                .list();
    }

    @Transactional
    public MsgLogVO save(String msgId, String msgName, String apiId, String taskName,
                         long startTime, long replyTime, BigDecimal wait,
                      int status) {
        MsgLogVO msgLog = new MsgLogVO();
        msgLog.setUuid(Platform.getUuid());
        msgLog.setMsgId(msgId);
        msgLog.setMsgName(msgName);
        msgLog.setApiId(apiId);
        msgLog.setTaskName(taskName);
        msgLog.setStartTime(startTime);
        msgLog.setReplyTime(replyTime);
        msgLog.setWait(wait);
        msgLog.setStatus(status);
        return dbf.persist(msgLog);
    }

    @Transactional
    public void update(MsgLogVO msgLog) {
        dbf.update(msgLog);
    }

    @Transactional
    public void updateStatus(Long id, int status) {
        MsgLogVO msgLog = dbf.findById(id, MsgLogVO.class);
        msgLog.setStatus(status);
        dbf.update(msgLog);
    }

}
