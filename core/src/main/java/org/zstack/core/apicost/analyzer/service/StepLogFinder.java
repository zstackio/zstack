package org.zstack.core.apicost.analyzer.service;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.apicost.analyzer.entity.StepLogVO;
import org.zstack.core.apicost.analyzer.entity.StepLogVO_;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Created by huaxin on 2021/7/9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StepLogFinder {

    @Autowired
    private DatabaseFacade dbf;

    @Transactional
    public List<StepLogVO> listApiLogs(int pageNum, int pageSize) {
        return dbf.listAll((pageNum - 1) * pageSize, pageSize, StepLogVO.class);
    }

    @Transactional
    public List<StepLogVO> listOrderedStepLogsByApiLogId(Long apiLogId) {
        return Q.New(StepLogVO.class)
                .eq(StepLogVO_.apiLogId, apiLogId)
                .orderBy(StepLogVO_.startTime, SimpleQuery.Od.ASC)
                .list();
    }

    @Transactional
    public StepLogVO save(String stepId, String name, Long apiLogId,
                          long startTime, long endTime, BigDecimal wait) {
        StepLogVO stepLog = new StepLogVO();
        stepLog.setStepId(stepId);
        stepLog.setName(name);
        stepLog.setApiLogId(apiLogId);
        stepLog.setStartTime(startTime);
        stepLog.setEndTime(endTime);
        stepLog.setWait(wait);
        stepLog.setUuid(Platform.getUuid());
        return dbf.persist(stepLog);
    }

}
