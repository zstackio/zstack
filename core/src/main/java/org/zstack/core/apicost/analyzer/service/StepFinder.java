package org.zstack.core.apicost.analyzer.service;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.apicost.analyzer.entity.StepVO;
import org.zstack.core.apicost.analyzer.entity.StepVO_;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by huaxin on 2021/7/9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StepFinder {

    @Autowired
    private DatabaseFacade dbf;

    @Transactional
    public List<StepVO> findAll() {
        return dbf.listAll(StepVO.class);
    }

    @Transactional
    public List<StepVO> listAllStepsByApiId(String apiId) {
        return Q.New(StepVO.class).eq(StepVO_.apiId, apiId).list();
    }

    @Transactional
    public void updateByStepId(String stepId, Integer logCount, BigDecimal meanWait) {
        StepVO stepVO = findOne(stepId);
        stepVO.setLogCount(logCount);
        stepVO.setMeanWait(meanWait);
        dbf.update(stepVO);
    }

    @Transactional
    public StepVO findOne(String stepId) {
        return Q.New(StepVO.class).eq(StepVO_.stepId, stepId).find();
    }

    @Transactional
    public StepVO save(StepVO step) {
        if (null == step.getUuid() || "".equals(step.getUuid()))
            step.setUuid(Platform.getUuid());
        return dbf.persist(step);
    }

}
